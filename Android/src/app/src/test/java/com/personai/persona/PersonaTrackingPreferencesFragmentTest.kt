package com.personai.persona

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonaTrackingPreferencesFragmentTest {

  @Test
  fun preferencesStoreSelectedPackagesAndPeople() {
    val fragment = PersonaTrackingPreferencesFragment()

    fragment.updateTrackedPackages(setOf("com.slack", "com.google.android.gm"))
    fragment.updateTrackedPeople(setOf("person_a", "person_b"))

    val snapshot = fragment.currentSelection()
    assertEquals(setOf("com.slack", "com.google.android.gm"), snapshot.trackedPackages)
    assertEquals(setOf("person_a", "person_b"), snapshot.trackedPeople)
  }
}
