package com.sysmetrics.app.domain.usecase

import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetSystemMetricsUseCase.
 * Tests metrics collection business logic.
 */
class GetSystemMetricsUseCaseTest {

    private lateinit var repository: ISystemMetricsRepository
    private lateinit var useCase: GetSystemMetricsUseCase

    private val testMetrics = SystemMetrics(
        cpuUsage = 45.5f,
        cpuCores = 4,
        ramUsedMb = 2048L,
        ramTotalMb = 4096L,
        ramUsagePercent = 50f,
        temperatureCelsius = 42.0f,
        timestamp = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = GetSystemMetricsUseCase(repository)
    }

    @Test
    fun `invoke should return metrics flow from repository`() = runTest {
        // Given
        every { repository.getMetricsFlow(1000L) } returns flowOf(testMetrics)

        // When
        val result = useCase(1000L).first()

        // Then
        assertEquals(testMetrics, result)
    }

    @Test
    fun `invoke with default interval should use 1000ms`() = runTest {
        // Given
        every { repository.getMetricsFlow(any()) } returns flowOf(testMetrics)

        // When
        useCase().first()

        // Then
        verify { repository.getMetricsFlow(1000L) }
    }

    @Test
    fun `collectOnce should return single metrics snapshot`() = runTest {
        // Given
        coEvery { repository.collectMetrics() } returns testMetrics

        // When
        val result = useCase.collectOnce()

        // Then
        assertEquals(testMetrics, result)
        coVerify { repository.collectMetrics() }
    }

    @Test
    fun `resetBaseline should delegate to repository`() {
        // When
        useCase.resetBaseline()

        // Then
        verify { repository.resetBaseline() }
    }

    @Test
    fun `metrics should contain valid CPU usage`() = runTest {
        // Given
        every { repository.getMetricsFlow(any()) } returns flowOf(testMetrics)

        // When
        val result = useCase().first()

        // Then
        assertEquals(45.5f, result.cpuUsage, 0.01f)
    }

    @Test
    fun `metrics should contain valid RAM info`() = runTest {
        // Given
        every { repository.getMetricsFlow(any()) } returns flowOf(testMetrics)

        // When
        val result = useCase().first()

        // Then
        assertEquals(2048L, result.ramUsedMb)
        assertEquals(4096L, result.ramTotalMb)
        assertEquals(50f, result.ramUsagePercent, 0.01f)
    }
}
