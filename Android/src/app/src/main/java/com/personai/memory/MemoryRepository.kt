package com.personai.memory

/**
 * Result model representing a memory item and its calculated cosine similarity score.
 */
data class ScoredMemoryItem(
    val item: MemoryItemEntity,
    val score: Float
)

/**
 * Data access abstraction and semantic search engine across stored memories.
 */
class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val vectorizer: MemoryVectorizer = MemoryVectorizer()
) {
    suspend fun saveMemory(
        content: String,
        linkedFileUri: String? = null,
        conceptTag: String? = null,
        stability: Double = 1.0,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        val embedding = vectorizer.vectorize(content).toList()
        val entity = MemoryItemEntity(
            content = content,
            embedding = embedding,
            linkedFileUri = linkedFileUri,
            conceptTag = conceptTag,
            createdAt = createdAt,
            lastReviewedAt = createdAt,
            stability = stability,
            repetitionCount = 0,
            retentionScore = 1.0,
            nextReviewAt = createdAt + (stability * 24 * 3600 * 1000L).toLong()
        )
        return memoryDao.insert(entity)
    }

    suspend fun searchSimilar(
        query: String,
        threshold: Float = 0.5f,
        limit: Int = 5
    ): List<ScoredMemoryItem> {
        val queryVector = vectorizer.vectorize(query)
        val allItems = memoryDao.getAll()

        return allItems.asSequence()
            .mapNotNull { item ->
                val itemVector = item.embedding.toFloatArray()
                if (itemVector.size != queryVector.size) {
                    null
                } else {
                    val similarity = vectorizer.cosineSimilarity(queryVector, itemVector)
                    ScoredMemoryItem(item = item, score = similarity)
                }
            }
            .filter { it.score >= threshold }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    suspend fun findMemoriesForFile(fileUri: String): List<MemoryItemEntity> {
        return memoryDao.getByFileUri(fileUri)
    }

    suspend fun getPendingRevisions(currentTime: Long): List<MemoryItemEntity> {
        return memoryDao.getDueForRevision(currentTime)
    }

    suspend fun updateMemory(item: MemoryItemEntity) {
        memoryDao.update(item)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteById(id)
    }
}
