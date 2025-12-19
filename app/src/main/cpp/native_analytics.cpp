#include "native_analytics.h"
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <vector>
#include <unordered_map>
#include <mutex>
#include <malloc.h>  // For memalign on Android

#define LOG_TAG "NATIVE_ANALYTICS"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Internal Storage for Handles
// ============================================================================

static std::mutex g_mutex;
static int64_t g_next_handle = 1;
static std::unordered_map<int64_t, TimeWindowCalculator*> g_twc_map;
static std::unordered_map<int64_t, ChartBuffer*> g_chart_map;

struct PeakTracker {
    CircularBuffer buffer;
    int64_t window_ms;
    PeakData current_peak;
};
static std::unordered_map<int64_t, PeakTracker*> g_peak_map;

// ============================================================================
// Circular Buffer Implementation
// ============================================================================

int native_buffer_init(CircularBuffer* buffer, int32_t capacity) {
    if (!buffer || capacity <= 0 || capacity > MAX_BUFFER_SIZE) {
        return -1;
    }
    
    buffer->data = (DataPoint*)memalign(CACHE_LINE_SIZE, 
                                         capacity * sizeof(DataPoint));
    if (!buffer->data) {
        LOGE("Failed to allocate buffer memory");
        return -1;
    }
    
    buffer->capacity = capacity;
    buffer->head = 0;
    buffer->count = 0;
    buffer->oldest_timestamp = 0;
    buffer->newest_timestamp = 0;
    
    return 0;
}

void native_buffer_free(CircularBuffer* buffer) {
    if (buffer && buffer->data) {
        free(buffer->data);
        buffer->data = nullptr;
        buffer->capacity = 0;
        buffer->count = 0;
    }
}

void native_buffer_push(CircularBuffer* buffer, float value, int64_t timestamp) {
    if (!buffer || !buffer->data) return;
    
    int32_t index = (buffer->head + buffer->count) % buffer->capacity;
    
    if (buffer->count == buffer->capacity) {
        // Buffer full, overwrite oldest
        buffer->head = (buffer->head + 1) % buffer->capacity;
    } else {
        buffer->count++;
    }
    
    buffer->data[index].value = value;
    buffer->data[index].timestamp = timestamp;
    buffer->newest_timestamp = timestamp;
    
    // Update oldest timestamp
    if (buffer->count > 0) {
        buffer->oldest_timestamp = buffer->data[buffer->head].timestamp;
    }
}

void native_buffer_trim(CircularBuffer* buffer, int64_t cutoff_timestamp) {
    if (!buffer || !buffer->data || buffer->count == 0) return;
    
    while (buffer->count > 0) {
        if (buffer->data[buffer->head].timestamp >= cutoff_timestamp) {
            break;
        }
        buffer->head = (buffer->head + 1) % buffer->capacity;
        buffer->count--;
    }
    
    if (buffer->count > 0) {
        buffer->oldest_timestamp = buffer->data[buffer->head].timestamp;
    } else {
        buffer->oldest_timestamp = 0;
        buffer->newest_timestamp = 0;
    }
}

int32_t native_buffer_get_all(const CircularBuffer* buffer, DataPoint* out, int32_t max_count) {
    if (!buffer || !buffer->data || !out || max_count <= 0) return 0;
    
    int32_t count = std::min(buffer->count, max_count);
    
    for (int32_t i = 0; i < count; i++) {
        int32_t index = (buffer->head + i) % buffer->capacity;
        out[i] = buffer->data[index];
    }
    
    return count;
}

void native_buffer_clear(CircularBuffer* buffer) {
    if (!buffer) return;
    buffer->head = 0;
    buffer->count = 0;
    buffer->oldest_timestamp = 0;
    buffer->newest_timestamp = 0;
}

// ============================================================================
// Statistics Calculation (Optimized)
// ============================================================================

