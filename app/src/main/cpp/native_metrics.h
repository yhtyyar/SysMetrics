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
 * Reads temperature from thermal zone.
 * Returns temperature in Celsius, or -1 on failure.
 */
float read_temperature(int zone_index);

#ifdef __cplusplus
}
#endif

#endif // SYSMETRICS_NATIVE_METRICS_H
