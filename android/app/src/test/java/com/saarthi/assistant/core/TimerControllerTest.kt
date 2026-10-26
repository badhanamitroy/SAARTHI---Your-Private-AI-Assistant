package com.saarthi.assistant.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerControllerTest {

    private class TestTimeProvider(var currentTime: Long = 1000000L) : TimeProvider {
        override fun currentTimeMillis(): Long = currentTime
    }

    private lateinit var testTimeProvider: TestTimeProvider

    @Before
    fun setUp() {
        testTimeProvider = TestTimeProvider(1000000L)
        TimerController.resetForTesting()
        TimerController.setTimeProviderForTesting(testTimeProvider, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TimerController.resetForTesting()
    }

    // --- Duration Validation Tests ---

    @Test
    fun startTimer_zeroDuration_returnsHonestFailedContract() {
        val contract = TimerController.startTimer(null, 0)
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("greater than zero"))
        assertEquals(TimerState.IDLE, TimerController.snapshot.value.state)
    }

    @Test
    fun startTimer_negativeDuration_returnsHonestFailedContract() {
        val contract = TimerController.startTimer(null, -45)
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("greater than zero"))
        assertEquals(TimerState.IDLE, TimerController.snapshot.value.state)
    }

    @Test
    fun startTimer_exceeds24Hours_returnsHonestFailedContract() {
        val contract = TimerController.startTimer(null, 86401L)
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("24 hours"))
        assertEquals(TimerState.IDLE, TimerController.snapshot.value.state)
    }

    @Test
    fun startTimer_validDuration_setsRunningStateAndReturnsSucceeded() {
        val contract = TimerController.startTimer(null, 120L)
        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("2 minutes"))
        assertTrue(succeeded.displayFeedback.contains("Timer: 02:00"))

        val snapshot = TimerController.snapshot.value
        assertEquals(TimerState.RUNNING, snapshot.state)
        assertEquals(120L, snapshot.totalSeconds)
        assertEquals(120L, snapshot.remainingSeconds)
        assertEquals("02:00", snapshot.formattedRemaining)
    }

    // --- Duplicate Command Debounce Tests ---

    @Test
    fun startTimer_duplicateCommandWithin1500ms_returnsAlreadyRunningContract() {
        testTimeProvider.currentTime = 1000L
        val firstContract = TimerController.startTimer(null, 60L)
        assertTrue(firstContract is ExecutionContract.Succeeded)

        // Simulate immediate second invocation (e.g. 200ms later) with same duration
        testTimeProvider.currentTime = 1200L
        val duplicateContract = TimerController.startTimer(null, 60L)
        assertTrue(duplicateContract is ExecutionContract.Succeeded)
        val succeeded = duplicateContract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("already running"))
    }

    @Test
    fun startTimer_differentDurationOrAfterDebounceWindow_resetsTimer() {
        testTimeProvider.currentTime = 1000L
        TimerController.startTimer(null, 60L)

        // 3 seconds later, new timer for 90 seconds
        testTimeProvider.currentTime = 4000L
        val newContract = TimerController.startTimer(null, 90L)
        assertTrue(newContract is ExecutionContract.Succeeded)
        val succeeded = newContract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("1 minute, 30 seconds"))
        assertEquals(90L, TimerController.snapshot.value.totalSeconds)
    }

    // --- State Transition Tests: Pause, Resume, Cancel ---

    @Test
    fun pauseTimer_whenRunning_pausesAndPreservesRemaining() {
        TimerController.startTimer(null, 100L)
        TimerController.manualTick(30L)

        val contract = TimerController.pauseTimer()
        assertTrue(contract is ExecutionContract.Succeeded)
        val snapshot = TimerController.snapshot.value
        assertEquals(TimerState.PAUSED, snapshot.state)
        assertEquals(70L, snapshot.remainingSeconds)
    }

    @Test
    fun pauseTimer_whenIdle_returnsFailedContract() {
        val contract = TimerController.pauseTimer()
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("no active timer", ignoreCase = true))
    }

    @Test
    fun resumeTimer_whenPaused_resumesCountdown() {
        TimerController.startTimer(null, 100L)
        TimerController.pauseTimer()

        val contract = TimerController.resumeTimer(null)
        assertTrue(contract is ExecutionContract.Succeeded)
        assertEquals(TimerState.RUNNING, TimerController.snapshot.value.state)
    }

    @Test
    fun resumeTimer_whenAlreadyRunning_returnsAlreadyRunningContract() {
        TimerController.startTimer(null, 100L)

        val contract = TimerController.resumeTimer(null)
        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("already running"))
    }

    @Test
    fun cancelTimer_whenActive_resetsToIdle() {
        TimerController.startTimer(null, 100L)
        val contract = TimerController.cancelTimer()
        assertTrue(contract is ExecutionContract.Succeeded)
        val snapshot = TimerController.snapshot.value
        assertEquals(TimerState.IDLE, snapshot.state)
        assertEquals(0L, snapshot.remainingSeconds)
    }

    @Test
    fun cancelTimer_whenIdle_returnsNoActiveTimerContract() {
        val contract = TimerController.cancelTimer()
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("no active timer", ignoreCase = true))
    }

    // --- Manual Tick & Completion Tests ---

    @Test
    fun manualTick_decrementsRemainingTime() {
        TimerController.startTimer(null, 60L)
        TimerController.manualTick(10L)

        val snapshot = TimerController.snapshot.value
        assertEquals(50L, snapshot.remainingSeconds)
        assertEquals(TimerState.RUNNING, snapshot.state)
    }

    @Test
    fun manualTick_reachingZero_transitionsToFinishedAndFiresCallback() {
        var finishedFired = false
        TimerController.onTimerFinishedListener = { snapshot ->
            finishedFired = true
            assertEquals(60L, snapshot.totalSeconds)
            assertEquals(0L, snapshot.remainingSeconds)
        }

        TimerController.startTimer(null, 60L)
        TimerController.manualTick(60L)

        val snapshot = TimerController.snapshot.value
        assertEquals(TimerState.FINISHED, snapshot.state)
        assertEquals(0L, snapshot.remainingSeconds)
        assertTrue(finishedFired)
    }

    // --- Status Query Contract Tests ---

    @Test
    fun getStatus_whenIdle_returnsNoTimerContract() {
        val contract = TimerController.getStatus()
        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("no active timer", ignoreCase = true))
    }

    @Test
    fun getStatus_whenRunning_returnsAccurateRemainingSpoken() {
        TimerController.startTimer(null, 150L)
        val contract = TimerController.getStatus()
        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("remaining"))
        assertTrue(succeeded.spokenFeedback.contains("2 minutes, 30 seconds"))
    }

    // --- Formatting Helper Tests ---

    @Test
    fun formatDuration_variousSeconds_returnsMmSsOrHhMmSs() {
        assertEquals("00:00", TimerController.formatDuration(0))
        assertEquals("00:45", TimerController.formatDuration(45))
        assertEquals("02:30", TimerController.formatDuration(150))
        assertEquals("01:05:20", TimerController.formatDuration(3920))
    }

    @Test
    fun formatDurationSpoken_variousSeconds_returnsHumanReadable() {
        assertEquals("0 seconds", TimerController.formatDurationSpoken(0))
        assertEquals("45 seconds", TimerController.formatDurationSpoken(45))
        assertEquals("2 minutes", TimerController.formatDurationSpoken(120))
        assertEquals("2 minutes, 30 seconds", TimerController.formatDurationSpoken(150))
        assertEquals("1 hour, 5 minutes, 20 seconds", TimerController.formatDurationSpoken(3920))
    }

    @Test
    fun progress_calculatesCorrectRatio() {
        TimerController.startTimer(null, 100L)
        assertEquals(0f, TimerController.snapshot.value.progress, 0.001f)

        TimerController.manualTick(50L)
        assertEquals(0.5f, TimerController.snapshot.value.progress, 0.001f)

        TimerController.manualTick(50L)
        assertEquals(1.0f, TimerController.snapshot.value.progress, 0.001f)
    }
}
