package com.personai.memory

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update

/**
 * Memory item entity for vector representation, file links, and spaced repetition tracking.
 */
@Entity(
    tableName = "memory_items",
    indices = [
        androidx.room.Index(value = ["linkedFileUri"]),
        androidx.room.Index(value = ["conceptTag"]),
        androidx.room.Index(value = ["nextReviewAt"])
    ]
)
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val content: String,
    val embedding: List<Float>,
    val linkedFileUri: String? = null,
    val conceptTag: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val stability: Double = 1.0,
    val repetitionCount: Int = 0,
    val retentionScore: Double = 1.0,
    val nextReviewAt: Long = System.currentTimeMillis()
)

class MemoryTypeConverters {
    @TypeConverter
    fun fromFloatList(list: List<Float>?): String {
        if (list == null || list.isEmpty()) return ""
        return list.joinToString(separator = ",") { it.toString() }
    }

    @TypeConverter
    fun toFloatList(data: String?): List<Float> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",").mapNotNull {
            it.trim().toFloatOrNull()
        }
    }
}

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MemoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MemoryItemEntity>): List<Long>

    @Update
    suspend fun update(item: MemoryItemEntity)

    @Update
    suspend fun updateAll(items: List<MemoryItemEntity>)

    @Query("SELECT * FROM memory_items ORDER BY id DESC")
    suspend fun getAll(): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE linkedFileUri = :uri ORDER BY id DESC")
    suspend fun getByFileUri(uri: String): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items WHERE conceptTag = :tag ORDER BY id DESC")
    suspend fun getByConceptTag(tag: String): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items WHERE nextReviewAt <= :cutoffTimestamp ORDER BY nextReviewAt ASC")
    suspend fun getDueForRevision(cutoffTimestamp: Long): List<MemoryItemEntity>

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory_items")
    suspend fun clearAll()
}

@Database(entities = [MemoryItemEntity::class], version = 1, exportSchema = false)
@TypeConverters(MemoryTypeConverters::class)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "personai_memory.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
