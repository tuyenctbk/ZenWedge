package com.example.ui.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerViewModelTest {

    private lateinit var viewModel: TimerViewModel

    @Before
    fun setUp() {
        viewModel = TimerViewModel()
    }

    @Test
    fun testInitialTimerState() {
        assertEquals(TimerState.IDLE, viewModel.timerState.value)
        assertEquals(25 * 60, viewModel.remainingSeconds.value)
        assertEquals(25 * 60, viewModel.totalDurationSeconds.value)
        assertEquals(SessionMode.FOCUS, viewModel.sessionMode.value)
        assertEquals(1, viewModel.pomodoroCycle.value)
    }

    @Test
    fun testSetDurationClamping() {
        // Test lower bound clamping (minimum 60s)
        viewModel.setDuration(10)
        assertEquals(60, viewModel.remainingSeconds.value)

        // Test upper bound clamping (maximum 10800s)
        viewModel.setDuration(20000)
        assertEquals(10800, viewModel.remainingSeconds.value)

        // Test valid duration setting
        viewModel.setDuration(1800)
        assertEquals(1800, viewModel.remainingSeconds.value)
        assertEquals(1800, viewModel.totalDurationSeconds.value)
    }

    @Test
    fun testSetSessionMode() {
        viewModel.setSessionMode(SessionMode.SHORT_BREAK)
        assertEquals(SessionMode.SHORT_BREAK, viewModel.sessionMode.value)
        assertEquals(5 * 60, viewModel.remainingSeconds.value)

        viewModel.setSessionMode(SessionMode.LONG_BREAK)
        assertEquals(SessionMode.LONG_BREAK, viewModel.sessionMode.value)
        assertEquals(15 * 60, viewModel.remainingSeconds.value)

        viewModel.setSessionMode(SessionMode.POMODORO)
        assertEquals(SessionMode.POMODORO, viewModel.sessionMode.value)
        assertEquals(25 * 60, viewModel.remainingSeconds.value)
    }

    @Test
    fun testSelectTheme() {
        viewModel.selectTheme(WedgeTheme.ZEN_PURPLE)
        assertEquals(WedgeTheme.ZEN_PURPLE, viewModel.currentTheme.value)

        viewModel.selectTheme(WedgeTheme.WARM_AMBER)
        assertEquals(WedgeTheme.WARM_AMBER, viewModel.currentTheme.value)
    }

    @Test
    fun testDialogStateDismissals() {
        viewModel.triggerRateApp()
        assertTrue(viewModel.showRatingDialog.value)
        viewModel.dismissRatingDialog()
        assertFalse(viewModel.showRatingDialog.value)

        viewModel.triggerShareApp()
        assertTrue(viewModel.showShareDialog.value)
        viewModel.dismissShareDialog()
        assertFalse(viewModel.showShareDialog.value)

        viewModel.checkAppUpdate()
        assertTrue(viewModel.showUpdateDialog.value)
        viewModel.dismissUpdateDialog()
        assertFalse(viewModel.showUpdateDialog.value)
    }
}
