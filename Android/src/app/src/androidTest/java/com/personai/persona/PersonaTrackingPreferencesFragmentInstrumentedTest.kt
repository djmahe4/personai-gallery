package com.personai.persona

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonaTrackingPreferencesFragmentInstrumentedTest {

  @Test
  fun fragmentClassIsResolvableForUiSurface() {
    assertNotNull(PersonaTrackingPreferencesFragment())
  }
}