// Helper to iterate buffer within time window
template<typename Func>
static void iterate_window(const CircularBuffer* buffer, int64_t window_ms, 
                           int64_t now, Func&& func) {
    if (!buffer || !buffer->data || buffer->count == 0) return;
    
    int64_t cutoff = now - window_ms;
    
    for (int32_t i = 0; i < buffer->count; i++) {
        int32_t index = (buffer->head + i) % buffer->capacity;
        const DataPoint& point = buffer->data[index];
        
        if (point.timestamp >= cutoff) {
            func(point.value);
        }
    }
}

float native_calc_average(const CircularBuffer* buffer, int64_t window_ms, int64_t now) {
    if (!buffer || buffer->count == 0) return 0.0f;
    
    double sum = 0.0;
    int32_t count = 0;
    
    iterate_window(buffer, window_ms, now, [&](float value) {
        sum += value;
        count++;
    });
    
    return count > 0 ? static_cast<float>(sum / count) : 0.0f;
}

float native_calc_min(const CircularBuffer* buffer, int64_t window_ms, int64_t now) {
    if (!buffer || buffer->count == 0) return 0.0f;
    
    float min_val = INFINITY;
    bool found = false;
    
    iterate_window(buffer, window_ms, now, [&](float value) {
        if (value < min_val) {
            min_val = value;
            found = true;
        }
    });
    
    return found ? min_val : 0.0f;
}

float native_calc_max(const CircularBuffer* buffer, int64_t window_ms, int64_t now) {
    if (!buffer || buffer->count == 0) return 0.0f;
    
    float max_val = -INFINITY;
    bool found = false;
    
    iterate_window(buffer, window_ms, now, [&](float value) {
        if (value > max_val) {
            max_val = value;
            found = true;
        }
    });
    
    return found ? max_val : 0.0f;
}

// QuickSelect algorithm for O(n) average percentile calculation
static float quickselect(std::vector<float>& arr, int k) {
    if (arr.empty()) return 0.0f;
    if (k < 0) k = 0;
    if (k >= (int)arr.size()) k = arr.size() - 1;
    
    int left = 0, right = arr.size() - 1;
    
    while (left < right) {
        float pivot = arr[(left + right) / 2];
        int i = left, j = right;
        
        while (i <= j) {
            while (arr[i] < pivot) i++;
            while (arr[j] > pivot) j--;
            if (i <= j) {
                std::swap(arr[i], arr[j]);
                i++;
                j--;
            }
        }
        
        if (j < k) left = i;
        if (k < i) right = j;
    }
    
    return arr[k];
}

float native_calc_percentile(const CircularBuffer* buffer, int32_t percentile, 
                              int64_t window_ms, int64_t now) {
    if (!buffer || buffer->count == 0) return 0.0f;
    
    std::vector<float> values;
    values.reserve(buffer->count);
    
    iterate_window(buffer, window_ms, now, [&](float value) {
        values.push_back(value);
    });
    
    if (values.empty()) return 0.0f;
    
    int index = static_cast<int>(std::ceil(values.size() * percentile / 100.0)) - 1;
    return quickselect(values, std::max(0, index));
}

