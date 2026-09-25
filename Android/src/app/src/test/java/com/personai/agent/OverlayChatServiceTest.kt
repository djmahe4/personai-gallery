package com.personai.agent

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverlayChatServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowSettings.setCanDrawOverlays(true)
    }

    @Test
    fun testOverlayLayoutParamsConfiguration() {
        val service = Robolectric.buildService(OverlayChatService::class.java).create().get()
        val params = service.createOverlayLayoutParams()

        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, params.type)
        assertEquals(PixelFormat.TRANSLUCENT, params.format)
        val expectedFlags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        assertEquals(expectedFlags, params.flags and expectedFlags)
        assertEquals(Gravity.TOP or Gravity.START, params.gravity)
    }

    @Test
    fun testOverlayLifecycleAndShowHide() {
        val serviceController = Robolectric.buildService(OverlayChatService::class.java).create()
        val service = serviceController.get()

        assertFalse(service.isOverlayVisible)
        assertNull(service.overlayView)

        // Show overlay
        service.showOverlay()
        assertTrue(service.isOverlayVisible)
        assertNotNull(service.overlayView)

        // Toggle chat expansion
        assertFalse(service.isChatExpanded)
        service.toggleChatExpansion()
        assertTrue(service.isChatExpanded)
        service.toggleChatExpansion()
        assertFalse(service.isChatExpanded)

        // Hide overlay
        service.hideOverlay()
        assertFalse(service.isOverlayVisible)
        assertNull(service.overlayView)

        // Destroy service cleans up
        serviceController.destroy()
        assertFalse(service.isOverlayVisible)
    }

    @Test
    fun testOverlayPermissionDeniedDoesNotShow() {
        ShadowSettings.setCanDrawOverlays(false)
        val service = Robolectric.buildService(OverlayChatService::class.java).create().get()

        assertFalse(service.canDrawOverlay())
        val shown = service.showOverlay()
        assertFalse(shown)
        assertFalse(service.isOverlayVisible)
        assertNull(service.overlayView)
    }
}
