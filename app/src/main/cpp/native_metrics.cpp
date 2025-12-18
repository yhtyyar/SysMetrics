#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include "native_metrics.h"

#define LOG_TAG "SysMetricsNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Static storage for previous CPU stats
static CpuStats prev_stats = {0};
static bool has_prev_stats = false;

/**
 * Reads CPU statistics from /proc/stat.
 * Optimized for minimal allocations and fast parsing.
 */
int read_cpu_stats(CpuStats* stats) {
    FILE* fp = fopen("/proc/stat", "r");
    if (!fp) {
        LOGE("Failed to open /proc/stat");
        return -1;
    }

    // Read first line (aggregate CPU stats)
    // Format: cpu user nice system idle iowait irq softirq steal guest guest_nice
    int result = fscanf(fp, "cpu %ld %ld %ld %ld %ld %ld %ld %ld",
                        &stats->user,
                        &stats->nice,
                        &stats->system,
                        &stats->idle,
                        &stats->iowait,
                        &stats->irq,
                        &stats->softirq,
                        &stats->steal);

    fclose(fp);

    if (result < 4) {
        LOGE("Failed to parse /proc/stat, got %d values", result);
        return -1;
    }

    // Set optional fields to 0 if not read
    if (result < 5) stats->iowait = 0;
    if (result < 6) stats->irq = 0;
    if (result < 7) stats->softirq = 0;
    if (result < 8) stats->steal = 0;

    return 0;
}

/**
 * Calculates CPU usage percentage between two snapshots.
 * Uses integer arithmetic where possible for performance.
 */
float calculate_cpu_usage(const CpuStats* prev, const CpuStats* curr) {
    // Calculate totals
    long prev_total = prev->user + prev->nice + prev->system + prev->idle +
                      prev->iowait + prev->irq + prev->softirq + prev->steal;
    long curr_total = curr->user + curr->nice + curr->system + curr->idle +
                      curr->iowait + curr->irq + curr->softirq + curr->steal;

    // Calculate active (non-idle) time
    long prev_active = prev->user + prev->nice + prev->system +
                       prev->irq + prev->softirq + prev->steal;
    long curr_active = curr->user + curr->nice + curr->system +
                       curr->irq + curr->softirq + curr->steal;

    long total_diff = curr_total - prev_total;
    long active_diff = curr_active - prev_active;

    if (total_diff <= 0) {
        return 0.0f;
    }

    float usage = (float)active_diff / (float)total_diff * 100.0f;

    // Clamp to valid range
    if (usage < 0.0f) usage = 0.0f;
    if (usage > 100.0f) usage = 100.0f;

    return usage;
}

/**
 * Reads memory statistics from /proc/meminfo.
 * Uses optimized line-by-line parsing.
 */
int read_memory_stats(MemoryStats* stats) {
    FILE* fp = fopen("/proc/meminfo", "r");
    if (!fp) {
        LOGE("Failed to open /proc/meminfo");
        return -1;
    }

    char line[256];
    int found = 0;
    const int needed = 5;

    memset(stats, 0, sizeof(MemoryStats));

    while (fgets(line, sizeof(line), fp) && found < needed) {
        char key[64];
        long value;

        if (sscanf(line, "%63[^:]: %ld", key, &value) == 2) {
            if (strcmp(key, "MemTotal") == 0) {
                stats->total_kb = value;
                found++;
            } else if (strcmp(key, "MemFree") == 0) {
                stats->free_kb = value;
                found++;
            } else if (strcmp(key, "MemAvailable") == 0) {
                stats->available_kb = value;
                found++;
            } else if (strcmp(key, "Buffers") == 0) {
                stats->buffers_kb = value;
                found++;
            } else if (strcmp(key, "Cached") == 0) {
                stats->cached_kb = value;
                found++;
            }
        }
    }

    fclose(fp);
    return (found >= 2) ? 0 : -1; // At least MemTotal and MemFree required
}

/**
 * Reads CPU stats for specific PID from /proc/pid/stat.
 * Highly optimized for frequent calls.
 */
