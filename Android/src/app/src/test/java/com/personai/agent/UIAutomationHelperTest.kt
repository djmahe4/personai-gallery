package com.personai.agent

import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UIAutomationHelperTest {

    private lateinit var helper: UIAutomationHelper
    private lateinit var rootNode: AccessibilityNodeInfo

    @Before
    fun setUp() {
        helper = UIAutomationHelper()
        rootNode = mockk(relaxed = true)
    }

    @Test
    fun testFindNodeByViewIdAndText() {
        val childNode1 = mockk<AccessibilityNodeInfo>(relaxed = true)
        val childNode2 = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { rootNode.findAccessibilityNodeInfosByViewId("com.example:id/submit_button") } returns listOf(childNode1)
        every { rootNode.findAccessibilityNodeInfosByText("Search") } returns listOf(childNode2)

        val foundById = helper.findNodeByViewId(rootNode, "com.example:id/submit_button")
        assertNotNull(foundById)
        assertEquals(childNode1, foundById)

        val foundByText = helper.findNodeByText(rootNode, "Search")
        assertNotNull(foundByText)
        assertEquals(childNode2, foundByText)

        // Missing returns null without error
        every { rootNode.findAccessibilityNodeInfosByViewId("com.example:id/non_existent") } returns emptyList()
        assertNull(helper.findNodeByViewId(rootNode, "com.example:id/non_existent"))
    }

    @Test
    fun testPerformClickAndScrollActions() {
        val targetNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK) } returns true
        every { targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) } returns true

        val clickSuccess = helper.performClick(targetNode)
        assertTrue(clickSuccess)
        verify { targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

        val scrollSuccess = helper.performScroll(targetNode)
        assertTrue(scrollSuccess)
        verify { targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) }

        // Null node returns false
        assertFalse(helper.performClick(null))
        assertFalse(helper.performScroll(null))
    }

    @Test
    fun testGenerateWebSearchFallbackUrl() {
        val url = helper.buildWebSearchUrl("google pixel 9 pro")
        assertEquals("https://www.google.com/search?q=google+pixel+9+pro", url)

        val intent = helper.buildWebSearchIntent("gemini nano")
        assertEquals("android.intent.action.VIEW", intent.action)
        assertEquals("https://www.google.com/search?q=gemini+nano", intent.data.toString())
    }
}
