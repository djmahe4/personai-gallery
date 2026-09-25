package com.personai.persona

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personai.agent.PersonAILogger

class PersonaInferenceEngine {
  private val workKeywords = setOf("review", "sprint", "release", "incident", "meeting", "blocker", "project")
  private val workPackages = setOf("com.slack", "com.google.android.gm", "com.microsoft.teams")

  fun inferState(events: List<NotificationEventEntity>): PersonaState {
    if (events.isEmpty()) {
      return PersonaState.GENERAL
    }

    val workSignals =
        events.count { event ->
          event.hourOfDay in 9..17 &&
              (event.packageName in workPackages ||
                  workKeywords.any { keyword -> event.message.contains(keyword, ignoreCase = true) })
        }

    return if (workSignals * 2 >= events.size) PersonaState.FOCUSED_WORK else PersonaState.GENERAL
  }
}

class PersonaInferenceWorker(
    appContext: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  private val repository: PersonaRepository by lazy {
    val db = PersonaDatabase.getInstance(applicationContext)
    PersonaRepository(db.notificationEventDao(), db.personaStateDao())
  }
  private val engine = PersonaInferenceEngine()

  override suspend fun doWork(): Result {
    val now = System.currentTimeMillis()
    val events = repository.getNotifications(now - WINDOW_MILLIS, now)
    val inferred = engine.inferState(events)
    repository.savePersonaState(inferred, now)
    PersonAILogger.i("PersonaInferenceWorker", "Persona inferred as $inferred")
    return Result.success()
  }

  companion object {
    private const val WINDOW_MILLIS = 60 * 60 * 1000L
  }
}
