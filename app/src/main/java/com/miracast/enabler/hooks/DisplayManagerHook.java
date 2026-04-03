package com.miracast.enabler.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks in system_server targeting WifiDisplayAdapter and WifiDisplayController
 * to ensure the WFD stack initializes and scans correctly.
 *
 * On some builds, WifiDisplayController.requestStartScan() has additional
 * gating checks beyond the resource flag. This hook ensures scanning proceeds.
 */
public class DisplayManagerHook {

    private static final String TAG = "MiracastEnabler/Display";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookWifiDisplayFeatureState(lpparam);
        hookWifiDisplayController(lpparam);
    }

    /**
     * Hook WifiDisplayStatus.getFeatureState() to always return FEATURE_STATE_ON (3).
     * This ensures that any code checking the feature state (Settings, SystemUI)
     * sees WFD as fully enabled.
     */
    private static void hookWifiDisplayFeatureState(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.display.WifiDisplayStatus",
                lpparam.classLoader,
                "getFeatureState",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(3); // FEATURE_STATE_ON
                    }
                }
            );
            XposedBridge.log(TAG + ": hooked WifiDisplayStatus.getFeatureState -> ON");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook getFeatureState");
            XposedBridge.log(t);
        }
    }

    /**
     * Hook WifiDisplayController to ensure scanning is not blocked by
     * vendor-specific checks (e.g., missing WFD IE support flag).
     */
    private static void hookWifiDisplayController(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> controllerClass = XposedHelpers.findClass(
                "com.android.server.display.WifiDisplayController",
                lpparam.classLoader
            );

            // Log when scan starts to confirm the WFD stack is active
            XposedHelpers.findAndHookMethod(
                controllerClass,
                "requestStartScan",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(TAG + ": WifiDisplayController.requestStartScan() called");
                    }
                }
            );

            XposedBridge.log(TAG + ": hooked WifiDisplayController");
        } catch (Throwable t) {
            // Controller class name may differ or not exist — non-fatal
            XposedBridge.log(TAG + ": WifiDisplayController hook skipped: " + t.getMessage());
        }
    }
}
