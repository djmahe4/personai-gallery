package com.personai.memory

/**
 * Links local document files and snippets with prompts and conversation context,
 * persisting reciprocal associations in the memory repository.
 */
class FilePromptLinker(
    private val memoryRepository: MemoryRepository
) {
    /**
     * Stores a bidirectional or contextual link between prompt content and a referenced file URI.
     */
    suspend fun linkPromptToFile(
        prompt: String,
        fileUri: String,
        conceptTag: String? = null,
        fileSnippet: String? = null
    ): Long {
        require(prompt.isNotBlank()) { "Prompt cannot be blank" }
        require(fileUri.isNotBlank()) { "fileUri cannot be blank" }
        val aggregatedContent = buildString {
            append(prompt.trim())
            if (!fileSnippet.isNullOrBlank()) {
                append("\n[Context File Snippet]: ")
                append(fileSnippet.trim())
            }
        }

        return memoryRepository.saveMemory(
            content = aggregatedContent,
            linkedFileUri = fileUri,
            conceptTag = conceptTag
        )
    }

    /**
     * Resolves all prior prompt contexts associated with the given file URI.
     */
    suspend fun resolveContextForFile(fileUri: String): List<String> {
        val items = memoryRepository.findMemoriesForFile(fileUri)
        return items.map { it.content }
    }
}
