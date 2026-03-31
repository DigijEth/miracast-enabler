#!/system/bin/sh
# Miracast Enabler - post-fs-data
# Runs early in boot before most services start

MODDIR=${0%/*}

# Core Wi-Fi Display props
resetprop persist.debug.wfd.enable 1
resetprop persist.sys.wfd.virtual 0

# HDCP — disable by default for maximum compatibility
resetprop persist.sys.wfd.nohdcp 1
resetprop wlan.wfd.hdcp disable

# Tensor / Pixel: force GPU composition for virtual displays
# HWC on Tensor chips does not correctly handle virtual display layers,
# which causes black screen or green artifacts during Miracast
resetprop debug.sf.enable_hwc_vds 0

# Wi-Fi Direct concurrency — needed on Pixel to allow P2P alongside STA
resetprop wifi.direct.interface p2p-dev-wlan0
