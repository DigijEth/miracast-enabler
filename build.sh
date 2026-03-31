#!/bin/bash
# Build Miracast Enabler KernelSU/Magisk module zip
#
# The module uses fabricated overlays at runtime (no APK needed).
# If you have a local aapt2 binary (e.g. pulled from the phone),
# set AAPT2=/path/to/aapt2 and FRAMEWORK_RES=/path/to/framework-res.apk
# to also build an RRO APK as a fallback for pre-Android 12 devices.
set -e

cd "$(dirname "$0")"

MODULE_ZIP="miracast-enabler-v1.0.0.zip"

echo "=== Miracast Enabler Build ==="
echo ""

# Optional: build RRO APK if tools are available
AAPT2="${AAPT2:-tools/aapt2}"
FRAMEWORK_RES="${FRAMEWORK_RES:-tools/framework-res.apk}"

if [ -x "$AAPT2" ] && [ -f "$FRAMEWORK_RES" ]; then
    echo "[1/3] Building RRO overlay APK..."

    OVERLAY_DIR="overlay"
    rm -rf "$OVERLAY_DIR/build"
    mkdir -p "$OVERLAY_DIR/build"

    "$AAPT2" compile \
        --dir "$OVERLAY_DIR/res" \
        -o "$OVERLAY_DIR/build/compiled.zip"

    "$AAPT2" link \
        --manifest "$OVERLAY_DIR/AndroidManifest.xml" \
        -I "$FRAMEWORK_RES" \
        -o "$OVERLAY_DIR/build/MiracastEnablerOverlay.apk" \
        "$OVERLAY_DIR/build/compiled.zip"

    # Sign if possible
    KEYSTORE="build-key.jks"
    KEY_ALIAS="miracast"
    KEY_PASS="miracast-build"

    if ! [ -f "$KEYSTORE" ] && command -v keytool >/dev/null 2>&1; then
        keytool -genkeypair \
            -keystore "$KEYSTORE" \
            -alias "$KEY_ALIAS" \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass "$KEY_PASS" -keypass "$KEY_PASS" \
            -dname "CN=Miracast Enabler,O=Module,C=US"
    fi

    if [ -f "$KEYSTORE" ]; then
        if command -v apksigner >/dev/null 2>&1; then
            apksigner sign \
                --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
                --ks-pass "pass:$KEY_PASS" --key-pass "pass:$KEY_PASS" \
                --out "$OVERLAY_DIR/MiracastEnablerOverlay.apk" \
                "$OVERLAY_DIR/build/MiracastEnablerOverlay.apk"
        elif command -v jarsigner >/dev/null 2>&1; then
            cp "$OVERLAY_DIR/build/MiracastEnablerOverlay.apk" "$OVERLAY_DIR/MiracastEnablerOverlay.apk"
            jarsigner -keystore "$KEYSTORE" \
                -storepass "$KEY_PASS" -keypass "$KEY_PASS" \
                "$OVERLAY_DIR/MiracastEnablerOverlay.apk" "$KEY_ALIAS"
        else
            cp "$OVERLAY_DIR/build/MiracastEnablerOverlay.apk" "$OVERLAY_DIR/MiracastEnablerOverlay.apk"
        fi
    else
        cp "$OVERLAY_DIR/build/MiracastEnablerOverlay.apk" "$OVERLAY_DIR/MiracastEnablerOverlay.apk"
    fi

    mkdir -p system/vendor/overlay
    cp "$OVERLAY_DIR/MiracastEnablerOverlay.apk" system/vendor/overlay/
    echo "   RRO APK built: $(du -h system/vendor/overlay/MiracastEnablerOverlay.apk | cut -f1)"
    rm -rf "$OVERLAY_DIR/build"
else
    echo "[1/3] Skipping RRO APK (no local aapt2 or framework-res.apk)"
    echo "   Module will use fabricated overlays on Android 12+"
    echo "   To build APK: pull aapt2 from phone with ./pull-aapt2.sh"
    rm -rf system/vendor/overlay
fi

echo "[2/3] Packaging module..."
rm -f "$MODULE_ZIP"

# Build file list
FILES=(
    module.prop
    customize.sh
    post-fs-data.sh
    service.sh
    system.prop
    system/
    webroot/
)

# Include overlay source for on-device build fallback
FILES+=(overlay/AndroidManifest.xml overlay/res/)

# Include pre-built APK if it exists
if [ -f "system/vendor/overlay/MiracastEnablerOverlay.apk" ]; then
    FILES+=(system/vendor/overlay/MiracastEnablerOverlay.apk)
fi

zip -r9 "$MODULE_ZIP" "${FILES[@]}" \
    -x "overlay/build/*" "*.git*" "build.sh" "build-key.jks" \
    "pull-aapt2.sh" "tools/*" "README*"

echo "[3/3] Done"
echo ""
echo "=== $MODULE_ZIP ($(du -h "$MODULE_ZIP" | cut -f1)) ==="
echo "Flash via KernelSU or Magisk manager"
