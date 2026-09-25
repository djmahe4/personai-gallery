package com.personai.memory

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Android WorkManager CoroutineWorker for periodic concept revision passes.
 * Evaluates memory retention decay and flags items due for spaced revision.
 * Standard (Context, WorkerParameters) constructor with lazy Room database singleton.
 */
class ConceptRevisionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database: MemoryDatabase by lazy {
        MemoryDatabase.getInstance(applicationContext)
    }

    private val decayEngine = EbbinghausDecayEngine()

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        Log.d(TAG, "Starting ConceptRevisionWorker pass at timestamp $now")

        return try {
            val dao = database.memoryDao()
            val allMemories = dao.getAll()
            var revisionCandidateCount = 0
            val updatedMemories = ArrayList<MemoryItemEntity>(allMemories.size)

            for (memory in allMemories) {
                val decayedMemory = decayEngine.applyDecay(memory, now)
                if (decayEngine.isDueForRevision(decayedMemory, now)) {
                    revisionCandidateCount++
                    Log.i(
                        TAG,
                        "Memory #${memory.id} due for revision: tag=${memory.conceptTag}, retention=${decayedMemory.retentionScore}"
                    )
                }
                updatedMemories.add(decayedMemory)
            }

            if (updatedMemories.isNotEmpty()) {
                dao.updateAll(updatedMemories)
            }

            Log.d(TAG, "Completed ConceptRevisionWorker pass. Found $revisionCandidateCount candidate(s) for revision.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running concept revision worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ConceptRevisionWorker"
    }
}