void native_calc_all_stats(const CircularBuffer* buffer, StatsResult* result, int64_t now) {
    if (!result) return;
    
    memset(result, 0, sizeof(StatsResult));
    result->timestamp = now;
    
    if (!buffer || buffer->count == 0) return;
    
    // Collect values for different windows in single pass
    std::vector<float> all_values;
    std::vector<float> values_30s, values_1m, values_5m;
    all_values.reserve(buffer->count);
    
    int64_t cutoff_30s = now - WINDOW_30S;
    int64_t cutoff_1m = now - WINDOW_1M;
    int64_t cutoff_5m = now - WINDOW_5M;
    
    double sum_30s = 0, sum_1m = 0, sum_5m = 0;
    int count_30s = 0, count_1m = 0, count_5m = 0;
    float min_val = INFINITY, max_val = -INFINITY;
    float current = 0;
    
    for (int32_t i = 0; i < buffer->count; i++) {
        int32_t index = (buffer->head + i) % buffer->capacity;
        const DataPoint& point = buffer->data[index];
        float value = point.value;
        int64_t ts = point.timestamp;
        
        all_values.push_back(value);
        current = value; // Last value is current
        
        if (value < min_val) min_val = value;
        if (value > max_val) max_val = value;
        
        if (ts >= cutoff_5m) {
            sum_5m += value;
            count_5m++;
            values_5m.push_back(value);
            
            if (ts >= cutoff_1m) {
                sum_1m += value;
                count_1m++;
                values_1m.push_back(value);
                
                if (ts >= cutoff_30s) {
                    sum_30s += value;
                    count_30s++;
                    values_30s.push_back(value);
                }
            }
        }
    }
    
    result->current = current;
    result->avg_30s = count_30s > 0 ? static_cast<float>(sum_30s / count_30s) : 0;
    result->avg_1m = count_1m > 0 ? static_cast<float>(sum_1m / count_1m) : 0;
    result->avg_5m = count_5m > 0 ? static_cast<float>(sum_5m / count_5m) : 0;
    result->min = (min_val != INFINITY) ? min_val : 0;
    result->max = (max_val != -INFINITY) ? max_val : 0;
    result->count = buffer->count;
    
    // Calculate percentiles from 1-minute window
    if (!values_1m.empty()) {
        int p50_idx = std::max(0, static_cast<int>(values_1m.size() * 0.50) - 1);
        int p95_idx = std::max(0, static_cast<int>(values_1m.size() * 0.95) - 1);
        int p99_idx = std::max(0, static_cast<int>(values_1m.size() * 0.99) - 1);
        
        std::vector<float> sorted = values_1m;
        std::nth_element(sorted.begin(), sorted.begin() + p50_idx, sorted.end());
        result->p50 = sorted[p50_idx];
        
        std::nth_element(sorted.begin(), sorted.begin() + p95_idx, sorted.end());
        result->p95 = sorted[p95_idx];
        
        std::nth_element(sorted.begin(), sorted.begin() + p99_idx, sorted.end());
        result->p99 = sorted[p99_idx];
    }
}

// ============================================================================
// Time Window Calculator Implementation
// ============================================================================

int64_t native_twc_create(int64_t max_duration_ms) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    TimeWindowCalculator* twc = new (std::nothrow) TimeWindowCalculator();
    if (!twc) return 0;
    
    int capacity = static_cast<int>(max_duration_ms / 500) + 10; // ~2 samples/sec
    capacity = std::min(capacity, MAX_BUFFER_SIZE);
    
    if (native_buffer_init(&twc->buffer, capacity) != 0) {
        delete twc;
        return 0;
    }
    
    twc->max_duration_ms = max_duration_ms;
    twc->cache_valid = false;
    twc->last_cache_update = 0;
    
    int64_t handle = g_next_handle++;
    g_twc_map[handle] = twc;
    
    LOGD("Created TimeWindowCalculator handle=%lld capacity=%d", (long long)handle, capacity);
    return handle;
}

void native_twc_destroy(int64_t handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_twc_map.find(handle);
    if (it != g_twc_map.end()) {
        native_buffer_free(&it->second->buffer);
        delete it->second;
        g_twc_map.erase(it);
        LOGD("Destroyed TimeWindowCalculator handle=%lld", (long long)handle);
    }
}

void native_twc_add_point(int64_t handle, float value, int64_t timestamp) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_twc_map.find(handle);
    if (it == g_twc_map.end()) return;
    
    TimeWindowCalculator* twc = it->second;
    
    // Trim old data
    int64_t cutoff = timestamp - twc->max_duration_ms;
    native_buffer_trim(&twc->buffer, cutoff);
    
    // Add new point
    native_buffer_push(&twc->buffer, value, timestamp);
    
    // Invalidate cache
    twc->cache_valid = false;
}

void native_twc_get_stats(int64_t handle, StatsResult* result) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    if (!result) return;
    memset(result, 0, sizeof(StatsResult));
    
    auto it = g_twc_map.find(handle);
    if (it == g_twc_map.end()) return;
    
    TimeWindowCalculator* twc = it->second;
    int64_t now = twc->buffer.newest_timestamp;
    
    native_calc_all_stats(&twc->buffer, result, now);
}

