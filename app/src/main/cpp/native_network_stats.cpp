#include "native_network_stats.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/time.h>
#include <errno.h>
#include <inttypes.h>

// Path to /proc/net/dev
static const char* PROC_NET_DEV = "/proc/net/dev";

// Buffer size for reading file
#define READ_BUFFER_SIZE 4096

// Get current timestamp in milliseconds
static int64_t get_timestamp_ms() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

// Check if interface is loopback
static int is_loopback(const char* name) {
    return strcmp(name, "lo") == 0;
}

// Parse a single line from /proc/net/dev
// Format: "interface: rx_bytes rx_packets rx_errs rx_drop ... tx_bytes tx_packets ..."
static int parse_interface_line(const char* line, InterfaceStatsNative* stats, int64_t timestamp) {
    // Find colon separator
    const char* colon = strchr(line, ':');
    if (!colon) return -1;
    
    // Extract interface name (trim leading whitespace)
    const char* name_start = line;
    while (*name_start == ' ' || *name_start == '\t') name_start++;
    
    size_t name_len = colon - name_start;
    if (name_len >= sizeof(stats->interface_name)) {
        name_len = sizeof(stats->interface_name) - 1;
    }
    
    strncpy(stats->interface_name, name_start, name_len);
    stats->interface_name[name_len] = '\0';
    
    // Parse values after colon
    // rx: bytes packets errs drop fifo frame compressed multicast
    // tx: bytes packets errs drop fifo colls carrier compressed
    const char* values = colon + 1;
    
    uint64_t rx_bytes, rx_packets, rx_errs, rx_drop;
    uint64_t tx_bytes, tx_packets, tx_errs, tx_drop;
    uint64_t dummy; // For unused fields
    
    int parsed = sscanf(values,
        "%" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64 " %" SCNu64,
        &rx_bytes, &rx_packets, &rx_errs, &rx_drop, &dummy, &dummy, &dummy, &dummy,
        &tx_bytes, &tx_packets, &tx_errs, &tx_drop);
    
    if (parsed < 12) return -1;
    
    stats->rx_bytes = rx_bytes;
    stats->rx_packets = rx_packets;
    stats->rx_errors = rx_errs;
    stats->rx_dropped = rx_drop;
    stats->tx_bytes = tx_bytes;
    stats->tx_packets = tx_packets;
    stats->tx_errors = tx_errs;
    stats->tx_dropped = tx_drop;
    stats->timestamp_ms = timestamp;
    
    return 0;
}

int native_read_proc_net_dev(InterfaceStatsNative* stats, int max_count) {
    if (!stats || max_count <= 0) return -1;
    
    int fd = open(PROC_NET_DEV, O_RDONLY);
    if (fd < 0) return -1;
    
    char buffer[READ_BUFFER_SIZE];
    ssize_t bytes_read = read(fd, buffer, sizeof(buffer) - 1);
    close(fd);
    
    if (bytes_read <= 0) return -1;
    buffer[bytes_read] = '\0';
    
    int64_t timestamp = get_timestamp_ms();
    int count = 0;
    int line_num = 0;
    
    char* line = strtok(buffer, "\n");
    while (line && count < max_count) {
        line_num++;
        
        // Skip first two header lines
        if (line_num > 2) {
            if (parse_interface_line(line, &stats[count], timestamp) == 0) {
                count++;
            }
        }
        
        line = strtok(NULL, "\n");
    }
    
    return count;
}

int native_aggregate_network_stats(
    const InterfaceStatsNative* interfaces,
    int count,
    NetworkStatsNative* result
) {
    if (!interfaces || !result || count < 0) return -1;
    
    memset(result, 0, sizeof(NetworkStatsNative));
    result->timestamp_ms = get_timestamp_ms();
    
    int valid_count = 0;
    for (int i = 0; i < count; i++) {
        // Skip loopback interface
        if (is_loopback(interfaces[i].interface_name)) continue;
        
        result->total_rx_bytes += interfaces[i].rx_bytes;
        result->total_tx_bytes += interfaces[i].tx_bytes;
        valid_count++;
    }
    
    result->interface_count = valid_count;
    result->is_valid = 1;
    
    return 0;
}

