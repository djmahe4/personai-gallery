package com.personai.persona

class PersonaRepository(
    private val notificationEventDao: NotificationEventDao,
    private val personaStateDao: PersonaStateDao,
) {
  suspend fun saveNotification(event: NotificationEventEntity) {
    notificationEventDao.insert(event)
  }

  suspend fun getNotifications(startInclusive: Long, endInclusive: Long): List<NotificationEventEntity> {
    return notificationEventDao.getBetween(startInclusive, endInclusive)
  }

  suspend fun savePersonaState(state: PersonaState, updatedAt: Long) {
    personaStateDao.upsert(PersonaStateEntity(state = state, updatedAt = updatedAt))
  }

  suspend fun getCurrentPersonaState(): PersonaState? = personaStateDao.getByKey()?.state
}