void native_twc_clear(int64_t handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_twc_map.find(handle);
    if (it != g_twc_map.end()) {
        native_buffer_clear(&it->second->buffer);
        it->second->cache_valid = false;
    }
}

// ============================================================================
// Chart Buffer Implementation
// ============================================================================

int64_t native_chart_create(int32_t capacity) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    ChartBuffer* chart = new (std::nothrow) ChartBuffer();
    if (!chart) return 0;
    
    capacity = std::min(capacity, MAX_BUFFER_SIZE);
    
    if (native_buffer_init(&chart->buffer, capacity) != 0) {
        delete chart;
        return 0;
    }
    
    chart->normalized_values = (float*)calloc(capacity, sizeof(float));
    if (!chart->normalized_values) {
        native_buffer_free(&chart->buffer);
        delete chart;
        return 0;
    }
    
    chart->min_value = 0;
    chart->max_value = 100;
    chart->normalized_count = 0;
    
    int64_t handle = g_next_handle++;
    g_chart_map[handle] = chart;
    
    LOGD("Created ChartBuffer handle=%lld capacity=%d", (long long)handle, capacity);
    return handle;
}

void native_chart_destroy(int64_t handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_chart_map.find(handle);
    if (it != g_chart_map.end()) {
        native_buffer_free(&it->second->buffer);
        free(it->second->normalized_values);
        delete it->second;
        g_chart_map.erase(it);
        LOGD("Destroyed ChartBuffer handle=%lld", (long long)handle);
    }
}

void native_chart_add_point(int64_t handle, float value, int64_t timestamp) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_chart_map.find(handle);
    if (it == g_chart_map.end()) return;
    
    ChartBuffer* chart = it->second;
    native_buffer_push(&chart->buffer, value, timestamp);
    
    // Update min/max
    if (chart->buffer.count == 1) {
        chart->min_value = value;
        chart->max_value = value;
    } else {
        if (value < chart->min_value) chart->min_value = value;
        if (value > chart->max_value) chart->max_value = value;
    }
    
    // Update normalized values
    float range = chart->max_value - chart->min_value;
    if (range < 0.001f) range = 1.0f; // Avoid division by zero
    
    chart->normalized_count = chart->buffer.count;
    for (int32_t i = 0; i < chart->buffer.count; i++) {
        int32_t index = (chart->buffer.head + i) % chart->buffer.capacity;
        float v = chart->buffer.data[index].value;
        chart->normalized_values[i] = (v - chart->min_value) / range;
    }
}

int32_t native_chart_get_normalized(int64_t handle, float* out, int32_t max_count) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    if (!out || max_count <= 0) return 0;
    
    auto it = g_chart_map.find(handle);
    if (it == g_chart_map.end()) return 0;
    
    ChartBuffer* chart = it->second;
    int32_t count = std::min(chart->normalized_count, max_count);
    
    memcpy(out, chart->normalized_values, count * sizeof(float));
    return count;
}

void native_chart_get_range(int64_t handle, float* min_out, float* max_out) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_chart_map.find(handle);
    if (it == g_chart_map.end()) {
        if (min_out) *min_out = 0;
        if (max_out) *max_out = 100;
        return;
    }
    
    if (min_out) *min_out = it->second->min_value;
    if (max_out) *max_out = it->second->max_value;
}

void native_chart_clear(int64_t handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_chart_map.find(handle);
    if (it != g_chart_map.end()) {
        native_buffer_clear(&it->second->buffer);
        it->second->min_value = 0;
        it->second->max_value = 100;
        it->second->normalized_count = 0;
    }
}

// ============================================================================
// Peak Tracker Implementation
// ============================================================================

