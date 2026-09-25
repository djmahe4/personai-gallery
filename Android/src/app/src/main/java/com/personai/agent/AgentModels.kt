package com.personai.agent

data class ToolSchema(
    val name: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
    val requiredParameters: List<String> = emptyList()
)

data class FunctionCall(
    val name: String,
    val parameters: Map<String, String> = emptyMap()
)

sealed class ToolExecutionResult {
    abstract val toolName: String

    data class Success(
        override val toolName: String,
        val output: String,
        val metadata: Map<String, Any?> = emptyMap()
    ) : ToolExecutionResult()

    data class Failure(
        override val toolName: String,
        val error: String
    ) : ToolExecutionResult()
}

enum class AgentStatus {
    IDLE,
    THINKING,
    EXECUTING_TOOL
}

data class AgentExecutionRecord(
    val id: String,
    val prompt: String,
    val functionCall: FunctionCall,
    val result: ToolExecutionResult,
    val timestamp: Long = System.currentTimeMillis()
)
