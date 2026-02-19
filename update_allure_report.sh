#!/bin/bash
# Screenshot collection script for SysMetrics Allure report

set -e

PROJECT_DIR="/home/tester/CascadeProjects/SysMetrics"
REPORT_DIR="$PROJECT_DIR/allure-report"
SCREENSHOT_DIR="$REPORT_DIR/screenshots"

# Screenshot locations on device
DEVICE_PATHS=(
    "/sdcard/Android/data/com.sysmetrics.app/screenshots"
    "/sdcard/Android/data/com.sysmetrics.app/files/screenshots"
    "/sdcard/Download/SysMetrics"
    "/sdcard/Pictures/SysMetrics"
)

echo "================================"
echo "📸 Screenshot Collection"
echo "================================"

mkdir -p "$SCREENSHOT_DIR"

# Check devices
echo ""
echo "🔍 Checking devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ No devices found"
    exit 1
fi

TOTAL=0

for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Device")
    echo ""
    echo "📱 $MODEL ($DEVICE)"
    
    for PATH in "${DEVICE_PATHS[@]}"; do
        if adb -s "$DEVICE" shell "[ -d $PATH ]" 2>/dev/null; then
            FILES=$(adb -s "$DEVICE" shell ls "$PATH"/*.png 2>/dev/null | tr -d '\r')
            if [ -n "$FILES" ]; then
                echo "   📂 Found: $PATH"
                while read -r file; do
                    [ -n "$file" ] || continue
                    NAME=$(basename "$file")
                    if adb -s "$DEVICE" pull "$file" "$SCREENSHOT_DIR/${MODEL}_${NAME}" 2>/dev/null; then
                        echo "      ✓ $NAME"
                        ((TOTAL++))
                    fi
                done <<< "$FILES"
            fi
        fi
    done
done

echo ""
echo "================================"
echo "📊 Total: $TOTAL screenshots"
echo "📁 Location: $SCREENSHOT_DIR"
echo "🌐 Report: file://$REPORT_DIR/index.html"
echo "================================"
