package com.personai.agent

import android.content.Intent
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonAIAccessibilityServiceTest {

    @Test
    fun testLifecycleAndUnbindClearsInstance() {
        val serviceController = Robolectric.buildService(PersonAIAccessibilityService::class.java).create()
        val service = serviceController.get()

        service.onServiceConnected()
        assertSame(service, PersonAIAccessibilityService.instance)

        // onUnbind clears instance
        service.onUnbind(Intent())
        assertNull(PersonAIAccessibilityService.instance)

        // reconnect and destroy
        service.onServiceConnected()
        assertSame(service, PersonAIAccessibilityService.instance)
        serviceController.destroy()
        assertNull(PersonAIAccessibilityService.instance)
    }
}
