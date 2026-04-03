package com.miracast.enabler.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks in the Settings app to make WFD (Miracast) sinks visible
 * in the Cast / Connected Devices UI.
 *
 * Android Settings checks WifiDisplayStatus.getFeatureState() to decide
 * whether to show the Wi-Fi Display settings section. We force it to
 * FEATURE_STATE_ON so the WFD option appears.
 *
 * We also hook in Settings to ensure the WifiDisplaySettings fragment
 * is reachable and not filtered out.
 */
public class MediaRouterHook {

    private static final String TAG = "MiracastEnabler/Router";

    public static void hookSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        hookFeatureStateInSettings(lpparam);
        hookWifiDisplaySettings(lpparam);
    }

    /**
     * In the Settings process, hook WifiDisplayStatus.getFeatureState()
     * to return FEATURE_STATE_ON. This makes the Wi-Fi Display section
     * appear in Cast preferences.
     */
    private static void hookFeatureStateInSettings(XC_LoadPackage.LoadPackageParam lpparam) {
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
            XposedBridge.log(TAG + ": hooked getFeatureState in Settings");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook getFeatureState in Settings");
            XposedBridge.log(t);
        }
    }

    /**
     * Try to hook the WifiDisplaySettings fragment to ensure it initializes
     * even if the Settings app tries to hide it based on device config.
     */
    private static void hookWifiDisplaySettings(XC_LoadPackage.LoadPackageParam lpparam) {
        // On AOSP Settings, WifiDisplaySettings is a PreferenceFragment that
        // checks isAvailable() based on the feature state. Since we already
        // hook getFeatureState, this should work. But some OEMs override
        // the availability check separately.
        String[] possibleClasses = {
            "com.android.settings.wfd.WifiDisplaySettings",
            "com.android.settings.display.WifiDisplaySettings",
            "com.android.settings.connecteddevice.WifiDisplaySettings",
        };

        for (String className : possibleClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "isAvailable",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    }
                );
                XposedBridge.log(TAG + ": hooked " + className + ".isAvailable()");
                return;
            } catch (Throwable ignored) {
            }
        }

        // Also try hooking the preference controller that gates WFD visibility
        String[] controllerClasses = {
            "com.android.settings.wfd.WifiDisplayPreferenceController",
            "com.android.settings.display.WifiDisplayPreferenceController",
        };

        for (String className : controllerClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "getAvailabilityStatus",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(0); // AVAILABLE = 0
                        }
                    }
                );
                XposedBridge.log(TAG + ": hooked " + className + ".getAvailabilityStatus()");
                return;
            } catch (Throwable ignored) {
            }
        }

        XposedBridge.log(TAG + ": no WifiDisplaySettings class found to hook (non-fatal)");
    }
}
