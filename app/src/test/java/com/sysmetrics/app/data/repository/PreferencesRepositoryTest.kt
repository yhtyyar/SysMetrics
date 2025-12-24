package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.data.source.PreferencesDataSource
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PreferencesRepository.
 * Tests data layer logic for preferences management.
 */
class PreferencesRepositoryTest {

    private lateinit var dataSource: PreferencesDataSource
    private lateinit var repository: PreferencesRepository

    private val testConfig = OverlayConfig(
        positionX = 50,
        positionY = 100,
        position = OverlayPosition.BOTTOM_RIGHT,
        updateIntervalMs = 1500L,
        opacity = 0.85f,
        showCpu = true,
        showRam = false,
        showTemperature = true,
        showTime = false
    )

    @Before
    fun setup() {
        dataSource = mockk(relaxed = true)
    }

    @Test
    fun `getOverlayConfig should return flow from data source`() = runTest {
        // Given
        every { dataSource.overlayConfig } returns flowOf(testConfig)
        repository = PreferencesRepository(dataSource)

        // When
        val result = repository.overlayConfig.first()

        // Then
        assertEquals(testConfig, result)
    }

    @Test
    fun `saveConfig should delegate to data source`() = runTest {
        // Given
        repository = PreferencesRepository(dataSource)
        
        // When
        repository.saveConfig(testConfig)

        // Then
        coVerify { dataSource.saveOverlayConfig(testConfig) }
    }

    @Test
    fun `isOverlayEnabled should return flow from data source`() = runTest {
        // Given
        every { dataSource.isOverlayEnabled } returns flowOf(true)
        repository = PreferencesRepository(dataSource)

        // When
        val result = repository.isOverlayEnabled.first()

        // Then
        assertTrue(result)
    }

    @Test
    fun `setOverlayEnabled should delegate to data source`() = runTest {
        // Given
        repository = PreferencesRepository(dataSource)
        
        // When
        repository.setOverlayEnabled(true)

        // Then
        coVerify { dataSource.setOverlayEnabled(true) }
    }

    @Test
    fun `default config should have expected values`() {
        // Then
        val default = OverlayConfig.DEFAULT
        assertEquals(20, default.positionX)
        assertEquals(20, default.positionY)
        assertEquals(OverlayPosition.TOP_LEFT, default.position)
        assertTrue(default.showCpu)
        assertTrue(default.showRam)
        assertTrue(default.showTime)
    }
}
