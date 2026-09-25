package com.personai.notification

import android.app.Notification
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.personai.agent.PersonAILogger
import com.personai.match.PrivacyHasher
import com.personai.persona.NotificationEventEntity
import com.personai.persona.PersonaDatabase
import com.personai.persona.PersonaRepository
import com.personai.persona.PersonaTrackingPreferencesFragment
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PersonaNotificationService : NotificationListenerService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val repository: PersonaRepository by lazy {
    val db = PersonaDatabase.getInstance(applicationContext)
    PersonaRepository(db.notificationEventDao(), db.personaStateDao())
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
    if (!shouldProcess(readBatteryPercent(batteryManager), powerManager?.isDeviceIdleMode == true)) {
      PersonAILogger.i("PersonaNotificationService", "Notification skipped due to battery/doze guard")
      return
    }

    val selection = PersonaTrackingPreferencesFragment.getSelection(this)
    if (selection.trackedPackages.isNotEmpty() && sbn.packageName !in selection.trackedPackages) {
      PersonAILogger.d("PersonaNotificationService", "Notification skipped: package not in tracked list")
      return
    }

    val extras = sbn.notification.extras
    val payload =
        extractPayload(
            packageName = sbn.packageName,
            title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            postedAt = sbn.postTime,
            sender = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            hasher = PrivacyHasher { perInstallSalt(this) },
        ) ?: return

    PersonAILogger.d(
        "PersonaNotificationService",
        "Extracted notification event package=${payload.packageName} time=${payload.postedAt}")

    serviceScope.launch {
      runCatching {
        repository.saveNotification(payload)
      }.onFailure { e ->
        PersonAILogger.e("PersonaNotificationService", "Failed to save notification event", e)
      }
    }
  }

  fun shouldProcess(batteryPercent: Int, isDozeMode: Boolean): Boolean {
    return batteryPercent >= MIN_BATTERY_PERCENT && !isDozeMode
  }

  fun extractPayload(
      packageName: String,
      title: String?,
      text: String?,
      postedAt: Long,
      sender: String?,
      hasher: PrivacyHasher,
  ): NotificationEventEntity? {
    val message = listOfNotNull(title?.trim(), text?.trim()).filter { it.isNotBlank() }.joinToString(" ")
    if (message.isBlank()) {
      return null
    }

    val senderHash = sender?.takeIf { it.isNotBlank() }?.let { hasher.hash(it) }
    val hour = Calendar.getInstance().apply { timeInMillis = postedAt }.get(Calendar.HOUR_OF_DAY)

    return NotificationEventEntity(
        packageName = packageName,
        message = message,
        postedAt = postedAt,
        senderHash = senderHash,
        hourOfDay = hour,
    )
  }

  private fun readBatteryPercent(batteryManager: BatteryManager?): Int {
    if (batteryManager == null) return 100
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
  }

  companion object {
    private const val MIN_BATTERY_PERCENT = 15
    private const val PREFS_SECURITY = "personai_security"
    private const val KEY_SALT = "hasher_salt"

    fun perInstallSalt(context: Context): ByteArray {
      val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
      var salt = prefs.getString(KEY_SALT, null)
      if (salt == null) {
        salt = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_SALT, salt).apply()
      }
      return salt.toByteArray()
    }
  }
}
