package com.sysmetrics.app.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getSystemMetricsUseCase: GetSystemMetricsUseCase
    private lateinit var manageOverlayConfigUseCase: ManageOverlayConfigUseCase
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSystemMetricsUseCase = mock()
        manageOverlayConfigUseCase = mock()

        whenever(manageOverlayConfigUseCase.observeConfig())
            .thenReturn(flowOf(OverlayConfig.DEFAULT))
        whenever(manageOverlayConfigUseCase.observeEnabled())
            .thenReturn(flowOf(false))
        whenever(getSystemMetricsUseCase.invoke(any()))
            .thenReturn(flowOf(SystemMetrics.EMPTY))

        viewModel = MainViewModel(getSystemMetricsUseCase, manageOverlayConfigUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial overlayEnabled is false`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.overlayEnabled.value)
    }

    @Test
    fun `initial overlayConfig is DEFAULT`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(OverlayConfig.DEFAULT, viewModel.overlayConfig.value)
    }

    @Test
    fun `initial systemMetrics is EMPTY`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(SystemMetrics.EMPTY, viewModel.systemMetrics.value)
    }

    @Test
    fun `setOverlayEnabled calls use case`() = runTest {
        viewModel.setOverlayEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify(manageOverlayConfigUseCase).setEnabled(true)
    }

    @Test
    fun `saveConfig calls use case`() = runTest {
        val config = OverlayConfig.DEFAULT.copy(opacity = 0.5f)
        
        viewModel.saveConfig(config)
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify(manageOverlayConfigUseCase).saveConfig(config)
    }

    @Test
    fun `toggleOverlay enables when disabled`() = runTest {
        whenever(manageOverlayConfigUseCase.observeEnabled())
            .thenReturn(flowOf(false))
        
        viewModel.toggleOverlay()
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify(manageOverlayConfigUseCase).setEnabled(true)
    }
}
