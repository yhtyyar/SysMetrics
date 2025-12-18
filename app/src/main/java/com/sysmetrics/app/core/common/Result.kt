package com.sysmetrics.app.core.common

/**
 * A generic wrapper class for handling success and error states.
 * Provides a type-safe way to handle operation results.
 * 
 * Usage:
 * ```
 * val result = Result.runCatching { metricsCollector.collect() }
 * result.onSuccess { metrics -> 
 *     updateUI(metrics) 
 * }.onError { error ->
 *     if (error.isRetryable) retry()
 *     showError(error.message)
 * }
 * ```
 */
sealed class Result<out T> {
    
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with error details.
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String = exception?.message ?: "Unknown error",
        val isRetryable: Boolean = false,
        val errorType: ErrorType = ErrorType.UNKNOWN
    ) : Result<Nothing>()
    
    /**
     * Represents a loading state.
     */
    object Loading : Result<Nothing>()
    
    /**
     * Classification of error types for better handling.
     */
    enum class ErrorType {
        NETWORK,        // Network connectivity issues
        IO,             // File system errors
        PERMISSION,     // Permission denied
        TIMEOUT,        // Operation timeout
        PARSE,          // Data parsing errors
        UNKNOWN         // Unclassified errors
    }
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    /**
     * Returns the data if success, or null if error.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error, Loading -> null
    }
    
    /**
     * Returns the data if success, or the default value if error.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error, Loading -> default
    }
    
    /**
     * Transforms the success data using the provided mapper.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        Loading -> Loading
    }
    
    /**
     * Executes the action if this is a success.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Executes the action if this is an error.
     */
    inline fun onError(action: (Error) -> Unit): Result<T> {
        if (this is Error) action(this)
        return this
    }
    
    companion object {
        /**
         * Creates a Success result with the given data.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Creates an Error result with the given exception.
         */
        fun error(exception: Throwable): Result<Nothing> = Error(exception)
        
        /**
         * Creates an Error result with the given message.
         */
        fun error(message: String): Result<Nothing> = Error(message = message)
        
        /**
         * Wraps a suspending block in a Result, catching any exceptions.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e)
        }
    }
}
