#!/system/bin/sh
# Miracast Enabler - late service
# Runs after boot is completed

MODDIR=${0%/*}
LOGFILE="$MODDIR/miracast.log"

mlog() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOGFILE"
    log -t MiracastEnabler "$1"
}

# Truncate log on each boot
echo "" > "$LOGFILE"
mlog "Waiting for boot..."

# Wait for boot to complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done
sleep 3

mlog "Boot complete, applying Miracast configuration"

# --- Core WFD properties ---
# Some vendors reset these during boot, so re-apply
resetprop persist.debug.wfd.enable 1
resetprop persist.sys.wfd.virtual 0
resetprop persist.sys.wfd.nohdcp 1
resetprop wlan.wfd.hdcp disable

# Tensor / Pixel: force GPU composition for virtual display surfaces
# PowerVR (G5) and Mali (G4 and earlier) HWC implementations don't
# handle virtual display layers for Miracast, causing black/green frames
resetprop debug.sf.enable_hwc_vds 0

# Wi-Fi Direct concurrency — needed on Pixel to allow P2P alongside STA
resetprop wifi.direct.interface p2p-dev-wlan0

# --- Device detection ---
DEVICE=$(getprop ro.product.device)
SOC=$(getprop ro.soc.model)
PLATFORM=$(getprop ro.board.platform)
API=$(getprop ro.build.version.sdk)
GPU=$(getprop ro.hardware.egl)

mlog "Device=$DEVICE SoC=$SOC Platform=$PLATFORM API=$API GPU=$GPU"

# --- Tensor SoC family tweaks ---
case "$SOC" in
    Tensor*)
        # High GO intent so Pixel acts as Group Owner in P2P negotiation
        # This ensures the Pixel controls the channel and timing
        resetprop wifi.direct.go_intent 15
        mlog "Tensor SoC: set P2P GO intent=15"
        ;;
esac

# --- Pixel 10 Pro Fold (rango) — Tensor G5, Broadcom BCM4390 ---
# Confirmed: codename=rango, platform=laguna, gpu=powervr, wifi=bcmdhd4390
# Display IDs: 0 (inner), 3 (outer)
# Wi-Fi features include WFD_R2 — hardware fully supports Miracast
case "$DEVICE" in
    rango)
        mlog "Pixel 10 Pro Fold (rango) detected"

        # Inner display (ID 0) as default Miracast source
        if [ -z "$(getprop persist.sys.wfd.display_id)" ]; then
            resetprop persist.sys.wfd.display_id 0
        fi
        # Default 1080p30 — the inner panel is 2076x2152 which is too wide
        # for most Miracast receivers; 1080p is the safe maximum
        if [ -z "$(getprop persist.sys.wfd.resolution)" ]; then
            resetprop persist.sys.wfd.resolution 7
        fi

        # Tensor G5 / laguna platform: PowerVR GPU composition
        resetprop debug.sf.enable_hwc_vds 0

        # BCM4390 Wi-Fi: P2P supplicant already supports VHT and DFS
        # channels (p2p_go_vht=1, p2p_dfs_chan_enable=1 in vendor config)
        # but 6GHz is disabled for P2P (p2p_6ghz_disable=1), which is fine
        # since most Miracast receivers don't support 6GHz

        # Force single-channel concurrency to prevent the BCM4390 from
        # splitting STA and P2P across bands which can cause latency spikes
        resetprop persist.vendor.wifi.wfd.scc 1

        mlog "rango: inner display=0, outer display=3, wifi=BCM4390, gpu=PowerVR"
        ;;
esac

# --- Pixel 9 Pro Fold (comet/cometl) — Tensor G4 ---
case "$DEVICE" in
    comet|cometl)
        mlog "Pixel 9 Pro Fold detected"
        if [ -z "$(getprop persist.sys.wfd.display_id)" ]; then
            resetprop persist.sys.wfd.display_id 0
        fi
        if [ -z "$(getprop persist.sys.wfd.resolution)" ]; then
            resetprop persist.sys.wfd.resolution 7
        fi
        ;;
esac

# --- Original Pixel Fold (felix) — Tensor G2 ---
case "$DEVICE" in
    felix)
        mlog "Pixel Fold (1st gen) detected"
        if [ -z "$(getprop persist.sys.wfd.display_id)" ]; then
            resetprop persist.sys.wfd.display_id 0
        fi
        if [ -z "$(getprop persist.sys.wfd.resolution)" ]; then
            resetprop persist.sys.wfd.resolution 7
        fi
        ;;
esac

# --- Pixel 9 series (non-fold) — Tensor G4 ---
case "$DEVICE" in
    caiman|komodo|tokay|blazer)
        mlog "Pixel 9 series detected ($DEVICE)"
        # No foldable display concerns, standard single display
        ;;
esac

# --- Pixel 10 series (non-fold) ---
# Platform "laguna" is Tensor G5; other Pixel 10 variants share it
case "$PLATFORM" in
    laguna)
        if [ "$DEVICE" != "rango" ]; then
            mlog "Tensor G5 device ($DEVICE) on laguna platform"
            resetprop debug.sf.enable_hwc_vds 0
            resetprop persist.vendor.wifi.wfd.scc 1
        fi
        ;;
esac

# --- Fabricated overlay (Android 12+, API 31+) ---
# Primary method to flip config_enableWifiDisplay in the framework
# No APK compilation needed — fabricated overlays are created at runtime
# Requires root, which KernelSU provides
if [ "$API" -ge 31 ]; then
    mlog "API $API >= 31, creating fabricated overlays"

    # TYPE_INT_BOOLEAN = 0x12, true = 0xFFFFFFFF
    cmd overlay fabricate --target android --name MiracastEnablerWifiDisplay \
        android:bool/config_enableWifiDisplay 0x12 0xFFFFFFFF 2>/dev/null
    RET1=$?

    cmd overlay fabricate --target android --name MiracastEnablerProtectedBuffers \
        android:bool/config_wifiDisplaySupportsProtectedBuffers 0x12 0xFFFFFFFF 2>/dev/null
    RET2=$?

    # Enable the fabricated overlays
    cmd overlay enable --user current com.android.shell:MiracastEnablerWifiDisplay 2>/dev/null
    cmd overlay enable --user current com.android.shell:MiracastEnablerProtectedBuffers 2>/dev/null

    # Verify and log
    WFD_STATE=$(cmd overlay list 2>/dev/null | grep MiracastEnabler)
    mlog "Fabricate WifiDisplay ret=$RET1, ProtectedBuffers ret=$RET2"
    mlog "Overlay state: $WFD_STATE"
else
    mlog "API $API < 31, fabricated overlays not available"
    mlog "Relying on system properties and sysconfig XML only"
fi

# --- Fallback: enable pre-installed RRO APK if present ---
if [ -f "$MODDIR/system/vendor/overlay/MiracastEnablerOverlay.apk" ]; then
    cmd overlay enable --user current com.miracast.enabler.overlay 2>/dev/null
    mlog "Enabled RRO APK overlay"
fi

mlog "Miracast enabler service complete"
