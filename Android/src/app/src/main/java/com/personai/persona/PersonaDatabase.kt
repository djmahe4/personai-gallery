package com.personai.persona

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "notification_events")
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val message: String,
    val postedAt: Long,
    val senderHash: String?,
    val hourOfDay: Int,
)

@Entity(tableName = "persona_state")
data class PersonaStateEntity(
    @PrimaryKey val key: String = CURRENT_STATE_KEY,
    val state: PersonaState,
    val updatedAt: Long,
)

enum class PersonaState {
  FOCUSED_WORK,
  GENERAL,
}

const val CURRENT_STATE_KEY = "current"

@Dao
interface NotificationEventDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(event: NotificationEventEntity)

  @Query("SELECT * FROM notification_events WHERE postedAt BETWEEN :startInclusive AND :endInclusive")
  suspend fun getBetween(startInclusive: Long, endInclusive: Long): List<NotificationEventEntity>
}

@Dao
interface PersonaStateDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(state: PersonaStateEntity)

  @Query("SELECT * FROM persona_state WHERE `key` = :key LIMIT 1")
  suspend fun getByKey(key: String = CURRENT_STATE_KEY): PersonaStateEntity?
}

@Database(entities = [NotificationEventEntity::class, PersonaStateEntity::class], version = 1)
abstract class PersonaDatabase : RoomDatabase() {
  abstract fun notificationEventDao(): NotificationEventDao

  abstract fun personaStateDao(): PersonaStateDao
}
