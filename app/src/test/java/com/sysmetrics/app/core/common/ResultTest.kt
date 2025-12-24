package com.sysmetrics.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Result sealed class.
 * Tests error handling and state management.
 */
class ResultTest {

    @Test
    fun `Success should be success`() {
        // Given
        val result = Result.Success("data")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `Error should be error`() {
        // Given
        val result = Result.Error(message = "Error occurred")

        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
    }

    @Test
    fun `getOrNull should return data for Success`() {
        // Given
        val result = Result.Success("test data")

        // When
        val data = result.getOrNull()

        // Then
        assertEquals("test data", data)
    }

    @Test
    fun `getOrNull should return null for Error`() {
        // Given
        val result: Result<String> = Result.Error(message = "Failed")

        // When
        val data = result.getOrNull()

        // Then
        assertNull(data)
    }

    @Test
    fun `getOrDefault should return data for Success`() {
        // Given
        val result = Result.Success(42)

        // When
        val value = result.getOrDefault(0)

        // Then
        assertEquals(42, value)
    }

    @Test
    fun `getOrDefault should return default for Error`() {
        // Given
        val result: Result<Int> = Result.Error(message = "Failed")

        // When
        val value = result.getOrDefault(99)

        // Then
        assertEquals(99, value)
    }

    @Test
    fun `map should transform Success data`() {
        // Given
        val result = Result.Success(10)

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertEquals(20, (mapped as Result.Success).data)
    }

    @Test
    fun `map should preserve Error`() {
        // Given
        val result: Result<Int> = Result.Error(message = "Original error")

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertTrue(mapped is Result.Error)
        assertEquals("Original error", (mapped as Result.Error).message)
    }

    @Test
    fun `onSuccess should execute action for Success`() {
        // Given
        var executed = false
        val result = Result.Success("data")

        // When
        result.onSuccess { executed = true }

        // Then
        assertTrue(executed)
    }

    @Test
    fun `onSuccess should not execute action for Error`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Error(message = "Error")

        // When
        result.onSuccess { executed = true }

        // Then
        assertFalse(executed)
    }

    @Test
    fun `onError should execute action for Error`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Error(message = "Error")

        // When
        result.onError { executed = true }

        // Then
        assertTrue(executed)
    }

    @Test
    fun `onError should not execute action for Success`() {
        // Given
        var executed = false
        val result = Result.Success("data")

        // When
        result.onError { executed = true }

        // Then
        assertFalse(executed)
    }

    @Test
    fun `runCatching should create Success for successful operation`() {
        // When
        val result = Result.runCatching {
            "success"
        }

        // Then
        assertTrue(result is Result.Success)
        assertEquals("success", (result as Result.Success).data)
    }

    @Test
    fun `runCatching should create Error for failed operation`() {
        // When
        val result = Result.runCatching {
            throw RuntimeException("Test exception")
        }

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Test exception", (result as Result.Error).message)
    }

    @Test
    fun `Error with ErrorType should preserve error type`() {
        // Given
        val result = Result.Error(
            message = "Network error",
            errorType = Result.ErrorType.NETWORK,
            isRetryable = true
        )

        // Then
        assertEquals(Result.ErrorType.NETWORK, result.errorType)
        assertTrue(result.isRetryable)
    }

    @Test
    fun `Loading should not be Success or Error`() {
        // Given
        val result: Result<String> = Result.Loading

        // Then
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `chaining onSuccess and onError should work`() {
        // Given
        var successCalled = false
        var errorCalled = false
        val result = Result.Success("test")

        // When
        result
            .onSuccess { successCalled = true }
            .onError { errorCalled = true }

        // Then
        assertTrue(successCalled)
        assertFalse(errorCalled)
    }
}
