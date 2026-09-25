package com.personai.agent

import android.util.Log

/** PII-safe logger for PersonAI packages. */
object PersonAILogger {
  private const val REDACTED = "[REDACTED]"

  fun d(tag: String, message: String) = Log.d(tag, sanitize(message))

  fun i(tag: String, message: String) = Log.i(tag, sanitize(message))

  fun w(tag: String, message: String) = Log.w(tag, sanitize(message))

  fun e(tag: String, message: String, throwable: Throwable? = null) =
      Log.e(tag, sanitize(message), throwable)

  fun sanitize(message: String): String {
    return message
        .replace(Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE), REDACTED)
        .replace(Regex("\\b\\d{10,}\\b"), REDACTED)
  }
}
