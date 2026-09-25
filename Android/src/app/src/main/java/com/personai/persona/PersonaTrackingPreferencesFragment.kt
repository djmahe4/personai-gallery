package com.personai.persona

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class PersonaTrackingPreferencesFragment : Fragment() {
  private var trackedPackages: Set<String> = emptySet()
  private var trackedPeople: Set<String> = emptySet()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    context?.let { ctx ->
      val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      trackedPackages = prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
      trackedPeople = prefs.getStringSet(KEY_PEOPLE, emptySet()) ?: emptySet()
    }
  }

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
    context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putStringSet(KEY_PACKAGES, packages)?.apply()
  }

  fun updateTrackedPeople(people: Set<String>) {
    trackedPeople = people
    context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putStringSet(KEY_PEOPLE, people)?.apply()
  }

  fun currentSelection(): PersonaTrackingSelection {
    return PersonaTrackingSelection(
        trackedPackages = trackedPackages,
        trackedPeople = trackedPeople,
    )
  }

  companion object {
    const val PREFS_NAME = "personai_tracking_prefs"
    const val KEY_PACKAGES = "tracked_packages"
    const val KEY_PEOPLE = "tracked_people"

    fun getSelection(context: Context): PersonaTrackingSelection {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return PersonaTrackingSelection(
        trackedPackages = prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet(),
        trackedPeople = prefs.getStringSet(KEY_PEOPLE, emptySet()) ?: emptySet()
      )
    }
  }
}

data class PersonaTrackingSelection(
    val trackedPackages: Set<String>,
    val trackedPeople: Set<String>,
)
