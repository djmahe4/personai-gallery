package com.personai.queue

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

data class WorkItem(
    val id: String,
    val isHighPriority: Boolean = false,
    val action: suspend () -> Unit
)

class AdaptiveWorkQueue(
    private val governor: CpuPacingGovernor,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val queue = ConcurrentLinkedQueue<WorkItem>()
    private val triggerChannel = Channel<Unit>(Channel.CONFLATED)
    private var workerJob: Job? = null

    fun start() {
        if (workerJob != null) return
        workerJob = coroutineScope.launch(dispatcher) {
            while (isActive) {
                // If queue is empty, wait for a signal
                if (queue.isEmpty()) {
                    triggerChannel.receive()
                }

                // Find candidate job or wait if execution conditions not met
                val iterator = queue.iterator()
                var nextItem: WorkItem? = null
                while (iterator.hasNext()) {
                    val candidate = iterator.next()
                    if (governor.canExecuteBackgroundJob(candidate.isHighPriority)) {
                        nextItem = candidate
                        iterator.remove()
                        break
                    }
                }

                if (nextItem != null) {
                    try {
                        nextItem.action.invoke()
                    } catch (t: Throwable) {
                        // Log or handle failure
                    }

                    // Apply CPU throttle / pacing delay between jobs
                    val pacingDelay = governor.computePacingDelayMs()
                    if (pacingDelay > 0) {
                        delay(pacingDelay)
                    }
                } else if (queue.isNotEmpty()) {
                    // Queue has items, but conditions do not permit execution (e.g., throttled/paused)
                    delay(100)
                }
            }
        }
    }

    fun enqueue(item: WorkItem) {
        queue.add(item)
        triggerChannel.trySend(Unit)
    }

    fun stop() {
        workerJob?.cancel()
        workerJob = null
    }
}
