package com.personai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Memory stress, throughput, and heap allocation benchmark suite.
 * Evaluates behavior under high vector volume, sliding window tokenization, and rapid querying.
 */
class MemoryStressBenchmarkTest {

    private class BenchMemoryDao : MemoryDao {
        val items = mutableListOf<MemoryItemEntity>()
        private var nextId = 1L

        override suspend fun insert(item: MemoryItemEntity): Long {
            val assigned = item.copy(id = nextId++)
            items.add(assigned)
            return assigned.id
        }

        override suspend fun insertAll(items: List<MemoryItemEntity>): List<Long> {
            return items.map { insert(it) }
        }

        override suspend fun update(item: MemoryItemEntity) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx >= 0) items[idx] = item
        }

        override suspend fun updateAll(items: List<MemoryItemEntity>) {
            items.forEach { update(it) }
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

    private lateinit var dao: BenchMemoryDao
    private lateinit var repository: MemoryRepository
    private val vectorizer = MemoryVectorizer(dimension = 128)

    @Before
    fun setUp() {
        dao = BenchMemoryDao()
        repository = MemoryRepository(dao, vectorizer)
    }

    @Test
    fun benchmarkBatchIngestionHeapAndThroughput() = runBlocking {
        val count = 1000
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val beforeHeap = runtime.totalMemory() - runtime.freeMemory()

        val elapsed = measureTimeMillis {
            for (i in 1..count) {
                repository.saveMemory(
                    content = "Benchmark test memory item #$i with topic features and domain context",
                    conceptTag = "tag_${i % 10}"
                )
            }
        }

        runtime.gc()
        val afterHeap = runtime.totalMemory() - runtime.freeMemory()
        val heapGrowthBytes = (afterHeap - beforeHeap).coerceAtLeast(0)

        // Throughput assertion: 1000 items vectorized and saved under 5000ms on JVM
        assertTrue("Ingestion elapsed time should be < 5000ms, was ${elapsed}ms", elapsed < 5000)

        // Per-vector heap footprint should be < 25KB per item (including strings & collections)
        val bytesPerItem = if (count > 0) heapGrowthBytes / count else 0
        assertTrue("Heap footprint per vector item should remain modest (< 25KB), was ~$bytesPerItem bytes", bytesPerItem < 25000)
    }

    @Test
    fun benchmarkSlidingWindowSearchUnderLoad() = runBlocking {
        // Pre-populate 200 documents with variable sizes
        for (i in 1..200) {
            val content = (1..30).joinToString(" ") { "doc_$i feature_token_$it vocabulary_term_$it" }
            repository.saveMemory(content, conceptTag = "bench")
        }

        // Run 50 sliding window queries
        val elapsed = measureTimeMillis {
            for (q in 1..50) {
                val query = "feature_token_${q % 20} vocabulary_term_${q % 15}"
                val results = repository.searchSimilarSlidingWindow(
                    query = query,
                    threshold = 0.2f,
                    limit = 5,
                    maxCandidates = 200,
                    windowSize = 16,
                    stepSize = 8
                )
                assertTrue("Search should return candidates", results.isNotEmpty())
            }
        }

        // 50 sliding window scans across 200 docs with windowing under 3500ms
        val avgLatencyMs = elapsed / 50.0
        assertTrue("Average sliding window query latency should be < 100ms, was ${avgLatencyMs}ms", avgLatencyMs < 100.0)
    }

    @Test
    fun benchmarkMemoryTypeConvertersOverhead() {
        val converters = MemoryTypeConverters()
        val sampleFloats = (1..128).map { it * 0.0078125f }

        val elapsed = measureTimeMillis {
            for (i in 1..1000) {
                val serialized = converters.fromFloatList(sampleFloats)
                val deserialized = converters.toFloatList(serialized)
                org.junit.Assert.assertEquals(128, deserialized.size)
            }
        }

        assertTrue("1000 serialization/deserialization cycles should take < 500ms, was ${elapsed}ms", elapsed < 500)
    }
}
