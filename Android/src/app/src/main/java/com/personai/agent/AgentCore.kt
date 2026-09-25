package com.personai.agent

import java.util.regex.Pattern

class AgentCore {

    private val tools = mutableMapOf<String, Pair<ToolSchema, (Map<String, String>) -> ToolExecutionResult>>()

    init {
        registerBuiltInSchemas()
    }

    private fun registerBuiltInSchemas() {
        // Built-in placeholder registrations
        registerTool(
            ToolSchema(
                name = "searchImages",
                description = "Searches gallery images by query or tag",
                parameters = mapOf("query" to "String", "tag" to "String?"),
                requiredParameters = listOf("query")
            )
        ) { params ->
            val query = params["query"] ?: ""
            ToolExecutionResult.Success("searchImages", "Images found matching $query")
        }

        registerTool(
            ToolSchema(
                name = "filterByTag",
                description = "Filters memories or gallery items by tag",
                parameters = mapOf("tag" to "String"),
                requiredParameters = listOf("tag")
            )
        ) { params ->
            val tag = params["tag"] ?: ""
            ToolExecutionResult.Success("filterByTag", "Filtered items by tag $tag")
        }

        registerTool(
            ToolSchema(
                name = "linkMemoryConcept",
                description = "Links a memory to a concept tag",
                parameters = mapOf("memoryId" to "Long", "conceptTag" to "String"),
                requiredParameters = listOf("memoryId", "conceptTag")
            )
        ) { params ->
            val memoryId = params["memoryId"] ?: ""
            val concept = params["conceptTag"] ?: ""
            ToolExecutionResult.Success("linkMemoryConcept", "Linked memory $memoryId to concept $concept")
        }

        registerTool(
            ToolSchema(
                name = "dispatchWebSearch",
                description = "Delegates web search query to search provider or browser",
                parameters = mapOf("query" to "String"),
                requiredParameters = listOf("query")
            )
        ) { params ->
            val query = params["query"] ?: ""
            ToolExecutionResult.Success("dispatchWebSearch", "Dispatched web search for $query")
        }
    }

    fun registerTool(
        schema: ToolSchema,
        executor: (Map<String, String>) -> ToolExecutionResult
    ) {
        tools[schema.name] = Pair(schema, executor)
    }

    fun parsePrompt(prompt: String): FunctionCall {
        val trimmed = prompt.trim()

        // 1. searchImages: e.g. "find pictures of sunsets" or "search images of cats"
        val searchMatcher = SEARCH_IMAGE_PATTERN.matcher(trimmed)
        if (searchMatcher.find()) {
            val query = searchMatcher.group(1)?.trim() ?: ""
            return FunctionCall("searchImages", mapOf("query" to query))
        }

        // 2. filterByTag: e.g. "filter tags by vacation" or "filter by tag vacation"
        val filterMatcher = FILTER_PATTERN.matcher(trimmed)
        if (filterMatcher.find()) {
            val tag = filterMatcher.group(1)?.trim() ?: ""
            return FunctionCall("filterByTag", mapOf("tag" to tag))
        }

        // 3. linkMemoryConcept: e.g. "link memory 42 with biology"
        val linkMatcher = LINK_PATTERN.matcher(trimmed)
        if (linkMatcher.find()) {
            val memoryId = linkMatcher.group(1)?.trim() ?: ""
            val conceptTag = linkMatcher.group(2)?.trim() ?: ""
            return FunctionCall("linkMemoryConcept", mapOf("memoryId" to memoryId, "conceptTag" to conceptTag))
        }

        // 4. dispatchWebSearch: e.g. "search google for latest android release" or "google latest android release"
        val webMatcher = WEB_SEARCH_PATTERN.matcher(trimmed)
        if (webMatcher.find()) {
            val query = webMatcher.group(1)?.trim() ?: ""
            return FunctionCall("dispatchWebSearch", mapOf("query" to query))
        }

        // Default / fallback function call
        return FunctionCall("searchImages", mapOf("query" to trimmed))
    }

    fun execute(call: FunctionCall): ToolExecutionResult {
        val entry = tools[call.name]
            ?: return ToolExecutionResult.Failure(call.name, "Tool not registered: ${call.name}")

        val (schema, executor) = entry

        for (req in schema.requiredParameters) {
            if (!call.parameters.containsKey(req) || call.parameters[req].isNullOrBlank()) {
                return ToolExecutionResult.Failure(
                    call.name,
                    "Missing required parameter: $req"
                )
            }
        }

        return try {
            executor(call.parameters)
        } catch (e: Exception) {
            ToolExecutionResult.Failure(call.name, e.message ?: "Execution failed")
        }
    }

    companion object {
        private val SEARCH_IMAGE_PATTERN: Pattern =
            Pattern.compile("(?i)(?:find pictures of|search images of|find images of|search images)\\s+(.+)")
        private val FILTER_PATTERN: Pattern =
            Pattern.compile("(?i)filter(?:\\s+tags)?\\s+by\\s+(?:tag\\s+)?([\\w-]+)")
        private val LINK_PATTERN: Pattern =
            Pattern.compile("(?i)link memory\\s+(\\d+)\\s+(?:with|to)\\s+(.+)")
        private val WEB_SEARCH_PATTERN: Pattern =
            Pattern.compile("(?i)(?:search google for|google for|google|web search)\\s+(.+)")
    }
}
