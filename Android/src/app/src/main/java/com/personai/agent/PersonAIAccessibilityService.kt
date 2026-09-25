package com.personai.agent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PersonAIAccessibilityService : AccessibilityService() {

    companion object {
        var instance: PersonAIAccessibilityService? = null
            private set

        fun getRootNode(): AccessibilityNodeInfo? {
            return instance?.rootInActiveWindow
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intercept events if needed for agent awareness
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance == this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
