package com.personai.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentCoreTest {

    private lateinit var agentCore: AgentCore

    @Before
    fun setUp() {
        agentCore = AgentCore()
    }

    @Test
    fun testParseNaturalLanguagePromptToFunctionSchema() {
        // 1. searchImages
        val searchCall = agentCore.parsePrompt("find pictures of sunsets")
        assertEquals("searchImages", searchCall.name)
        assertEquals("sunsets", searchCall.parameters["query"])

        // 2. filterByTag
        val filterCall = agentCore.parsePrompt("filter tags by vacation")
        assertEquals("filterByTag", filterCall.name)
        assertEquals("vacation", filterCall.parameters["tag"])

        // 3. linkMemoryConcept
        val linkCall = agentCore.parsePrompt("link memory 42 with biology")
        assertEquals("linkMemoryConcept", linkCall.name)
        assertEquals("42", linkCall.parameters["memoryId"])
        assertEquals("biology", linkCall.parameters["conceptTag"])

        // 4. dispatchWebSearch
        val webCall = agentCore.parsePrompt("search google for latest android release")
        assertEquals("dispatchWebSearch", webCall.name)
        assertEquals("latest android release", webCall.parameters["query"])
    }

    @Test
    fun testToolDispatchSuccessResults() {
        var searchedQuery: String? = null
        var searchedTag: String? = null
        agentCore.registerTool(
            ToolSchema(
                name = "searchImages",
                description = "Searches gallery images",
                parameters = mapOf("query" to "String", "tag" to "String?")
            )
        ) { params ->
            searchedQuery = params["query"]
            searchedTag = params["tag"]
            ToolExecutionResult.Success(
                toolName = "searchImages",
                output = "Found 3 images matching $searchedQuery"
            )
        }

        val result = agentCore.execute(
            FunctionCall(
                name = "searchImages",
                parameters = mapOf("query" to "mountains", "tag" to "nature")
            )
        )

        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertEquals("searchImages", success.toolName)
        assertEquals("Found 3 images matching mountains", success.output)
        assertEquals("mountains", searchedQuery)
        assertEquals("nature", searchedTag)
    }

    @Test
    fun testToolDispatchUnknownToolFallback() {
        val result = agentCore.execute(
            FunctionCall(
                name = "unknownTool",
                parameters = emptyMap()
            )
        )

        assertTrue(result is ToolExecutionResult.Failure)
        val failure = result as ToolExecutionResult.Failure
        assertEquals("unknownTool", failure.toolName)
        assertTrue(failure.error.contains("Tool not registered"))
    }

    @Test
    fun testToolValidationMissingRequiredParameter() {
        agentCore.registerTool(
            ToolSchema(
                name = "filterByTag",
                description = "Filter items by tag",
                parameters = mapOf("tag" to "String"),
                requiredParameters = listOf("tag")
            )
        ) { params ->
            ToolExecutionResult.Success("filterByTag", "Filtered by ${params["tag"]}")
        }

        val result = agentCore.execute(
            FunctionCall(
                name = "filterByTag",
                parameters = emptyMap()
            )
        )

        assertTrue(result is ToolExecutionResult.Failure)
        val failure = result as ToolExecutionResult.Failure
        assertTrue(failure.error.contains("Missing required parameter: tag"))
    }
}
