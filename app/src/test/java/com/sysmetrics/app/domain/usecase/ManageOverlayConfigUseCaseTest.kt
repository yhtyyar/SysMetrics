package com.sysmetrics.app.domain.usecase

import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

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
        coEvery { repository.getOverlayConfig() } returns flowOf(testConfig)

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
        coVerify { repository.saveOverlayConfig(testConfig) }
    }

    @Test
    fun `updatePosition should save config with new position`() = runTest {
        // Given
        coEvery { repository.getOverlayConfig() } returns flowOf(testConfig)

        // When
        useCase.updatePosition(OverlayPosition.BOTTOM_LEFT)

        // Then
        coVerify { 
            repository.saveOverlayConfig(
                match { it.position == OverlayPosition.BOTTOM_LEFT }
            )
        }
    }

    @Test
    fun `toggleMetric should toggle specific metric visibility`() = runTest {
        // Given
        coEvery { repository.getOverlayConfig() } returns flowOf(testConfig)

        // When
        useCase.toggleMetric("cpu")

        // Then
        coVerify {
            repository.saveOverlayConfig(
                match { !it.showCpu } // Should be toggled
            )
        }
    }
}
