package com.personai.persona

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class PersonaTrackingPreferencesFragment : Fragment() {
  private var trackedPackages: Set<String> = emptySet()
  private var trackedPeople: Set<String> = emptySet()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    return TextView(requireContext()).apply {
      text = "Persona tracking preferences"
    }
  }

  fun updateTrackedPackages(packages: Set<String>) {
    trackedPackages = packages
  }

  fun updateTrackedPeople(people: Set<String>) {
    trackedPeople = people
  }

  fun currentSelection(): PersonaTrackingSelection {
    return PersonaTrackingSelection(
        trackedPackages = trackedPackages,
        trackedPeople = trackedPeople,
    )
  }
}

data class PersonaTrackingSelection(
    val trackedPackages: Set<String>,
    val trackedPeople: Set<String>,
)
