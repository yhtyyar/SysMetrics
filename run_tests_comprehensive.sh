#!/bin/bash
# Comprehensive test runner with ADB screenshot capture
# This script runs tests and captures screenshots via ADB in real-time

set -e

PROJECT_DIR="/home/tester/CascadeProjects/SysMetrics"
REPORT_DIR="$PROJECT_DIR/allure-report"
SCREENSHOT_DIR="$REPORT_DIR/screenshots"
ADB_SCREENSHOT_DIR="$PROJECT_DIR/adb-screenshots"

echo "================================"
echo "🧪 SysMetrics Test Suite"
echo "================================"

# Setup directories
mkdir -p "$SCREENSHOT_DIR"
mkdir -p "$ADB_SCREENSHOT_DIR"

# Check for connected devices
echo ""
echo "🔍 Checking for devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ No devices connected. Please connect a device or start an emulator."
    exit 1
fi

echo "✅ Found devices:"
for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
    echo "   📱 $MODEL ($DEVICE)"
done

# Function to capture screenshot via ADB
capture_screenshot() {
    local DEVICE=$1
    local MODEL=$2
    local PREFIX=$3
    local TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    local FILENAME="${PREFIX}_${TIMESTAMP}.png"
    local DEVICE_DIR="$ADB_SCREENSHOT_DIR/$MODEL"
    
    mkdir -p "$DEVICE_DIR"
    
    # Capture via ADB screencap
    if adb -s "$DEVICE" exec-out screencap -p > "$DEVICE_DIR/$FILENAME" 2>/dev/null; then
        if [ -s "$DEVICE_DIR/$FILENAME" ]; then
            echo "      ✓ Screenshot: $FILENAME"
            return 0
        else
            rm -f "$DEVICE_DIR/$FILENAME"
            return 1
        fi
    fi
    return 1
}

# Start background screenshot capture
echo ""
echo "📸 Starting ADB screenshot capture..."
echo "   (Will capture every 3 seconds during test execution)"

CAPTURE_PIDS=""
for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "$DEVICE")
    
    (
        COUNTER=1
        while true; do
            capture_screenshot "$DEVICE" "$MODEL" "$(printf '%03d' $COUNTER)"
            COUNTER=$((COUNTER + 1))
            sleep 3
        done
    ) &
    
    PID=$!
    CAPTURE_PIDS="$CAPTURE_PIDS $PID"
    echo "   Started capture process (PID: $PID) for $MODEL"
done

# Run tests
echo ""
echo "🚀 Running UI tests..."
echo "================================"
cd "$PROJECT_DIR"

# Run connected Android tests
if ./gradlew connectedAndroidTest --no-daemon 2>&1; then
    TEST_STATUS="✅ PASSED"
else
    TEST_STATUS="❌ FAILED"
fi

# Stop screenshot capture
echo ""
echo "🛑 Stopping screenshot capture..."
for PID in $CAPTURE_PIDS; do
    kill $PID 2>/dev/null || true
done
sleep 1

# Count screenshots
echo ""
echo "================================"
echo "📊 Test Results & Screenshots"
echo "================================"
echo "Tests: $TEST_STATUS"
echo ""

TOTAL_SCREENSHOTS=0
for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "$DEVICE")
    DEVICE_DIR="$ADB_SCREENSHOT_DIR/$MODEL"
    
    if [ -d "$DEVICE_DIR" ]; then
        COUNT=$(find "$DEVICE_DIR" -name "*.png" | wc -l)
        TOTAL_SCREENSHOTS=$((TOTAL_SCREENSHOTS + COUNT))
        
        if [ $COUNT -gt 0 ]; then
            echo "📱 $MODEL: $COUNT screenshots"
            # List first few screenshots
            ls -lh "$DEVICE_DIR"/*.png 2>/dev/null | head -5 | awk '{print "      " $9 " (" $5 ")"}'
            if [ $COUNT -gt 5 ]; then
                echo "      ... and $((COUNT - 5)) more"
            fi
        else
            echo "📱 $MODEL: No screenshots"
        fi
    fi
done

echo ""
echo "================================"
echo "📁 Total: $TOTAL_SCREENSHOTS screenshots"
echo "📂 Location: $ADB_SCREENSHOT_DIR"
echo "================================"

# Also try to pull any screenshots from device storage (in-app captures)
echo ""
echo "📥 Checking for in-app screenshots..."
for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "$DEVICE")
    
    # Check app-specific directories
    for PKG in "com.sysmetrics.app" "com.sysmetrics.app.debug"; do
        REMOTE_DIR="/sdcard/Android/data/$PKG/files/screenshots"
        
        if adb -s "$DEVICE" shell "ls $REMOTE_DIR/*.png 2>/dev/null" >/dev/null 2>&1; then
            echo "   📂 Found in-app screenshots for $PKG"
            
            # Create directory for in-app screenshots
            INAPP_DIR="$SCREENSHOT_DIR/${MODEL}_inapp"
            mkdir -p "$INAPP_DIR"
            
            # Pull screenshots
            adb -s "$DEVICE" pull "$REMOTE_DIR/" "$INAPP_DIR/" 2>/dev/null || true
            
            COUNT=$(find "$INAPP_DIR" -name "*.png" 2>/dev/null | wc -l)
            echo "      ✓ Pulled $COUNT in-app screenshots"
        fi
    done
done

echo ""
echo "🌐 Allure Report: file://$REPORT_DIR/index.html"
echo "================================"