int64_t native_peak_create(int64_t window_ms) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    PeakTracker* tracker = new (std::nothrow) PeakTracker();
    if (!tracker) return 0;
    
    int capacity = static_cast<int>(window_ms / 500) + 10;
    capacity = std::min(capacity, MAX_BUFFER_SIZE);
    
    if (native_buffer_init(&tracker->buffer, capacity) != 0) {
        delete tracker;
        return 0;
    }
    
    tracker->window_ms = window_ms;
    memset(&tracker->current_peak, 0, sizeof(PeakData));
    
    int64_t handle = g_next_handle++;
    g_peak_map[handle] = tracker;
    
    return handle;
}

void native_peak_destroy(int64_t handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_peak_map.find(handle);
    if (it != g_peak_map.end()) {
        native_buffer_free(&it->second->buffer);
        delete it->second;
        g_peak_map.erase(it);
    }
}

void native_peak_add_value(int64_t handle, float value, int64_t timestamp) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_peak_map.find(handle);
    if (it == g_peak_map.end()) return;
    
    PeakTracker* tracker = it->second;
    
    // Trim old data
    int64_t cutoff = timestamp - tracker->window_ms;
    native_buffer_trim(&tracker->buffer, cutoff);
    
    // Add new point
    native_buffer_push(&tracker->buffer, value, timestamp);
    
    // Update peak data
    float max_val = -INFINITY;
    int64_t max_ts = 0;
    double sum = 0;
    int count = 0;
    
    for (int32_t i = 0; i < tracker->buffer.count; i++) {
        int32_t index = (tracker->buffer.head + i) % tracker->buffer.capacity;
        float v = tracker->buffer.data[index].value;
        int64_t ts = tracker->buffer.data[index].timestamp;
        
        sum += v;
        count++;
        
        if (v > max_val) {
            max_val = v;
            max_ts = ts;
        }
    }
    
    tracker->current_peak.peak_value = (max_val != -INFINITY) ? max_val : 0;
    tracker->current_peak.peak_timestamp = max_ts;
    tracker->current_peak.avg_value = count > 0 ? static_cast<float>(sum / count) : 0;
    tracker->current_peak.sample_count = count;
}

void native_peak_get_data(int64_t handle, PeakData* result) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    if (!result) return;
    memset(result, 0, sizeof(PeakData));
    
    auto it = g_peak_map.find(handle);
    if (it != g_peak_map.end()) {
        *result = it->second->current_peak;
    }
}

void native_peak_reset(int64_t handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    auto it = g_peak_map.find(handle);
    if (it != g_peak_map.end()) {
        native_buffer_clear(&it->second->buffer);
        memset(&it->second->current_peak, 0, sizeof(PeakData));
    }
}

// ============================================================================
// Utility Functions
// ============================================================================

int native_format_speed(char* buffer, int buffer_size, int64_t bytes_per_sec) {
    if (!buffer || buffer_size <= 0) return 0;
    
    if (bytes_per_sec < 1024) {
        return snprintf(buffer, buffer_size, "%lldB/s", (long long)bytes_per_sec);
    } else if (bytes_per_sec < 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%.1fKB/s", bytes_per_sec / 1024.0);
    } else if (bytes_per_sec < 1024 * 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%.1fMB/s", bytes_per_sec / (1024.0 * 1024.0));
    } else {
        return snprintf(buffer, buffer_size, "%.2fGB/s", bytes_per_sec / (1024.0 * 1024.0 * 1024.0));
    }
}

int native_format_percent(char* buffer, int buffer_size, float percent) {
    if (!buffer || buffer_size <= 0) return 0;
    return snprintf(buffer, buffer_size, "%.1f%%", percent);
}

