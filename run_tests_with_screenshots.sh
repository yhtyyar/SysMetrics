#!/bin/bash
# Run tests and collect screenshots immediately

set -e

PROJECT_DIR="/home/tester/CascadeProjects/SysMetrics"
REPORT_DIR="$PROJECT_DIR/allure-report"
SCREENSHOT_DIR="$REPORT_DIR/screenshots"

echo "================================"
echo "🧪 Running UI Tests + 📸 Screenshots"
echo "================================"

# Create screenshot directory
mkdir -p "$SCREENSHOT_DIR"

# Check connected devices
echo ""
echo "🔍 Checking devices..."
adb devices | grep -v "List" | grep "device$" || {
    echo "❌ No devices connected"
    exit 1
}

# Run tests
echo ""
echo "🚀 Running tests..."
cd "$PROJECT_DIR"
./gradlew connectedAndroidTest --no-daemon || true

# Immediately pull screenshots from all devices
echo ""
echo "📥 Collecting screenshots..."

DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')
TOTAL=0

for DEVICE in $DEVICES; do
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "$DEVICE")
    echo ""
    echo "📱 $MODEL"
    
    # Try to find and pull screenshots from app-specific directories
    for PKG in "com.sysmetrics.app" "com.sysmetrics.app.debug"; do
        REMOTE_DIR="/sdcard/Android/data/$PKG/files/screenshots"
        
        echo "   Checking: $REMOTE_DIR"
        
        # Check if directory exists and has files
        if adb -s "$DEVICE" shell "ls $REMOTE_DIR/*.png 2>/dev/null" >/dev/null 2>&1; then
            echo "   📂 Found screenshots!"
            
            # Pull all screenshots
            while IFS= read -r file; do
                [ -z "$file" ] && continue
                FILENAME=$(basename "$file" | tr -d '\r')
                LOCAL_NAME="${MODEL}_${FILENAME}"
                
                if adb -s "$DEVICE" pull "$file" "$SCREENSHOT_DIR/$LOCAL_NAME" 2>/dev/null; then
                    echo "      ✓ $LOCAL_NAME"
                    ((TOTAL++))
                fi
            done < <(adb -s "$DEVICE" shell "ls $REMOTE_DIR/*.png 2>/dev/null")
        fi
    done
done

echo ""
echo "================================"
echo "📊 Total collected: $TOTAL screenshots"
echo "📁 Location: $SCREENSHOT_DIR"
echo "================================"

# List collected screenshots
echo ""
echo "📋 Screenshots:"
ls -la "$SCREENSHOT_DIR"/*.png 2>/dev/null || echo "   No screenshots found"
