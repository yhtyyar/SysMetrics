#ifndef SYSMETRICS_NATIVE_ANALYTICS_H
#define SYSMETRICS_NATIVE_ANALYTICS_H

#include <jni.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * ============================================================================
 * NATIVE ANALYTICS ENGINE - High-Performance C++ Implementation
 * ============================================================================
 * 
 * Optimizations:
 * - Lock-free circular buffers for O(1) operations
 * - SIMD-friendly data alignment
 * - Cache-optimized memory layout
 * - Minimal allocations during runtime
 * 
 * Performance Targets:
 * - Average calculation: <1μs
 * - Percentile calculation: <10μs
 * - Buffer operations: O(1)
 * - Memory overhead: <1KB per metric
 */

// ============================================================================
// Constants
// ============================================================================

#define MAX_BUFFER_SIZE 512
#define MAX_WINDOWS 4
#define CACHE_LINE_SIZE 64

// Time windows in milliseconds
#define WINDOW_30S  30000L
#define WINDOW_1M   60000L
#define WINDOW_5M   300000L
#define WINDOW_10M  600000L

// ============================================================================
// Data Structures (Cache-aligned for performance)
// ============================================================================

/**
 * Single data point with timestamp.
 * Packed for memory efficiency.
 */
typedef struct __attribute__((packed)) {
    float value;
    int64_t timestamp;
} DataPoint;

/**
 * Statistics result structure.
 */
typedef struct {
    float current;
    float avg_30s;
    float avg_1m;
    float avg_5m;
    float min;
    float max;
    float p50;
    float p95;
    float p99;
    int64_t timestamp;
    int32_t count;
} StatsResult;

/**
 * Peak tracking structure.
 */
typedef struct {
    float peak_value;
    int64_t peak_timestamp;
    float avg_value;
    int32_t sample_count;
} PeakData;

/**
 * Circular buffer for time-series data.
 * Lock-free, cache-optimized implementation.
 */
typedef struct __attribute__((aligned(CACHE_LINE_SIZE))) {
    DataPoint* data;
    int32_t capacity;
    int32_t head;
    int32_t count;
    int64_t oldest_timestamp;
    int64_t newest_timestamp;
} CircularBuffer;

/**
 * Time window calculator instance.
 */
typedef struct {
    CircularBuffer buffer;
    int64_t max_duration_ms;
    float cached_avg_30s;
    float cached_avg_1m;
    float cached_avg_5m;
    int64_t last_cache_update;
    bool cache_valid;
} TimeWindowCalculator;

/**
 * Chart data buffer with pre-computed render data.
 */
typedef struct {
    CircularBuffer buffer;
    float min_value;
    float max_value;
    float* normalized_values;  // Pre-computed 0-1 normalized values
    int32_t normalized_count;
} ChartBuffer;

// ============================================================================
// Circular Buffer API
// ============================================================================

/**
 * Initialize circular buffer with given capacity.
 * @param buffer Pointer to buffer structure
 * @param capacity Maximum number of elements
 * @return 0 on success, -1 on failure
 */
int native_buffer_init(CircularBuffer* buffer, int32_t capacity);

/**
 * Free buffer resources.
 */
void native_buffer_free(CircularBuffer* buffer);

/**
 * Add data point to buffer (O(1) operation).
 */
void native_buffer_push(CircularBuffer* buffer, float value, int64_t timestamp);

/**
 * Remove old data points outside time window.
 */
void native_buffer_trim(CircularBuffer* buffer, int64_t cutoff_timestamp);

/**
 * Get all data points as array.
 * @return Number of points copied
 */
int32_t native_buffer_get_all(const CircularBuffer* buffer, DataPoint* out, int32_t max_count);

/**
 * Clear all data from buffer.
 */
void native_buffer_clear(CircularBuffer* buffer);

// ============================================================================
// Statistics Calculation API (SIMD-optimized)
// ============================================================================

/**
 * Calculate average of values in buffer within time window.
 * Uses SIMD instructions when available.
 */
float native_calc_average(const CircularBuffer* buffer, int64_t window_ms, int64_t now);

/**
 * Calculate min value in buffer within time window.
 */
float native_calc_min(const CircularBuffer* buffer, int64_t window_ms, int64_t now);

/**
 * Calculate max value in buffer within time window.
 */
float native_calc_max(const CircularBuffer* buffer, int64_t window_ms, int64_t now);

/**
 * Calculate percentile using QuickSelect algorithm (O(n) average).
 * @param percentile Value 0-100
 */
float native_calc_percentile(const CircularBuffer* buffer, int32_t percentile, 
                              int64_t window_ms, int64_t now);

/**
 * Calculate all statistics in single pass (optimized).
 */
void native_calc_all_stats(const CircularBuffer* buffer, StatsResult* result, int64_t now);

// ============================================================================
// Time Window Calculator API
// ============================================================================

/**
 * Create time window calculator.
 * @param max_duration_ms Maximum data retention (default 5 minutes)
 * @return Handle to calculator, or 0 on failure
 */
int64_t native_twc_create(int64_t max_duration_ms);

/**
 * Destroy time window calculator.
 */
void native_twc_destroy(int64_t handle);

/**
 * Add data point to calculator.
 */
void native_twc_add_point(int64_t handle, float value, int64_t timestamp);

/**
 * Get statistics for all time windows.
 */
void native_twc_get_stats(int64_t handle, StatsResult* result);

/**
 * Clear all data.
 */
void native_twc_clear(int64_t handle);

// ============================================================================
// Chart Buffer API
// ============================================================================

/**
 * Create chart buffer.
 * @param capacity Number of visible points
 * @return Handle to buffer, or 0 on failure
 */
int64_t native_chart_create(int32_t capacity);

/**
 * Destroy chart buffer.
 */
void native_chart_destroy(int64_t handle);

/**
 * Add point to chart buffer.
 */
void native_chart_add_point(int64_t handle, float value, int64_t timestamp);

/**
 * Get normalized values (0-1 range) for rendering.
 * @param out Output array for normalized values
 * @param max_count Maximum values to return
 * @return Number of values returned
 */
int32_t native_chart_get_normalized(int64_t handle, float* out, int32_t max_count);

/**
 * Get min/max values in buffer.
 */
void native_chart_get_range(int64_t handle, float* min_out, float* max_out);

/**
 * Clear chart buffer.
 */
void native_chart_clear(int64_t handle);

// ============================================================================
// Peak Tracking API
// ============================================================================

/**
 * Create peak tracker.
 * @param window_ms Time window for tracking
 * @return Handle to tracker, or 0 on failure
 */
int64_t native_peak_create(int64_t window_ms);

/**
 * Destroy peak tracker.
 */
void native_peak_destroy(int64_t handle);

/**
 * Add value to peak tracker.
 */
void native_peak_add_value(int64_t handle, float value, int64_t timestamp);

/**
 * Get current peak data.
 */
void native_peak_get_data(int64_t handle, PeakData* result);

/**
 * Reset peak tracker.
 */
void native_peak_reset(int64_t handle);

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Format speed string (bytes/sec to human readable).
 */
int native_format_speed(char* buffer, int buffer_size, int64_t bytes_per_sec);

/**
 * Format percentage string.
 */
int native_format_percent(char* buffer, int buffer_size, float percent);

/**
 * Format memory size string.
 */
int native_format_memory(char* buffer, int buffer_size, int64_t bytes);

#ifdef __cplusplus
}
#endif

#endif // SYSMETRICS_NATIVE_ANALYTICS_H
