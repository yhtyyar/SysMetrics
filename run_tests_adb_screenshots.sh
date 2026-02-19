#!/bin/bash
# Run tests with immediate ADB screenshot capture

set -e

PROJECT_DIR="/home/tester/CascadeProjects/SysMetrics"
REPORT_DIR="$PROJECT_DIR/allure-report"
SCREENSHOT_DIR="$REPORT_DIR/screenshots"

echo "================================"
echo "🧪 Tests + 📸 ADB Screenshots"
echo "================================"

# Create screenshot directory
mkdir -p "$SCREENSHOT_DIR"

# Get list of devices
echo ""
echo "🔍 Checking devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ No devices connected"
    exit 1
fi

echo "📱 Found devices:"
echo "$DEVICES"

# Start background screenshot capture for each device
echo ""
echo "📸 Starting ADB screenshot capture..."

for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "$DEVICE")
    echo "   Starting capture for $MODEL ($DEVICE)"
    
    # Create device screenshot dir
    mkdir -p "$SCREENSHOT_DIR/$MODEL"
    
    # Start background capture process that takes screenshots every 2 seconds
    (
        COUNTER=1
        while true; do
            TIMESTAMP=$(date +%Y%m%d_%H%M%S)
            FILENAME="${MODEL}_${COUNTER}_${TIMESTAMP}.png"
            
            # Capture screenshot via ADB and save directly to host
            if adb -s "$DEVICE" exec-out screencap -p > "$SCREENSHOT_DIR/$MODEL/$FILENAME" 2>/dev/null; then
                # Check if file has content
                if [ -s "$SCREENSHOT_DIR/$MODEL/$FILENAME" ]; then
                    echo "      ✓ Captured: $FILENAME"
                else
                    rm -f "$SCREENSHOT_DIR/$MODEL/$FILENAME"
                fi
            fi
            
            COUNTER=$((COUNTER + 1))
            sleep 2
        done
    ) &
    
    # Save PID to kill later
    echo $! > "/tmp/screenshot_capture_${DEVICE}.pid"
done

# Run tests
echo ""
echo "🚀 Running tests..."
cd "$PROJECT_DIR"
./gradlew connectedAndroidTest --no-daemon 2>&1 | tail -50 || true

# Stop background screenshot processes
echo ""
echo "🛑 Stopping screenshot capture..."
for DEVICE in $DEVICES; do
    if [ -f "/tmp/screenshot_capture_${DEVICE}.pid" ]; then
        PID=$(cat "/tmp/screenshot_capture_${DEVICE}.pid")
        kill $PID 2>/dev/null || true
        rm -f "/tmp/screenshot_capture_${DEVICE}.pid"
    fi
done

# Count and display collected screenshots
echo ""
echo "================================"
echo "📊 Screenshot Collection Summary"
echo "================================"

TOTAL=0
for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "$DEVICE")
    COUNT=$(find "$SCREENSHOT_DIR/$MODEL" -name "*.png" 2>/dev/null | wc -l)
    TOTAL=$((TOTAL + COUNT))
    echo "📱 $MODEL: $COUNT screenshots"
done

echo ""
echo "📁 Total: $TOTAL screenshots"
echo "📂 Location: $SCREENSHOT_DIR"
echo "================================"

# List all collected screenshots
echo ""
echo "📋 Screenshot files:"
find "$SCREENSHOT_DIR" -name "*.png" -exec ls -lh {} \; 2>/dev/null || echo "   No screenshots collected"
