#!/system/bin/sh
# Miracast Enabler - Installation Script

SKIPUNZIP=0

ui_print "================================================"
ui_print " Miracast Enabler v1.0.0"
ui_print "================================================"
ui_print ""

# Detect device
DEVICE=$(getprop ro.product.device)
MODEL=$(getprop ro.product.model)
SOC=$(getprop ro.soc.model)
ANDROID=$(getprop ro.build.version.release)
API=$(getprop ro.build.version.sdk)

ui_print "- Device: $MODEL ($DEVICE)"
ui_print "- SoC: $SOC"
ui_print "- Android: $ANDROID (API $API)"
ui_print ""

# Pixel detection
case "$DEVICE" in
    rango|comet|cometl|caiman|komodo|tokay|blazer|husky|shiba|felix|tangorpro|lynx|panther|cheetah|oriole|raven|bluejay)
        ui_print "- Pixel device detected"
        ;;
    *)
        ui_print "! Non-Pixel device — module may still work"
        ;;
esac

# Foldable messaging
case "$DEVICE" in
    rango|comet|cometl|felix)
        ui_print "- Foldable detected: inner display is default Miracast source"
        ui_print "- Change display source via WebUI in KernelSU manager"
        ;;
esac
ui_print ""

# --- Build RRO APK on-device as fallback for pre-Android 12 ---
# Android 12+ uses fabricated overlays (no APK needed)
# Pre-Android 12 needs a real RRO APK, built right here on the phone
if [ "$API" -lt 31 ]; then
    ui_print "- Android < 12: building RRO overlay APK on-device..."

    AAPT2=$(which aapt2 2>/dev/null)
    if [ -z "$AAPT2" ]; then
        # aapt2 is typically in the build-tools or framework
        for candidate in \
            /system/bin/aapt2 \
            /system/xbin/aapt2 \
            /data/adb/aapt2; do
            if [ -x "$candidate" ]; then
                AAPT2="$candidate"
                break
            fi
        done
    fi

    if [ -n "$AAPT2" ] && [ -x "$AAPT2" ]; then
        ui_print "- Found aapt2: $AAPT2"
        OVERLAY_BUILD="$MODPATH/overlay_build"
        mkdir -p "$OVERLAY_BUILD"

        # Compile resources
        "$AAPT2" compile \
            --dir "$MODPATH/overlay/res" \
            -o "$OVERLAY_BUILD/compiled.zip" 2>/dev/null

        if [ $? -eq 0 ]; then
            # Link into APK
            "$AAPT2" link \
                --manifest "$MODPATH/overlay/AndroidManifest.xml" \
                -I /system/framework/framework-res.apk \
                -o "$OVERLAY_BUILD/overlay.apk" \
                "$OVERLAY_BUILD/compiled.zip" 2>/dev/null

            if [ $? -eq 0 ]; then
                mkdir -p "$MODPATH/system/vendor/overlay"
                cp "$OVERLAY_BUILD/overlay.apk" "$MODPATH/system/vendor/overlay/MiracastEnablerOverlay.apk"
                chmod 644 "$MODPATH/system/vendor/overlay/MiracastEnablerOverlay.apk"
                ui_print "- RRO overlay APK built and installed"
            else
                ui_print "! aapt2 link failed — overlay APK not built"
            fi
        else
            ui_print "! aapt2 compile failed — overlay APK not built"
        fi
        rm -rf "$OVERLAY_BUILD"
    else
        ui_print "! aapt2 not found on device"
        ui_print "! Pre-Android 12 will rely on system properties only"
    fi
else
    ui_print "- Android 12+: will use fabricated overlays (no APK needed)"
fi

ui_print ""
ui_print "- Setting permissions..."

set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/post-fs-data.sh 0 0 0755
set_perm $MODPATH/service.sh 0 0 0755
set_perm_recursive $MODPATH/system/etc 0 0 0755 0644

if [ -f "$MODPATH/system/vendor/overlay/MiracastEnablerOverlay.apk" ]; then
    set_perm "$MODPATH/system/vendor/overlay/MiracastEnablerOverlay.apk" 0 0 0644
fi

ui_print ""
ui_print "- Installation complete!"
ui_print "- Reboot to activate Miracast"
ui_print "- After reboot: Settings > Connected devices >"
ui_print "  Connection preferences > Cast"
ui_print "- WebUI available in KernelSU manager"
ui_print ""
