package com.personai.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class AdaptiveWorkQueueTest {

    private lateinit var monitor: ThermalBatteryMonitor
    private lateinit var governor: CpuPacingGovernor
    private lateinit var scope: CoroutineScope
    private lateinit var workQueue: AdaptiveWorkQueue

    @Before
    fun setUp() {
        monitor = ThermalBatteryMonitor()
        governor = CpuPacingGovernor(monitor)
        scope = CoroutineScope(Dispatchers.Default)
        workQueue = AdaptiveWorkQueue(
            governor = governor,
            coroutineScope = scope,
            dispatcher = Dispatchers.Default
        )
    }

    @Test
    fun testBoundedSequentialProcessing_concurrencyIsOne() {
        val concurrentCount = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)
        val completedCount = AtomicInteger(0)

        workQueue.start()

        val job: suspend () -> Unit = {
            val count = concurrentCount.incrementAndGet()
            if (count > maxObservedConcurrency.get()) {
                maxObservedConcurrency.set(count)
            }
            Thread.sleep(50)
            concurrentCount.decrementAndGet()
            completedCount.incrementAndGet()
        }

        workQueue.enqueue(WorkItem(id = "1", action = job))
        workQueue.enqueue(WorkItem(id = "2", action = job))
        workQueue.enqueue(WorkItem(id = "3", action = job))

        // Wait for all 3 tasks to finish
        val start = System.currentTimeMillis()
        while (completedCount.get() < 3 && System.currentTimeMillis() - start < 3000) {
            Thread.sleep(20)
        }

        assertEquals(3, completedCount.get())
        assertEquals(1, maxObservedConcurrency.get())
        workQueue.stop()
    }

    @Test
    fun testInterJobDelayInjectionGovernedByGovernor() {
        monitor.updateThermalStatus(ThermalStatus.MODERATE) // 250ms throttle delay
        workQueue.start()

        val executionTimestamps = CopyOnWriteArrayList<Long>()
        val action: suspend () -> Unit = {
            executionTimestamps.add(System.currentTimeMillis())
        }

        workQueue.enqueue(WorkItem(id = "1", action = action))
        workQueue.enqueue(WorkItem(id = "2", action = action))

        val start = System.currentTimeMillis()
        while (executionTimestamps.size < 2 && System.currentTimeMillis() - start < 3000) {
            Thread.sleep(20)
        }

        assertEquals(2, executionTimestamps.size)
        val delayObserved = executionTimestamps[1] - executionTimestamps[0]
        assertTrue("Expected delay >= 200ms but was $delayObserved", delayObserved >= 200L)
        workQueue.stop()
    }

    @Test
    fun testPauseAndResumeOfNonCriticalJobsWhenBatteryLow() {
        workQueue.start()
        val completed = CopyOnWriteArrayList<String>()

        monitor.updateBattery(level = 15, isCharging = false) // low battery discharges -> pause non-critical

        workQueue.enqueue(WorkItem(id = "non-critical", isHighPriority = false, action = {
            completed.add("non-critical")
        }))

        Thread.sleep(250)
        assertTrue(completed.isEmpty())

        // Resume by charging
        monitor.updateBattery(level = 15, isCharging = true)
        val start = System.currentTimeMillis()
        while (completed.isEmpty() && System.currentTimeMillis() - start < 3000) {
            Thread.sleep(20)
        }

        assertEquals(listOf("non-critical"), completed)
        workQueue.stop()
    }

    @Test
    fun testHighPriorityJobBypassesThermalOrBatteryThrottle() {
        monitor.updateThermalStatus(ThermalStatus.SEVERE)
        workQueue.start()

        val completed = CopyOnWriteArrayList<String>()

        workQueue.enqueue(WorkItem(id = "high-prio", isHighPriority = true, action = {
            completed.add("high-prio")
        }))

        val start = System.currentTimeMillis()
        while (completed.isEmpty() && System.currentTimeMillis() - start < 3000) {
            Thread.sleep(20)
        }

        assertEquals(listOf("high-prio"), completed)
        workQueue.stop()
    }
}
