package com.personai.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MemoryVectorizerTest {

    private val vectorizer = MemoryVectorizer(dimension = 128)

    @Test
    fun vectorHasConfiguredDimension() {
        val vector = vectorizer.vectorize("Machine learning on Android edge devices")
        assertEquals(128, vector.size)
    }

    @Test
    fun emptyOrWhitespaceProducesZeroVector() {
        val vector = vectorizer.vectorize("   ")
        assertEquals(128, vector.size)
        assertTrue(vector.all { it == 0.0f })
    }

    @Test
    fun normalizedVectorL2NormIsApproximatelyOne() {
        val vector = vectorizer.vectorize("Local LLM model inference pipeline")
        val sumSquares = vector.map { it * it }.sum()
        assertTrue("Sum of squares should be near 1.0, was $sumSquares", abs(sumSquares - 1.0f) < 0.001f)
    }

    @Test
    fun identicalStringsHaveCosineSimilarityOne() {
        val text = "Edge AI inference on mobile CPU and GPU"
        val v1 = vectorizer.vectorize(text)
        val v2 = vectorizer.vectorize(text)
        val similarity = vectorizer.cosineSimilarity(v1, v2)
        assertTrue("Identity similarity should be 1.0f, was $similarity", abs(similarity - 1.0f) < 0.001f)
    }

    @Test
    fun caseAndPunctuationNormalizedProperly() {
        val text1 = "Hello, World! Welcome to PersonAI."
        val text2 = "hello world welcome to personai"
        val v1 = vectorizer.vectorize(text1)
        val v2 = vectorizer.vectorize(text2)
        val similarity = vectorizer.cosineSimilarity(v1, v2)
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun semanticallyDisjointStringsHaveLowOrZeroSimilarity() {
        val v1 = vectorizer.vectorize("baking sourdough bread oven temperature yeast recipe")
        val v2 = vectorizer.vectorize("quantum physics entanglement superposition schrodinger equation")
        val similarity = vectorizer.cosineSimilarity(v1, v2)
        assertTrue("Disjoint text similarity should be low (< 0.25f), was $similarity", similarity < 0.25f)
    }
}
