cmake_minimum_required(VERSION 3.22.1)

project("sysmetrics_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for release
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -ffast-math -DNDEBUG")

# Create shared library
add_library(sysmetrics_native SHARED
    native_metrics.cpp
    native_network_stats.cpp
    native_analytics.cpp
)

# Find required libraries
find_library(log-lib log)

# Link libraries
target_link_libraries(sysmetrics_native
    ${log-lib}
)

# Include directories
target_include_directories(sysmetrics_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
