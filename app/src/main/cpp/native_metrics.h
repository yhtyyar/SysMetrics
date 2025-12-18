#ifndef SYSMETRICS_NATIVE_METRICS_H
#define SYSMETRICS_NATIVE_METRICS_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * CPU statistics structure matching /proc/stat format.
 */
typedef struct {
    long user;
    long nice;
    long system;
    long idle;
    long iowait;
    long irq;
    long softirq;
    long steal;
} CpuStats;

/**
 * Memory statistics from /proc/meminfo.
 */
typedef struct {
    long total_kb;
    long free_kb;
    long available_kb;
    long buffers_kb;
    long cached_kb;
} MemoryStats;

/**
 * Reads CPU statistics from /proc/stat.
 * Returns 0 on success, -1 on failure.
 */
int read_cpu_stats(CpuStats* stats);

/**
 * Calculates CPU usage percentage between two snapshots.
 * Returns usage as percentage (0-100).
 */
float calculate_cpu_usage(const CpuStats* prev, const CpuStats* curr);

/**
 * Reads memory statistics from /proc/meminfo.
 * Returns 0 on success, -1 on failure.
 */
int read_memory_stats(MemoryStats* stats);

/**
 * Process statistics for CPU calculation.
 */
typedef struct {
    long utime;    // user time
    long stime;    // system time
    long total_time; // utime + stime
} ProcessCpuStats;

/**
 * Reads CPU stats for specific PID from /proc/pid/stat.
 * Returns 0 on success, -1 on failure.
 */
int read_process_cpu_stats(int pid, ProcessCpuStats* stats);

/**
 * Optimized string formatting for UI display.
 * Returns formatted string length.
 */
int format_time_string(char* buffer, int buffer_size, int hour, int minute, bool use_24h);

/**
 * Format CPU usage string.
 */
int format_cpu_string(char* buffer, int buffer_size, float cpu_percent);

/**
 * Format RAM usage string.
 */
int format_ram_string(char* buffer, int buffer_size, long used_mb, long total_mb);

/**
 * Format self stats string.
 */
int format_self_stats_string(char* buffer, int buffer_size, float cpu_percent, long ram_mb);

#ifdef __cplusplus
}
#endif

#endif // SYSMETRICS_NATIVE_METRICS_H
