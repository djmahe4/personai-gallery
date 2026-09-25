package com.personai.activity

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker analyzing device usage activity and routine patterns.
 * Adheres to battery, idle, and doze mode deferrals.
 */
class ActivityAnalyzerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val patternModel = UsagePatternModel()

    override suspend fun doWork(): Result {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isDoze = powerManager?.isDeviceIdleMode == true

        if (!shouldAnalyze(isDozeMode = isDoze)) {
            Log.d(TAG, "Device in doze mode or low battery; deferring analysis.")
            return Result.retry()
        }

        val appOps = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
        val mode = appOps?.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            applicationContext.packageName
        )
        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            Log.w(TAG, "UsageStats permission not granted; deferring activity analysis.")
            return Result.failure()
        }

        return try {
            val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val endMillis = System.currentTimeMillis()
            val startMillis = endMillis - TimeUnit.DAYS.toMillis(1)

            val timestamps = mutableListOf<Long>()
            val events = usageStatsManager?.queryEvents(startMillis, endMillis)
            if (events != null) {
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.USER_INTERACTION
                    ) {
                        timestamps.add(event.timeStamp)
                    }
                }
            }

            val utcZone = java.time.ZoneOffset.UTC
            val peakHours = patternModel.detectHourlyPeaks(timestamps, zoneId = utcZone)
            val focusWindow = patternModel.recommendFocusWindow(timestamps, zoneId = utcZone)

            Log.i(TAG, "Activity analysis complete: peaks=$peakHours, suggested focus=$focusWindow")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing activity analyzer", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ActivityAnalyzerWorker"
        const val WORK_NAME = "personai_activity_analyzer_work"
        private const val MIN_BATTERY_PERCENT = 15

        /**
         * Evaluates whether background analysis should proceed based on doze mode and battery state.
         */
        fun shouldAnalyze(isDozeMode: Boolean, batteryPercent: Int = 100): Boolean {
            if (isDozeMode) return false
            if (batteryPercent < MIN_BATTERY_PERCENT) return false
            return true
        }

        /**
         * Builds standard WorkManager constraints: requires idle and battery not low.
         */
        fun createWorkConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        }

        /**
         * Enqueues periodic activity analyzer work.
         */
        fun enqueuePeriodic(context: Context, repeatIntervalHours: Long = 12) {
            val workRequest = PeriodicWorkRequestBuilder<ActivityAnalyzerWorker>(
                repeatIntervalHours,
                TimeUnit.HOURS
            ).setConstraints(createWorkConstraints()).build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