int native_format_memory(char* buffer, int buffer_size, int64_t bytes) {
    if (!buffer || buffer_size <= 0) return 0;
    
    if (bytes < 1024) {
        return snprintf(buffer, buffer_size, "%lldB", (long long)bytes);
    } else if (bytes < 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%.1fKB", bytes / 1024.0);
    } else if (bytes < 1024 * 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%.1fMB", bytes / (1024.0 * 1024.0));
    } else {
        return snprintf(buffer, buffer_size, "%.2fGB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

// ============================================================================
// JNI Functions
// ============================================================================

extern "C" {

// Time Window Calculator JNI
JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_createTimeWindowCalculator(
        JNIEnv* env, jclass clazz, jlong maxDurationMs) {
    return native_twc_create(maxDurationMs);
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_destroyTimeWindowCalculator(
        JNIEnv* env, jclass clazz, jlong handle) {
    native_twc_destroy(handle);
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_twcAddPoint(
        JNIEnv* env, jclass clazz, jlong handle, jfloat value, jlong timestamp) {
    native_twc_add_point(handle, value, timestamp);
}

JNIEXPORT jfloatArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_twcGetStats(
        JNIEnv* env, jclass clazz, jlong handle) {
    StatsResult result;
    native_twc_get_stats(handle, &result);
    
    // Return as float array: [current, avg30s, avg1m, avg5m, min, max, p50, p95, p99]
    jfloatArray arr = env->NewFloatArray(9);
    if (arr == nullptr) return nullptr;
    
    jfloat data[9] = {
        result.current, result.avg_30s, result.avg_1m, result.avg_5m,
        result.min, result.max, result.p50, result.p95, result.p99
    };
    env->SetFloatArrayRegion(arr, 0, 9, data);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_twcClear(
        JNIEnv* env, jclass clazz, jlong handle) {
    native_twc_clear(handle);
}

// Chart Buffer JNI
JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_createChartBuffer(
        JNIEnv* env, jclass clazz, jint capacity) {
    return native_chart_create(capacity);
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_destroyChartBuffer(
        JNIEnv* env, jclass clazz, jlong handle) {
    native_chart_destroy(handle);
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_chartAddPoint(
        JNIEnv* env, jclass clazz, jlong handle, jfloat value, jlong timestamp) {
    native_chart_add_point(handle, value, timestamp);
}

JNIEXPORT jfloatArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_chartGetNormalized(
        JNIEnv* env, jclass clazz, jlong handle, jint maxCount) {
    std::vector<float> values(maxCount);
    int32_t count = native_chart_get_normalized(handle, values.data(), maxCount);
    
    if (count == 0) return nullptr;
    
    jfloatArray arr = env->NewFloatArray(count);
    if (arr == nullptr) return nullptr;
    
    env->SetFloatArrayRegion(arr, 0, count, values.data());
    return arr;
}

JNIEXPORT jfloatArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_chartGetRange(
        JNIEnv* env, jclass clazz, jlong handle) {
    float min_val, max_val;
    native_chart_get_range(handle, &min_val, &max_val);
    
    jfloatArray arr = env->NewFloatArray(2);
    if (arr == nullptr) return nullptr;
    
    jfloat data[2] = { min_val, max_val };
    env->SetFloatArrayRegion(arr, 0, 2, data);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_chartClear(
        JNIEnv* env, jclass clazz, jlong handle) {
    native_chart_clear(handle);
}

// Peak Tracker JNI
JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_createPeakTracker(
        JNIEnv* env, jclass clazz, jlong windowMs) {
    return native_peak_create(windowMs);
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_destroyPeakTracker(
        JNIEnv* env, jclass clazz, jlong handle) {
    native_peak_destroy(handle);
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_peakAddValue(
        JNIEnv* env, jclass clazz, jlong handle, jfloat value, jlong timestamp) {
    native_peak_add_value(handle, value, timestamp);
}

JNIEXPORT jfloatArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_peakGetData(
        JNIEnv* env, jclass clazz, jlong handle) {
    PeakData data;
    native_peak_get_data(handle, &data);
    
    // Return as float array: [peak_value, peak_timestamp, avg_value, sample_count]
    jfloatArray arr = env->NewFloatArray(4);
    if (arr == nullptr) return nullptr;
    
    jfloat values[4] = {
        data.peak_value,
        static_cast<jfloat>(data.peak_timestamp),
        data.avg_value,
        static_cast<jfloat>(data.sample_count)
    };
    env->SetFloatArrayRegion(arr, 0, 4, values);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeAnalytics_peakReset(
        JNIEnv* env, jclass clazz, jlong handle) {
    native_peak_reset(handle);
}

} // extern "C"
