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
        return TOKEN_SPLIT_REGEX.split(PUNCTUATION_REGEX.replace(text.lowercase(), " "))
            .filter { it.isNotBlank() }
    }

    companion object {
        private val PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{Nd}\\s]")
        private val TOKEN_SPLIT_REGEX = Regex("\\s+")
    }
}
