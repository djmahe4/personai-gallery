package com.personai.persona

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonaInferenceEngineTest {

  @Test
  fun givenWorkNotificationsDuringNineToFiveWhenAnalyzedThenPersonaStateSetToFocusedWork() {
    val engine = PersonaInferenceEngine()
    val events =
        listOf(
            NotificationEventEntity(
                packageName = "com.slack",
                message = "Sprint planning and release blocker",
                postedAt = 1_700_000_000_000,
                senderHash = "h1",
                hourOfDay = 10,
            ),
            NotificationEventEntity(
                packageName = "com.google.android.gm",
                message = "Code review requested",
                postedAt = 1_700_000_000_100,
                senderHash = "h2",
                hourOfDay = 11,
            ),
        )

    assertEquals(PersonaState.FOCUSED_WORK, engine.inferState(events))
  }

  @Test
  fun nonWorkNotificationsOutsideOfficeHoursRemainGeneral() {
    val engine = PersonaInferenceEngine()
    val events =
        listOf(
            NotificationEventEntity(
                packageName = "com.instagram.android",
                message = "new reel posted",
                postedAt = 1L,
                senderHash = "h",
                hourOfDay = 22,
            )
        )

    assertEquals(PersonaState.GENERAL, engine.inferState(events))
  }
}
