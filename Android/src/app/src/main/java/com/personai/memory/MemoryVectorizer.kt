package com.personai.memory

import kotlin.math.sqrt

/**
 * Generates deterministic dense feature vectors from text using word n-gram feature hashing
 * and computes vector cosine similarities.
 */
class MemoryVectorizer(
    private val dimension: Int = 128
) {
    init {
        require(dimension > 0) { "Dimension must be positive: $dimension" }
    }

    /**
     * Converts raw text into an L2-normalized float embedding vector of size [dimension].
     */
    fun vectorize(text: String): FloatArray {
        val vector = FloatArray(dimension)
        val normalizedTokens = tokenize(text)
        if (normalizedTokens.isEmpty()) {
            return vector
        }

        // Unigrams & Bigrams
        for (i in normalizedTokens.indices) {
            val unigram = normalizedTokens[i]
            val h1 = (unigram.hashCode() and 0x7FFFFFFF) % dimension
            vector[h1] += 1.0f

            if (i + 1 < normalizedTokens.size) {
                val bigram = "$unigram ${normalizedTokens[i + 1]}"
                val h2 = (bigram.hashCode() and 0x7FFFFFFF) % dimension
                vector[h2] += 1.5f
            }
        }

        normalizeL2(vector)
        return vector
    }

    /**
     * Computes cosine similarity between two float vectors.
     * Returns 0.0 if either vector has 0 magnitude.
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        require(v1.size == v2.size) {
            "Vector dimensions must match: ${v1.size} vs ${v2.size}"
        }

        var dot = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in v1.indices) {
            val a = v1[i]
            val b = v2[i]
            dot += a * b
            normA += a * a
            normB += b * b
        }

        val denom = sqrt(normA) * sqrt(normB)
        if (denom == 0.0f) return 0.0f
        return (dot / denom).coerceIn(-1.0f, 1.0f)
    }

    private fun normalizeL2(vector: FloatArray) {
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        if (norm > 0.0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }

    private fun tokenize(text: String): List<String> {
        return TOKEN_SPLIT_REGEX.split(PUNCTUATION_REGEX.replace(text.lowercase(java.util.Locale.ROOT), " "))
            .filter { it.isNotBlank() }
    }

    /**
     * Splits [text] into overlapping token windows and produces normalized vector embeddings for each.
     * Guarantees localized context is preserved across long documents without missing sub-phrases.
     */
    fun vectorizeSlidingWindow(
        text: String,
        windowSize: Int = 32,
        stepSize: Int = 16
    ): List<TextWindow> {
        require(windowSize > 0) { "windowSize must be positive: $windowSize" }
        require(stepSize > 0) { "stepSize must be positive: $stepSize" }

        val tokens = tokenize(text)
        if (tokens.isEmpty()) return emptyList()

        if (tokens.size <= windowSize) {
            val windowText = tokens.joinToString(" ")
            return listOf(
                TextWindow(
                    index = 0,
                    text = windowText,
                    vector = vectorize(windowText)
                )
            )
        }

        val windows = mutableListOf<TextWindow>()
        var startIdx = 0
        var windowIndex = 0

        while (startIdx < tokens.size) {
            val endIdx = (startIdx + windowSize).coerceAtMost(tokens.size)
            val windowTokens = tokens.subList(startIdx, endIdx)
            val windowText = windowTokens.joinToString(" ")
            windows.add(
                TextWindow(
                    index = windowIndex++,
                    text = windowText,
                    vector = vectorize(windowText)
                )
            )
            startIdx += stepSize
        }

        return windows
    }

    /**
     * Identifies the window with the highest cosine similarity against [queryVector].
     * Returns the matching [TextWindow] along with its calculated score, or null if no windows exist.
     */
    fun bestMatchingWindow(
        queryVector: FloatArray,
        windows: List<TextWindow>
    ): Pair<TextWindow, Float>? {
        if (windows.isEmpty()) return null
        var bestWindow: TextWindow? = null
        var maxScore = -1.0f

        for (window in windows) {
            val score = cosineSimilarity(queryVector, window.vector)
            if (score > maxScore) {
                maxScore = score
                bestWindow = window
            }
        }

        return bestWindow?.let { Pair(it, maxScore) }
    }

    companion object {
        private val PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{Nd}\\s]")
        private val TOKEN_SPLIT_REGEX = Regex("\\s+")
    }
}

/**
 * Encapsulates an individual text chunk extracted via sliding window with its positional index and vector.
 */
data class TextWindow(
    val index: Int,
    val text: String,
    val vector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TextWindow
        if (index != other.index) return false
        if (text != other.text) return false
        return vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + text.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}
