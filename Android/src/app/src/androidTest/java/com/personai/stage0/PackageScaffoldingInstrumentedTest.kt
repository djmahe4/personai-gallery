package com.personai.stage0

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageScaffoldingInstrumentedTest {

  @Test
  fun appCanResolvePersonAiAndGalleryTypes() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    assertNotNull(appContext)
    assertNotNull(Class.forName("com.personai.agent.AgentPackageMarker"))
    assertNotNull(Class.forName("com.google.ai.edge.gallery.MainActivity"))
  }
}
