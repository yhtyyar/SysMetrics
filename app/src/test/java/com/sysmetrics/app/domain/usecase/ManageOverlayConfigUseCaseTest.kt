package com.sysmetrics.app.domain.usecase

import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ManageOverlayConfigUseCase.
 * Tests business logic for overlay configuration management.
 */
class ManageOverlayConfigUseCaseTest {

    private lateinit var repository: IPreferencesRepository
    private lateinit var useCase: ManageOverlayConfigUseCase

    private val testConfig = OverlayConfig(
        positionX = 100,
        positionY = 200,
        position = OverlayPosition.TOP_RIGHT,
        updateIntervalMs = 2000L,
        opacity = 0.9f,
        showCpu = true,
        showRam = true,
        showTemperature = false,
        showTime = true
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = ManageOverlayConfigUseCase(repository)
    }

    @Test
    fun `observeConfig should return config flow from repository`() = runTest {
        // Given
        every { repository.overlayConfig } returns flowOf(testConfig)

        // When
        val result = useCase.observeConfig().first()

        // Then
        assertEquals(testConfig, result)
    }

    @Test
    fun `saveConfig should delegate to repository`() = runTest {
        // When
        useCase.saveConfig(testConfig)

        // Then
        coVerify { repository.saveConfig(testConfig) }
    }

    @Test
    fun `updatePosition should delegate to repository`() = runTest {
        // When
        useCase.updatePosition(150, 250)

        // Then
        coVerify { repository.updatePosition(150, 250) }
    }

    @Test
    fun `setEnabled should delegate to repository`() = runTest {
        // When
        useCase.setEnabled(true)

        // Then
        coVerify { repository.setOverlayEnabled(true) }
    }

    @Test
    fun `observeEnabled should return enabled flow from repository`() = runTest {
        // Given
        every { repository.isOverlayEnabled } returns flowOf(true)

        // When
        val result = useCase.observeEnabled().first()

        // Then
        assertEquals(true, result)
    }
}
