package com.sysmetrics.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.domain.collector.ICpuMetricsCollector
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Before

/**
 * Base test class for ProcessStatsCollector tests.
 * Provides common mocking setup.
 */
@ExperimentalCoroutinesApi
abstract class ProcessStatsCollectorTestBase {

    protected lateinit var context: Context
    protected lateinit var packageManager: PackageManager
    protected lateinit var cpuMetricsCollector: ICpuMetricsCollector
    protected lateinit var dispatcherProvider: DispatcherProvider
    protected lateinit var testDispatcher: TestDispatcher

    protected lateinit var collector: ProcessStatsCollector

    @Before
    fun setup() {
        // Mock Android context and system services
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        cpuMetricsCollector = mockk(relaxed = true)

        every { context.packageManager } returns packageManager

        // Mock Process.myPid()
        mockkStatic(Process::class)
        every { Process.myPid() } returns 12345

        // Setup test dispatcher
        testDispatcher = StandardTestDispatcher()
        dispatcherProvider = mockk(relaxed = true)
        every { dispatcherProvider.io } returns testDispatcher

        // Create collector with mocked dependencies
        collector = ProcessStatsCollector(context, dispatcherProvider, cpuMetricsCollector)
    }

    @After
    fun tearDown() {
        // Clear any static mocks
    }
}
