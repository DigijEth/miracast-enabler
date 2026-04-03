package com.miracast.enabler.util;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Wrapper around Android's hidden DisplayManager Wi-Fi Display APIs.
 * Uses reflection since these methods are @hide.
 *
 * Key hidden methods on DisplayManager:
 * - connectWifiDisplay(String address)
 * - disconnectWifiDisplay()
 * - startWifiDisplayScan()
 * - stopWifiDisplayScan()
 * - getWifiDisplayStatus() -> WifiDisplayStatus
 *
 * WifiDisplayStatus has:
 * - getFeatureState() -> int (0=unavail, 1=disabled, 2=off, 3=on)
 * - getActiveDisplay() -> WifiDisplay (or null)
 * - getAvailableDisplays() -> WifiDisplay[]
 * - getScanState() -> int (0=not scanning, 1=scanning)
 * - getActiveDisplayState() -> int (0=disconnected, 1=connecting, 2=connected)
 */
public class WfdManager {

    private static final String TAG = "MiracastEnabler/Wfd";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private final DisplayManager displayManager;

    public WfdManager(Context context) {
        displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    public void startScan() {
        try {
            Method m = DisplayManager.class.getMethod("startWifiDisplayScan");
            m.invoke(displayManager);
        } catch (Throwable t) {
            Log.e(TAG, "startWifiDisplayScan failed", t);
        }
    }

    public void stopScan() {
        try {
            Method m = DisplayManager.class.getMethod("stopWifiDisplayScan");
            m.invoke(displayManager);
        } catch (Throwable t) {
            Log.e(TAG, "stopWifiDisplayScan failed", t);
        }
    }

    public void connect(String address) {
        try {
            Method m = DisplayManager.class.getMethod("connectWifiDisplay", String.class);
            m.invoke(displayManager, address);
        } catch (Throwable t) {
            Log.e(TAG, "connectWifiDisplay failed", t);
        }
    }

    public void disconnect() {
        try {
            Method m = DisplayManager.class.getMethod("disconnectWifiDisplay");
            m.invoke(displayManager);
        } catch (Throwable t) {
            Log.e(TAG, "disconnectWifiDisplay failed", t);
        }
    }

    /**
     * Returns the current WFD connection state:
     * STATE_DISCONNECTED (0), STATE_CONNECTING (1), or STATE_CONNECTED (2)
     */
    public int getConnectionState() {
        try {
            Object status = getWifiDisplayStatus();
            if (status == null) return STATE_DISCONNECTED;
            Method m = status.getClass().getMethod("getActiveDisplayState");
            return (int) m.invoke(status);
        } catch (Throwable t) {
            Log.e(TAG, "getConnectionState failed", t);
            return STATE_DISCONNECTED;
        }
    }

    /**
     * Returns the friendly name of the currently connected display, or null.
     */
    public String getActiveDisplayName() {
        try {
            Object status = getWifiDisplayStatus();
            if (status == null) return null;
            Method getActive = status.getClass().getMethod("getActiveDisplay");
            Object display = getActive.invoke(status);
            if (display == null) return null;
            Method getName = display.getClass().getMethod("getFriendlyDisplayName");
            return (String) getName.invoke(display);
        } catch (Throwable t) {
            Log.e(TAG, "getActiveDisplayName failed", t);
            return null;
        }
    }

    /**
     * Returns an array of available WifiDisplay objects, or null.
     */
    public Object[] getAvailableDisplays() {
        try {
            Object status = getWifiDisplayStatus();
            if (status == null) return null;
            Method m = status.getClass().getMethod("getAvailableDisplays");
            return (Object[]) m.invoke(status);
        } catch (Throwable t) {
            Log.e(TAG, "getAvailableDisplays failed", t);
            return null;
        }
    }

    /**
     * Get the device address from a WifiDisplay object.
     */
    public static String getDisplayAddress(Object wifiDisplay) {
        try {
            Method m = wifiDisplay.getClass().getMethod("getDeviceAddress");
            return (String) m.invoke(wifiDisplay);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Get the friendly name from a WifiDisplay object.
     */
    public static String getDisplayName(Object wifiDisplay) {
        try {
            Method m = wifiDisplay.getClass().getMethod("getFriendlyDisplayName");
            return (String) m.invoke(wifiDisplay);
        } catch (Throwable t) {
            return null;
        }
    }

    private Object getWifiDisplayStatus() {
        try {
            Method m = DisplayManager.class.getMethod("getWifiDisplayStatus");
            return m.invoke(displayManager);
        } catch (Throwable t) {
            Log.e(TAG, "getWifiDisplayStatus failed", t);
            return null;
        }
    }
}
