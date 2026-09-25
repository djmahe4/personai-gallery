package com.personai.memory

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

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
        MemoryDatabase.getInstance(appContext)
    }

    private val decayEngine = EbbinghausDecayEngine()

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        Log.d(TAG, "Starting ConceptRevisionWorker pass at timestamp $now")

        return try {
            val dao = database.memoryDao()
            // Query only items due for revision or with pending reviews instead of entire table
            val dueMemories = dao.getDueForRevision(now)
            var revisionCandidateCount = 0
            val updatedMemories = ArrayList<MemoryItemEntity>(dueMemories.size)

            for (memory in dueMemories) {
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
        const val WORK_NAME = "personai_concept_revision_work"

        /**
         * Enqueues periodic concept revision with strict resource constraints.
         */
        fun enqueuePeriodic(context: Context, repeatIntervalHours: Long = 6) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ConceptRevisionWorker>(
                repeatIntervalHours,
                TimeUnit.HOURS
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
