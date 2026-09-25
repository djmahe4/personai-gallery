package com.personai.agent

import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class UIAutomationHelper {

    fun findNodeByViewId(root: AccessibilityNodeInfo?, viewId: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes?.firstOrNull()
    }

    fun findNodeByText(root: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }

    fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun performScroll(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun buildWebSearchUrl(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return "https://www.google.com/search?q=$encodedQuery"
    }

    fun buildWebSearchIntent(query: String): Intent {
        val url = buildWebSearchUrl(query)
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
