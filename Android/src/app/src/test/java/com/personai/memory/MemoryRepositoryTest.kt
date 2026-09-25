package com.personai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MemoryRepositoryTest {

    private class FakeMemoryDao : MemoryDao {
        private val items = mutableListOf<MemoryItemEntity>()
        private var nextId = 1L

        override suspend fun insert(item: MemoryItemEntity): Long {
            val assigned = item.copy(id = if (item.id == 0L) nextId++ else item.id)
            items.removeAll { it.id == assigned.id }
            items.add(assigned)
            return assigned.id
        }

        override suspend fun insertAll(itemsToInsert: List<MemoryItemEntity>): List<Long> {
            return itemsToInsert.map { insert(it) }
        }

        override suspend fun update(item: MemoryItemEntity) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx >= 0) {
                items[idx] = item
            }
        }

        override suspend fun updateAll(itemsToUpdate: List<MemoryItemEntity>) {
            itemsToUpdate.forEach { update(it) }
        }

        override suspend fun getAll(): List<MemoryItemEntity> = items.toList()

        override suspend fun getById(id: Long): MemoryItemEntity? = items.find { it.id == id }

        override suspend fun getByFileUri(uri: String): List<MemoryItemEntity> =
            items.filter { it.linkedFileUri == uri }

        override suspend fun getByConceptTag(tag: String): List<MemoryItemEntity> =
            items.filter { it.conceptTag == tag }

        override suspend fun getDueForRevision(cutoffTimestamp: Long): List<MemoryItemEntity> =
            items.filter { it.nextReviewAt <= cutoffTimestamp }

        override suspend fun deleteById(id: Long) {
            items.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            items.clear()
        }
    }

    private lateinit var fakeDao: FakeMemoryDao
    private lateinit var repository: MemoryRepository
    private val vectorizer = MemoryVectorizer(dimension = 64)

    @Before
    fun setUp() {
        fakeDao = FakeMemoryDao()
        repository = MemoryRepository(fakeDao, vectorizer)
    }

    @Test
    fun saveMemoryPersistsAndAssignsEmbedding() = runBlocking {
        val id = repository.saveMemory(
            content = "Kotlin coroutines flow and state management",
            conceptTag = "kotlin"
        )
        val all = fakeDao.getAll()
        assertEquals(1, all.size)
        assertEquals(id, all[0].id)
        assertEquals(64, all[0].embedding.size)
        assertEquals("kotlin", all[0].conceptTag)
    }

    @Test
    fun searchSimilarRanksByCosineSimilarity() = runBlocking {
        repository.saveMemory("Deep learning neural networks and backpropagation")
        repository.saveMemory("Chocolate chip cookies butter sugar recipe")
        repository.saveMemory("Machine learning deep neural network architecture")

        val results = repository.searchSimilar("deep neural networks", threshold = 0.3f, limit = 2)

        assertEquals(2, results.size)
        assertTrue(results[0].score >= results[1].score)
        assertTrue(results[0].item.content.contains("neural", ignoreCase = true))
    }

    @Test
    fun findMemoriesForFileFiltersByUri() = runBlocking {
        repository.saveMemory("Document about design patterns", linkedFileUri = "file:///docs/patterns.pdf")
        repository.saveMemory("Unrelated note", linkedFileUri = "file:///notes/todo.txt")

        val found = repository.findMemoriesForFile("file:///docs/patterns.pdf")
        assertEquals(1, found.size)
        assertEquals("file:///docs/patterns.pdf", found[0].linkedFileUri)
    }

    @Test
    fun getPendingRevisionsFiltersByNextReviewAt() = runBlocking {
        val now = 1_000_000L
        repository.saveMemory("Old memory", createdAt = now - 100_000)
        // Set nextReviewAt explicitly in fakeDao
        val item = fakeDao.getAll()[0].copy(nextReviewAt = now - 10)
        fakeDao.update(item)

        val due = repository.getPendingRevisions(now)
        assertEquals(1, due.size)
    }
}
