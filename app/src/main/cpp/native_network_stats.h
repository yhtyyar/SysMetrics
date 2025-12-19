#ifndef SYSMETRICS_NATIVE_NETWORK_STATS_H
#define SYSMETRICS_NATIVE_NETWORK_STATS_H

#include <jni.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Network interface statistics structure.
 * Matches /proc/net/dev format fields.
 */
typedef struct {
    char interface_name[32];
    uint64_t rx_bytes;
    uint64_t rx_packets;
    uint64_t rx_errors;
    uint64_t rx_dropped;
    uint64_t tx_bytes;
    uint64_t tx_packets;
    uint64_t tx_errors;
    uint64_t tx_dropped;
    int64_t timestamp_ms;
} InterfaceStatsNative;

/**
 * Aggregated network statistics.
 */
typedef struct {
    uint64_t total_rx_bytes;
    uint64_t total_tx_bytes;
    uint64_t rx_bytes_per_sec;
    uint64_t tx_bytes_per_sec;
    float rx_mbps;
    float tx_mbps;
    int64_t timestamp_ms;
    int interface_count;
    int is_valid;
} NetworkStatsNative;

/**
 * Network speed calculation result.
 */
typedef struct {
    uint64_t ingress_bytes_per_sec;
    uint64_t egress_bytes_per_sec;
    float ingress_mbps;
    float egress_mbps;
    int is_valid;
} NetworkSpeedNative;

/**
 * Maximum number of interfaces to track.
 */
#define MAX_INTERFACES 16

/**
 * Reads /proc/net/dev and parses all network interface statistics.
 * Optimized for minimal allocations and fast parsing.
 *
 * @param stats Array to store interface statistics (must have space for MAX_INTERFACES)
 * @param max_count Maximum number of interfaces to read
 * @return Number of interfaces read, or -1 on error
 */
int native_read_proc_net_dev(InterfaceStatsNative* stats, int max_count);

/**
 * Calculates aggregated network statistics from interface array.
 * Excludes loopback interface (lo).
 *
 * @param interfaces Array of interface statistics
 * @param count Number of interfaces in array
 * @param result Output structure for aggregated stats
 * @return 0 on success, -1 on error
 */
int native_aggregate_network_stats(
    const InterfaceStatsNative* interfaces,
    int count,
    NetworkStatsNative* result
);

/**
 * Calculates network speed from two snapshots.
 * Uses delta between snapshots divided by time.
 *
 * @param prev Previous snapshot
 * @param curr Current snapshot
 * @param result Output structure for speed calculation
 * @return 0 on success, -1 on error
 */
int native_calculate_network_speed(
    const NetworkStatsNative* prev,
    const NetworkStatsNative* curr,
    NetworkSpeedNative* result
);

/**
 * Reads and returns current total RX/TX bytes.
 * Lightweight function for quick polling.
 *
 * @param rx_bytes Output for received bytes
 * @param tx_bytes Output for transmitted bytes
 * @return 0 on success, -1 on error
 */
int native_get_total_bytes(uint64_t* rx_bytes, uint64_t* tx_bytes);

/**
 * Formats bytes per second to human-readable string.
 * Uses appropriate unit (B/s, KB/s, MB/s, GB/s).
 *
 * @param bytes_per_sec Speed in bytes per second
 * @param buffer Output buffer for formatted string
 * @param buffer_size Size of output buffer
 * @param prefix Prefix to add (e.g., "↓" or "↑")
 * @return Length of formatted string, or -1 on error
 */
int native_format_speed_string(
    uint64_t bytes_per_sec,
    char* buffer,
    int buffer_size,
    const char* prefix
);

/**
 * Formats bytes to human-readable string.
 * Uses appropriate unit (B, KB, MB, GB).
 *
 * @param bytes Byte count
 * @param buffer Output buffer
 * @param buffer_size Size of output buffer
 * @return Length of formatted string, or -1 on error
 */
int native_format_bytes_string(
    uint64_t bytes,
    char* buffer,
    int buffer_size
);

/**
 * Converts bytes per second to Mbps.
 *
 * @param bytes_per_sec Speed in bytes per second
 * @return Speed in Mbps
 */
float native_bytes_to_mbps(uint64_t bytes_per_sec);

/**
 * Checks if /proc/net/dev is accessible.
 *
 * @return 1 if accessible, 0 otherwise
 */
int native_is_proc_net_dev_available(void);

/* JNI function declarations */

JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetTotalRxBytes(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetTotalTxBytes(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jlongArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetNetworkSnapshot(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jfloatArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeCalculateSpeed(
    JNIEnv* env,
    jobject thiz,
    jlong prev_rx,
    jlong prev_tx,
    jlong prev_time,
    jlong curr_rx,
    jlong curr_tx,
    jlong curr_time
);

JNIEXPORT jstring JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeFormatSpeed(
    JNIEnv* env,
    jobject thiz,
    jlong bytes_per_sec,
    jstring prefix
);

JNIEXPORT jboolean JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeIsAvailable(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jint JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetInterfaceCount(
    JNIEnv* env,
    jobject thiz
);

#ifdef __cplusplus
}
#endif

#endif // SYSMETRICS_NATIVE_NETWORK_STATS_H
