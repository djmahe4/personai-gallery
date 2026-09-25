package com.personai.memory

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stage 2 Memory & Concept Revision UI Fragment.
 * Displays spaced concept revision counts and allows manual trigger or query inspection.
 */
class MemoryRevisionDashboardFragment : Fragment() {

    private var repository: MemoryRepository? = null
    private var dueCount: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val db = MemoryDatabase.getInstance(context)
        repository = MemoryRepository(db.memoryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (repository == null) {
            context?.let { ctx ->
                val db = MemoryDatabase.getInstance(ctx)
                repository = MemoryRepository(db.memoryDao())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            id = android.R.id.text1
            text = "Due Concept Revisions: $dueCount"
        }
    }

    fun refreshDueRevisions(scope: CoroutineScope, currentTime: Long = System.currentTimeMillis(), onLoaded: (Int) -> Unit = {}) {
        val repo = repository ?: context?.let { ctx ->
            MemoryRepository(MemoryDatabase.getInstance(ctx).memoryDao()).also { repository = it }
        } ?: return

        scope.launch(Dispatchers.Default) {
            val due = repo.getPendingRevisions(currentTime)
            withContext(Dispatchers.Main) {
                dueCount = due.size
                view?.findViewById<TextView>(android.R.id.text1)?.text = "Due Concept Revisions: $dueCount"
                onLoaded(dueCount)
            }
        }
    }

    fun currentDueCount(): Int = dueCount
}
