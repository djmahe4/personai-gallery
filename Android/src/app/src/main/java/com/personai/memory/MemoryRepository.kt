package com.personai.memory

/**
 * Result model representing a memory item and its calculated cosine similarity score.
 */
data class ScoredMemoryItem(
    val item: MemoryItemEntity,
    val score: Float,
    val matchedSnippet: String? = null,
    val matchedWindowIndex: Int? = null
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
        limit: Int = 5,
        maxCandidates: Int = 500
    ): List<ScoredMemoryItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val queryVector = vectorizer.vectorize(query)
        val scoredList = mutableListOf<ScoredMemoryItem>()
        var offset = 0
        val batchSize = 100

        while (offset < maxCandidates) {
            val currentLimit = minOf(batchSize, maxCandidates - offset)
            val batch = memoryDao.getBatch(limit = currentLimit, offset = offset)
            if (batch.isEmpty()) break

            for (item in batch) {
                val itemVector = item.embedding.toFloatArray()
                if (itemVector.size == queryVector.size) {
                    val similarity = vectorizer.cosineSimilarity(queryVector, itemVector)
                    if (similarity >= threshold) {
                        scoredList.add(ScoredMemoryItem(item = item, score = similarity))
                    }
                }
            }
            if (batch.size < currentLimit) break
            offset += currentLimit
        }

        scoredList
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * Executes localized semantic search using sliding windows over long memory content.
     * Prevents diluted retrieval scores where localized needle keywords are obscured in long documents.
     */
    suspend fun searchSimilarSlidingWindow(
        query: String,
        threshold: Float = 0.25f,
        limit: Int = 5,
        maxCandidates: Int = 500,
        windowSize: Int = 32,
        stepSize: Int = 16
    ): List<ScoredMemoryItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val queryVector = vectorizer.vectorize(query)
        val scoredList = mutableListOf<ScoredMemoryItem>()
        var offset = 0
        val batchSize = 100

        while (offset < maxCandidates) {
            val currentLimit = minOf(batchSize, maxCandidates - offset)
            val batch = memoryDao.getBatch(limit = currentLimit, offset = offset)
            if (batch.isEmpty()) break

            for (item in batch) {
                val itemVector = item.embedding.toFloatArray()
                val wholeDocScore = if (itemVector.size == queryVector.size) {
                    vectorizer.cosineSimilarity(queryVector, itemVector)
                } else 0.0f

                // Token sliding window over content
                val windows = vectorizer.vectorizeSlidingWindow(
                    text = item.content,
                    windowSize = windowSize,
                    stepSize = stepSize
                )
                val bestMatch = vectorizer.bestMatchingWindow(queryVector, windows)

                val bestWindowScore = bestMatch?.second ?: -1.0f
                val finalScore = maxOf(wholeDocScore, bestWindowScore)

                if (finalScore >= threshold) {
                    val matchedSnippet = if (bestMatch != null && bestWindowScore >= wholeDocScore) {
                        bestMatch.first.text
                    } else null

                    val matchedIndex = if (bestMatch != null && bestWindowScore >= wholeDocScore) {
                        bestMatch.first.index
                    } else null

                    scoredList.add(
                        ScoredMemoryItem(
                            item = item,
                            score = finalScore,
                            matchedSnippet = matchedSnippet,
                            matchedWindowIndex = matchedIndex
                        )
                    )
                }
            }
            if (batch.size < currentLimit) break
            offset += currentLimit
        }

        scoredList
            .sortedByDescending { it.score }
            .take(limit)
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
