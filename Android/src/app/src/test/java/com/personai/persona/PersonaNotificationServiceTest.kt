package com.personai.persona

import com.personai.match.PrivacyHasher
import com.personai.notification.PersonaNotificationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaNotificationServiceTest {

  @Test
  fun givenValidNotificationBundleWhenPostedThenExtractedDataSavedCorrectly() {
    val service = PersonaNotificationService()
    val payload =
        service.extractPayload(
            packageName = "com.google.android.gm",
            title = "Daily standup",
            text = "Project updates at 10:00",
            postedAt = 1_700_000_000_000,
            sender = "alice@example.com",
            hasher = PrivacyHasher { "salt".toByteArray() },
        )

    assertNotNull(payload)
    assertEquals("com.google.android.gm", payload?.packageName)
    assertEquals("Daily standup Project updates at 10:00", payload?.message)
    assertTrue(payload?.senderHash?.isNotBlank() == true)
    assertFalse(payload?.senderHash?.contains("alice@example.com") == true)
  }

  @Test
  fun batteryAndDozeGuardsBlockProcessing() {
    val service = PersonaNotificationService()
    assertFalse(service.shouldProcess(batteryPercent = 10, isDozeMode = false))
    assertFalse(service.shouldProcess(batteryPercent = 95, isDozeMode = true))
    assertTrue(service.shouldProcess(batteryPercent = 80, isDozeMode = false))
  }

  @Test
  fun emptyNotificationProducesNoPayload() {
    val service = PersonaNotificationService()
    val payload =
        service.extractPayload(
            packageName = "com.test",
            title = null,
            text = null,
            postedAt = 1L,
            sender = null,
            hasher = PrivacyHasher { "salt".toByteArray() },
        )

    assertNull(payload)
  }
}
