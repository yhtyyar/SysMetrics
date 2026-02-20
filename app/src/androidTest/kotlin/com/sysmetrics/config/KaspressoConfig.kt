package com.sysmetrics.config

import com.kaspersky.components.kaspresso.configurator.Configurator
import com.kaspersky.components.kaspresso.configurator.KaspressoConfigurator
import com.kaspersky.components.kaspresso.configurator.builder.ConfiguratorBuilder
import com.kaspersky.components.kaspresso.params.Params
import com.kaspersky.components.kaspresso.params.StepParams
import com.kaspersky.components.kaspresso.params.TestCaseParams

/**
 * Custom Kaspresso configuration
 * Provides enhanced logging, screenshot capture, and error handling
 */
object KaspressoConfig {
    
    /**
     * Enhanced configurator with basic setup
     */
    fun createEnhancedConfigurator(): Configurator {
        return ConfiguratorBuilder.advanced()
            .withStepParams(
                StepParams(
                    stepTimeoutMs = 10_000L,
                    flakySafetyTimeoutMs = 5_000L
                )
            )
            .withTestCaseParams(
                TestCaseParams(
                    TestCaseParams.DEFAULT_TEST_CASE_TIMEOUT_MS,
                    TestCaseParams.DEFAULT_TEST_CASE_ATTEMPTS_COUNT
                )
            )
            .build()
    }
    
    /**
     * Configuration for visual testing with delays
     */
    fun createVisualTestingConfigurator(): Configurator {
        return ConfiguratorBuilder.advanced()
            .withStepParams(
                StepParams(
                    stepTimeoutMs = 15_000L, // Longer timeout for visual testing
                    flakySafetyTimeoutMs = 8_000L
                )
            )
            .withTestCaseParams(
                TestCaseParams(
                    timeoutMs = 60_000L, // 1 minute per test
                    attemptsCount = 2
                )
            )
            .build()
    }
    
    /**
     * Configuration for CI/CD environments
     */
    fun createCiConfigurator(): Configurator {
        return ConfiguratorBuilder.advanced()
            .withStepParams(
                StepParams(
                    stepTimeoutMs = 30_000L, // Longer timeout for CI
                    flakySafetyTimeoutMs = 15_000L
                )
            )
            .withTestCaseParams(
                TestCaseParams(
                    timeoutMs = 120_000L, // 2 minutes per test for CI
                    attemptsCount = 3 // More attempts in CI
                )
            )
            .build()
    }
}