int read_process_cpu_stats(int pid, ProcessCpuStats* stats) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/stat", pid);

    FILE* fp = fopen(path, "r");
    if (!fp) {
        return -1;
    }

    // Read only the fields we need: utime (14) and stime (15)
    // Skip fields 1-13, then read utime and stime
    char comm[256]; // comm field can contain spaces and parentheses
    int pid_check;
    char state;
    int ppid, pgrp, session, tty_nr, tpgid;
    unsigned flags;
    unsigned long minflt, cminflt, majflt, cmajflt;
    unsigned long utime, stime;

    int result = fscanf(fp, "%d %255s %c %d %d %d %d %d %u %lu %lu %lu %lu %lu %lu",
                       &pid_check, comm, &state, &ppid, &pgrp, &session, &tty_nr, &tpgid,
                       &flags, &minflt, &cminflt, &majflt, &cmajflt, &utime, &stime);

    fclose(fp);

    if (result < 15) {
        LOGE("Failed to parse /proc/%d/stat, got %d values", pid, result);
        return -1;
    }

    stats->utime = (long)utime;
    stats->stime = (long)stime;
    stats->total_time = stats->utime + stats->stime;

    return 0;
}

/**
 * Optimized time string formatting.
 */
int format_time_string(char* buffer, int buffer_size, int hour, int minute, bool use_24h) {
    if (use_24h) {
        return snprintf(buffer, buffer_size, "%02d:%02d", hour, minute);
    } else {
        const char* am_pm = (hour >= 12) ? "PM" : "AM";
        int display_hour = hour % 12;
        if (display_hour == 0) display_hour = 12;
        return snprintf(buffer, buffer_size, "%d:%02d %s", display_hour, minute, am_pm);
    }
}

/**
 * Format CPU usage string.
 */
int format_cpu_string(char* buffer, int buffer_size, float cpu_percent) {
    if (cpu_percent >= 10.0f) {
        return snprintf(buffer, buffer_size, "CPU: %.0f%%", cpu_percent);
    } else if (cpu_percent >= 1.0f) {
        return snprintf(buffer, buffer_size, "CPU: %.1f%%", cpu_percent);
    } else if (cpu_percent >= 0.1f) {
        return snprintf(buffer, buffer_size, "CPU: %.2f%%", cpu_percent);
    } else {
        return snprintf(buffer, buffer_size, "CPU: %.1f%%", cpu_percent);
    }
}

/**
 * Format RAM usage string.
 */
int format_ram_string(char* buffer, int buffer_size, long used_mb, long total_mb) {
    return snprintf(buffer, buffer_size, "RAM: %ld/%ld MB", used_mb, total_mb);
}

/**
 * Format self stats string.
 */
int format_self_stats_string(char* buffer, int buffer_size, float cpu_percent, long ram_mb) {
    return snprintf(buffer, buffer_size, "Self: %.1f%% / %ldM", cpu_percent, ram_mb);
}

/**
 * Read temperature from thermal zone.
 * Returns temperature in Celsius, or -1 if unavailable.
 */
float read_temperature(int zone) {
    char path[64];
    snprintf(path, sizeof(path), "/sys/class/thermal/thermal_zone%d/temp", zone);

    FILE* fp = fopen(path, "r");
    if (!fp) {
        return -1.0f;
    }

    int temp_millidegrees;
    if (fscanf(fp, "%d", &temp_millidegrees) != 1) {
        fclose(fp);
        return -1.0f;
    }

    fclose(fp);
    return (float)temp_millidegrees / 1000.0f;
}

// ============================================================================
// JNI Functions
// ============================================================================

