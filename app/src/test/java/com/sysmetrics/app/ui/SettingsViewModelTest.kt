package com.sysmetrics.app.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel.
 * Tests configuration management and state updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var manageOverlayConfigUseCase: ManageOverlayConfigUseCase
    private lateinit var viewModel: SettingsViewModel

    private val defaultConfig = OverlayConfig.DEFAULT

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        manageOverlayConfigUseCase = mockk(relaxed = true)
        
        coEvery { manageOverlayConfigUseCase.observeConfig() } returns flowOf(defaultConfig)
        
        viewModel = SettingsViewModel(manageOverlayConfigUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateConfig should modify pending config`() {
        // When
        viewModel.updateConfig(showCpu = false)

        // Then
        assertTrue(viewModel.configModified.value)
    }

    @Test
    fun `updateConfig with position should update pending position`() {
        // When
        viewModel.updateConfig(position = OverlayPosition.TOP_RIGHT)

        // Then
        assertTrue(viewModel.configModified.value)
    }

    @Test
    fun `saveConfig should call use case and reset modified flag`() = runTest {
        // Given
        viewModel.updateConfig(showCpu = false)
        assertTrue(viewModel.configModified.value)

        // When
        viewModel.saveConfig()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { manageOverlayConfigUseCase.saveConfig(any()) }
        assertFalse(viewModel.configModified.value)
    }

    @Test
    fun `discardChanges should reset modified flag`() {
        // Given
        viewModel.updateConfig(showRam = false)
        assertTrue(viewModel.configModified.value)

        // When
        viewModel.discardChanges()

        // Then
        assertFalse(viewModel.configModified.value)
    }

    @Test
    fun `multiple updateConfig calls should accumulate changes`() {
        // When
        viewModel.updateConfig(showCpu = false)
        viewModel.updateConfig(showRam = false)
        viewModel.updateConfig(showTime = true)

        // Then
        assertTrue(viewModel.configModified.value)
    }

    @Test
    fun `overlayConfig flow should emit default value initially`() = runTest {
        // Then
        assertEquals(defaultConfig, viewModel.overlayConfig.value)
    }
}
