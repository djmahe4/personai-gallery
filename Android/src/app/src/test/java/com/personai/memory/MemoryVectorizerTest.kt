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

    @Test
    fun slidingWindowGeneratesOverlappingWindows() {
        val words = (1..60).joinToString(" ") { "word$it" }
        val windows = vectorizer.vectorizeSlidingWindow(words, windowSize = 20, stepSize = 10)
        // 60 tokens with window 20 and step 10 -> window starts at 0, 10, 20, 30, 40, 50 -> 6 windows
        assertEquals(6, windows.size)
        assertEquals(0, windows[0].index)
        assertEquals(128, windows[0].vector.size)
        assertTrue(windows[0].text.startsWith("word1"))
    }

    @Test
    fun slidingWindowFindsLocalizedNeedleInLongHaystack() {
        val preamble = (1..50).joinToString(" ") { "irrelevant intro context data point $it" }
        val needle = "quantum entanglement cryptographic key distribution"
        val postamble = (1..50).joinToString(" ") { "unrelated ending details section $it" }
        val longDoc = "$preamble $needle $postamble"

        val queryVec = vectorizer.vectorize("quantum key distribution")
        val wholeDocVec = vectorizer.vectorize(longDoc)
        val wholeDocScore = vectorizer.cosineSimilarity(queryVec, wholeDocVec)

        val windows = vectorizer.vectorizeSlidingWindow(longDoc, windowSize = 20, stepSize = 10)
        val best = vectorizer.bestMatchingWindow(queryVec, windows)

        org.junit.Assert.assertNotNull(best)
        // Sliding window local match must significantly outperform diluted whole-doc score
        assertTrue("Sliding window score (${best!!.second}) should exceed diluted whole doc score ($wholeDocScore)", best.second > wholeDocScore)
        assertTrue("Best match score should be > 0.2, was ${best.second}", best.second > 0.2f)
        assertTrue("Matched snippet should contain needle words", best.first.text.contains("entanglement") || best.first.text.contains("cryptographic"))
    }

    @Test
    fun multiScenarioUnicodeAndHighNoiseInputs() {
        val unicodeDoc = "Kotlin 语言 协程 and 機械学習 models on デバイス edge"
        val vec1 = vectorizer.vectorize(unicodeDoc)
        assertEquals(128, vec1.size)
        val vec2 = vectorizer.vectorize(unicodeDoc)
        val sim = vectorizer.cosineSimilarity(vec1, vec2)
        assertEquals(1.0f, sim, 0.001f)

        // Noise and special characters
        val noisyText = "  !!!###  Machine-Learning...   +++ AI ???  "
        val cleanText = "Machine Learning AI"
        val vNoise = vectorizer.vectorize(noisyText)
        val vClean = vectorizer.vectorize(cleanText)
        assertEquals(1.0f, vectorizer.cosineSimilarity(vNoise, vClean), 0.001f)
    }
}
