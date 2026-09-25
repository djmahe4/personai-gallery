package com.personai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FilePromptLinkerTest {

    private class FakeMemoryDao : MemoryDao {
        val items = mutableListOf<MemoryItemEntity>()
        private var nextId = 1L

        override suspend fun insert(item: MemoryItemEntity): Long {
            val assigned = item.copy(id = nextId++)
            items.add(assigned)
            return assigned.id
        }

        override suspend fun insertAll(itemsToInsert: List<MemoryItemEntity>): List<Long> =
            itemsToInsert.map { insert(it) }

        override suspend fun update(item: MemoryItemEntity) {
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) items[index] = item
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
        override suspend fun getBatch(limit: Int, offset: Int): List<MemoryItemEntity> =
            items.sortedByDescending { it.id }.drop(offset).take(limit)
        override suspend fun deleteById(id: Long) { items.removeAll { it.id == id } }
        override suspend fun clearAll() { items.clear() }
    }

    private lateinit var repository: MemoryRepository
    private lateinit var linker: FilePromptLinker

    @Before
    fun setUp() {
        val dao = FakeMemoryDao()
        repository = MemoryRepository(dao, MemoryVectorizer(dimension = 32))
        linker = FilePromptLinker(repository)
    }

    @Test
    fun linkPromptToFileCreatesReciprocalMemoryWithContext() = runBlocking {
        val prompt = "Summarize the architectural guidelines in this document"
        val fileUri = "content://media/external/docs/architecture.pdf"
        val snippet = "Chapter 1: Clean architecture principles and layer separation"

        val id = linker.linkPromptToFile(
            prompt = prompt,
            fileUri = fileUri,
            conceptTag = "architecture",
            fileSnippet = snippet
        )

        assertTrue(id > 0)
        val memories = repository.findMemoriesForFile(fileUri)
        assertEquals(1, memories.size)
        assertEquals(fileUri, memories[0].linkedFileUri)
        assertEquals("architecture", memories[0].conceptTag)
        assertTrue(memories[0].content.contains("Summarize the architectural guidelines"))
        assertTrue(memories[0].content.contains("Chapter 1: Clean architecture"))
    }

    @Test
    fun resolveContextForFileReturnsAllAssociatedPrompts() = runBlocking {
        val fileUri = "content://media/external/docs/notes.md"
        linker.linkPromptToFile("What are the key action items?", fileUri)
        linker.linkPromptToFile("Explain section 2", fileUri)

        val contexts = linker.resolveContextForFile(fileUri)
        assertEquals(2, contexts.size)
        assertTrue(contexts[0].contains("What are the key action items?"))
        assertTrue(contexts[1].contains("Explain section 2"))
    }
}