int native_calculate_network_speed(
    const NetworkStatsNative* prev,
    const NetworkStatsNative* curr,
    NetworkSpeedNative* result
) {
    if (!prev || !curr || !result) return -1;
    if (!prev->is_valid || !curr->is_valid) return -1;
    
    memset(result, 0, sizeof(NetworkSpeedNative));
    
    int64_t time_delta_ms = curr->timestamp_ms - prev->timestamp_ms;
    if (time_delta_ms <= 0) {
        result->is_valid = 0;
        return 0;
    }
    
    // Calculate byte deltas (handle counter wrap)
    uint64_t rx_delta = 0;
    uint64_t tx_delta = 0;
    
    if (curr->total_rx_bytes >= prev->total_rx_bytes) {
        rx_delta = curr->total_rx_bytes - prev->total_rx_bytes;
    }
    if (curr->total_tx_bytes >= prev->total_tx_bytes) {
        tx_delta = curr->total_tx_bytes - prev->total_tx_bytes;
    }
    
    // Calculate bytes per second
    float time_delta_sec = time_delta_ms / 1000.0f;
    result->ingress_bytes_per_sec = (uint64_t)(rx_delta / time_delta_sec);
    result->egress_bytes_per_sec = (uint64_t)(tx_delta / time_delta_sec);
    
    // Convert to Mbps: bytes/sec * 8 / 1024 / 1024
    result->ingress_mbps = native_bytes_to_mbps(result->ingress_bytes_per_sec);
    result->egress_mbps = native_bytes_to_mbps(result->egress_bytes_per_sec);
    
    result->is_valid = 1;
    return 0;
}

int native_get_total_bytes(uint64_t* rx_bytes, uint64_t* tx_bytes) {
    if (!rx_bytes || !tx_bytes) return -1;
    
    InterfaceStatsNative interfaces[MAX_INTERFACES];
    int count = native_read_proc_net_dev(interfaces, MAX_INTERFACES);
    
    if (count < 0) return -1;
    
    *rx_bytes = 0;
    *tx_bytes = 0;
    
    for (int i = 0; i < count; i++) {
        if (is_loopback(interfaces[i].interface_name)) continue;
        *rx_bytes += interfaces[i].rx_bytes;
        *tx_bytes += interfaces[i].tx_bytes;
    }
    
    return 0;
}

int native_format_speed_string(
    uint64_t bytes_per_sec,
    char* buffer,
    int buffer_size,
    const char* prefix
) {
    if (!buffer || buffer_size <= 0) return -1;
    
    const char* pfx = prefix ? prefix : "";
    
    if (bytes_per_sec < 1024) {
        return snprintf(buffer, buffer_size, "%s%" PRIu64 " B/s", pfx, bytes_per_sec);
    } else if (bytes_per_sec < 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%s%.1f KB/s", pfx, bytes_per_sec / 1024.0f);
    } else if (bytes_per_sec < 1024ULL * 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%s%.2f MB/s", pfx, bytes_per_sec / (1024.0f * 1024.0f));
    } else {
        return snprintf(buffer, buffer_size, "%s%.2f GB/s", pfx, bytes_per_sec / (1024.0f * 1024.0f * 1024.0f));
    }
}

int native_format_bytes_string(uint64_t bytes, char* buffer, int buffer_size) {
    if (!buffer || buffer_size <= 0) return -1;
    
    if (bytes < 1024) {
        return snprintf(buffer, buffer_size, "%" PRIu64 " B", bytes);
    } else if (bytes < 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%.1f KB", bytes / 1024.0f);
    } else if (bytes < 1024ULL * 1024 * 1024) {
        return snprintf(buffer, buffer_size, "%.1f MB", bytes / (1024.0f * 1024.0f));
    } else {
        return snprintf(buffer, buffer_size, "%.2f GB", bytes / (1024.0f * 1024.0f * 1024.0f));
    }
}

float native_bytes_to_mbps(uint64_t bytes_per_sec) {
    // Mbps = bytes/sec * 8 bits/byte / 1024 / 1024
    return bytes_per_sec * 8.0f / (1024.0f * 1024.0f);
}

