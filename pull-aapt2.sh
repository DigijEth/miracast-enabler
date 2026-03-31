#!/bin/bash
# Pull aapt2 from a connected Android device for local builds
# Usage: ./pull-aapt2.sh
set -e

DEST="tools/aapt2"

if ! adb get-state >/dev/null 2>&1; then
    echo "ERROR: No device connected via ADB"
    exit 1
fi

echo "Searching for aapt2 on device..."

# Find aapt2 binary on the device
DEVICE_PATH=$(adb shell "find /system /vendor /apex -name 'aapt2' -type f 2>/dev/null" | tr -d '\r' | head -1)

if [ -z "$DEVICE_PATH" ]; then
    echo "aapt2 not found as standalone binary, checking APEX modules..."
    # On newer Android, build tools live inside APEX
    DEVICE_PATH=$(adb shell "find /apex/com.android.sdkext /apex/com.android.art -name 'aapt2' 2>/dev/null" | tr -d '\r' | head -1)
fi

if [ -z "$DEVICE_PATH" ]; then
    echo "aapt2 not found on device."
    echo "Trying framework-res.apk instead (for on-device overlay builds)..."
    DEVICE_PATH="/system/framework/framework-res.apk"
    DEST="tools/framework-res.apk"
fi

echo "Pulling: $DEVICE_PATH"
mkdir -p "$(dirname "$DEST")"
adb pull "$DEVICE_PATH" "$DEST"

if [ -f "$DEST" ]; then
    chmod +x "$DEST" 2>/dev/null
    echo "Saved to: $DEST"
    file "$DEST"

    if [[ "$DEST" == *aapt2 ]]; then
        VERSION=$("./$DEST" version 2>&1 || true)
        echo "Version: $VERSION"
    fi
fi
