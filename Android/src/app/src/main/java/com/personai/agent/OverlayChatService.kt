package com.personai.agent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayChatService : Service() {

    private var windowManager: WindowManager? = null
    var overlayView: View? = null
        private set

    var isOverlayVisible: Boolean = false
        private set

    var isChatExpanded: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
    }

    fun canDrawOverlay(): Boolean {
        return android.provider.Settings.canDrawOverlays(this)
    }

    fun showOverlay(): Boolean {
        if (isOverlayVisible || overlayView != null) return false
        if (!canDrawOverlay()) return false

        val view = FrameLayout(this)
        overlayView = view
        val params = createOverlayLayoutParams()

        return try {
            windowManager?.addView(view, params)
            isOverlayVisible = true
            true
        } catch (e: Exception) {
            overlayView = null
            isOverlayVisible = false
            false
        }
    }

    fun toggleChatExpansion() {
        isChatExpanded = !isChatExpanded
    }

    fun hideOverlay() {
        if (!isOverlayVisible && overlayView == null) return

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                // View might not be attached
            }
        }
        overlayView = null
        isOverlayVisible = false
        isChatExpanded = false
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }
}