int native_is_proc_net_dev_available(void) {
    int fd = open(PROC_NET_DEV, O_RDONLY);
    if (fd < 0) return 0;
    close(fd);
    return 1;
}

/* JNI Implementations */

JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetTotalRxBytes(
    JNIEnv* env,
    jobject thiz
) {
    uint64_t rx_bytes, tx_bytes;
    if (native_get_total_bytes(&rx_bytes, &tx_bytes) < 0) {
        return -1;
    }
    return (jlong)rx_bytes;
}

JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetTotalTxBytes(
    JNIEnv* env,
    jobject thiz
) {
    uint64_t rx_bytes, tx_bytes;
    if (native_get_total_bytes(&rx_bytes, &tx_bytes) < 0) {
        return -1;
    }
    return (jlong)tx_bytes;
}

JNIEXPORT jlongArray JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetNetworkSnapshot(
    JNIEnv* env,
    jobject thiz
) {
    uint64_t rx_bytes, tx_bytes;
    if (native_get_total_bytes(&rx_bytes, &tx_bytes) < 0) {
        return NULL;
    }
    
    int64_t timestamp = get_timestamp_ms();
    
    // Return array: [rx_bytes, tx_bytes, timestamp]
    jlongArray result = env->NewLongArray(3);
    if (result == NULL) return NULL;
    
    jlong data[3] = { (jlong)rx_bytes, (jlong)tx_bytes, (jlong)timestamp };
    env->SetLongArrayRegion(result, 0, 3, data);
    
    return result;
}

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
) {
    int64_t time_delta_ms = curr_time - prev_time;
    if (time_delta_ms <= 0) {
        return NULL;
    }
    
    uint64_t rx_delta = (curr_rx > prev_rx) ? (curr_rx - prev_rx) : 0;
    uint64_t tx_delta = (curr_tx > prev_tx) ? (curr_tx - prev_tx) : 0;
    
    float time_delta_sec = time_delta_ms / 1000.0f;
    uint64_t rx_bps = (uint64_t)(rx_delta / time_delta_sec);
    uint64_t tx_bps = (uint64_t)(tx_delta / time_delta_sec);
    
    float rx_mbps = native_bytes_to_mbps(rx_bps);
    float tx_mbps = native_bytes_to_mbps(tx_bps);
    
    // Return array: [rx_bytes_per_sec, tx_bytes_per_sec, rx_mbps, tx_mbps]
    jfloatArray result = env->NewFloatArray(4);
    if (result == NULL) return NULL;
    
    jfloat data[4] = { (jfloat)rx_bps, (jfloat)tx_bps, rx_mbps, tx_mbps };
    env->SetFloatArrayRegion(result, 0, 4, data);
    
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeFormatSpeed(
    JNIEnv* env,
    jobject thiz,
    jlong bytes_per_sec,
    jstring prefix
) {
    const char* prefix_str = NULL;
    if (prefix != NULL) {
        prefix_str = env->GetStringUTFChars(prefix, NULL);
    }
    
    char buffer[64];
    native_format_speed_string((uint64_t)bytes_per_sec, buffer, sizeof(buffer), prefix_str);
    
    if (prefix_str != NULL) {
        env->ReleaseStringUTFChars(prefix, prefix_str);
    }
    
    return env->NewStringUTF(buffer);
}

JNIEXPORT jboolean JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeIsAvailable(
    JNIEnv* env,
    jobject thiz
) {
    return native_is_proc_net_dev_available() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_sysmetrics_app_native_1bridge_NativeNetworkMetrics_nativeGetInterfaceCount(
    JNIEnv* env,
    jobject thiz
) {
    InterfaceStatsNative interfaces[MAX_INTERFACES];
    int count = native_read_proc_net_dev(interfaces, MAX_INTERFACES);
    
    if (count < 0) return 0;
    
    // Count non-loopback interfaces
    int valid_count = 0;
    for (int i = 0; i < count; i++) {
        if (!is_loopback(interfaces[i].interface_name)) {
            valid_count++;
        }
    }
    
    return valid_count;
}