extern "C" {

/**
 * Get current CPU usage percentage.
 * Automatically manages previous stats for delta calculation.
 */
JNIEXPORT jfloat JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_getCpuUsage(JNIEnv* env, jobject thiz) {
    CpuStats curr_stats;

    if (read_cpu_stats(&curr_stats) != 0) {
        return -1.0f;
    }

    float usage = 0.0f;

    if (has_prev_stats) {
        usage = calculate_cpu_usage(&prev_stats, &curr_stats);
    }

    // Store current as previous for next call
    memcpy(&prev_stats, &curr_stats, sizeof(CpuStats));
    has_prev_stats = true;

    return usage;
}

/**
 * Reset CPU stats baseline.
 * Call this when starting a new monitoring session.
 */
JNIEXPORT void JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_resetCpuBaseline(JNIEnv* env, jobject thiz) {
    has_prev_stats = false;
    memset(&prev_stats, 0, sizeof(CpuStats));
    LOGI("CPU baseline reset");
}

/**
 * Get memory statistics.
 * Returns array: [totalMb, usedMb, availableMb, usagePercent]
 */
JNIEXPORT jfloatArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_getMemoryStats(JNIEnv* env, jobject thiz) {
    MemoryStats stats;

    jfloatArray result = env->NewFloatArray(4);
    if (!result) {
        return nullptr;
    }

    float values[4] = {0.0f, 0.0f, 0.0f, 0.0f};

    if (read_memory_stats(&stats) == 0) {
        float total_mb = (float)stats.total_kb / 1024.0f;
        float available_mb = (float)stats.available_kb / 1024.0f;
        float used_mb = total_mb - available_mb;
        float usage_percent = (total_mb > 0) ? (used_mb / total_mb * 100.0f) : 0.0f;

        values[0] = total_mb;
        values[1] = used_mb;
        values[2] = available_mb;
        values[3] = usage_percent;
    }

    env->SetFloatArrayRegion(result, 0, 4, values);
    return result;
}

/**
 * Get CPU temperature from thermal zone 0.
 * Returns temperature in Celsius, or -1 if unavailable.
 */
JNIEXPORT jfloat JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_getTemperature(JNIEnv* env, jobject thiz) {
    // Try multiple thermal zones, return first valid one
    for (int i = 0; i < 10; i++) {
        float temp = read_temperature(i);
        if (temp > 0.0f && temp < 150.0f) { // Sanity check
            return temp;
        }
    }
    return -1.0f;
}

/**
 * Check if native library is loaded correctly.
 */
JNIEXPORT jboolean JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_isAvailable(JNIEnv* env, jobject thiz) {
    return JNI_TRUE;
}

/**
 * Get CPU stats for specific PID.
 * Returns array: [utime, stime, total_time] or null if failed.
 */
JNIEXPORT jlongArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_getProcessCpuStats(JNIEnv* env, jobject thiz, jint pid) {
    ProcessCpuStats stats;

    if (read_process_cpu_stats(pid, &stats) != 0) {
        return nullptr;
    }

    jlongArray result = env->NewLongArray(3);
    if (!result) {
        return nullptr;
    }

    jlong values[3] = {stats.utime, stats.stime, stats.total_time};
    env->SetLongArrayRegion(result, 0, 3, values);
    return result;
}

/**
 * Format time string natively.
 */
JNIEXPORT jstring JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_formatTimeString(JNIEnv* env, jobject thiz,
                                                                     jint hour, jint minute, jboolean use24h) {
    char buffer[16];
    int len = format_time_string(buffer, sizeof(buffer), hour, minute, use24h);
    return env->NewStringUTF(buffer);
}

/**
 * Format CPU string natively.
 */
JNIEXPORT jstring JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_formatCpuString(JNIEnv* env, jobject thiz, jfloat cpuPercent) {
    char buffer[32];
    format_cpu_string(buffer, sizeof(buffer), cpuPercent);
    return env->NewStringUTF(buffer);
}

/**
 * Format RAM string natively.
 */
JNIEXPORT jstring JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_formatRamString(JNIEnv* env, jobject thiz,
                                                                    jlong usedMb, jlong totalMb) {
    char buffer[32];
    format_ram_string(buffer, sizeof(buffer), usedMb, totalMb);
    return env->NewStringUTF(buffer);
}

/**
 * Format self stats string natively.
 */
JNIEXPORT jstring JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeMetrics_formatSelfStatsString(JNIEnv* env, jobject thiz,
                                                                          jfloat cpuPercent, jlong ramMb) {
    char buffer[32];
    format_self_stats_string(buffer, sizeof(buffer), cpuPercent, ramMb);
    return env->NewStringUTF(buffer);
}

}
