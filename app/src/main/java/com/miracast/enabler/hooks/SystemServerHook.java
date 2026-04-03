package com.miracast.enabler.hooks;

import android.content.pm.PackageManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks in system_server to:
 * 1. Force hasSystemFeature("android.software.wifi_display") = true
 * 2. Grant CONFIGURE_WIFI_DISPLAY and CONTROL_WIFI_DISPLAY permissions
 *    to our package so the QS tile can control WFD.
 */
public class SystemServerHook {

    private static final String TAG = "MiracastEnabler/System";
    private static final String OUR_PACKAGE = "com.miracast.enabler";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookSystemFeature(lpparam);
        hookPermissions(lpparam);
    }

    private static void hookSystemFeature(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "hasSystemFeature",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String feature = (String) param.args[0];
                        if ("android.software.wifi_display".equals(feature)
                                || "android.hardware.wifi.direct".equals(feature)) {
                            param.setResult(true);
                        }
                    }
                }
            );
            XposedBridge.log(TAG + ": hooked hasSystemFeature");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook hasSystemFeature");
            XposedBridge.log(t);
        }
    }

    private static void hookPermissions(XC_LoadPackage.LoadPackageParam lpparam) {
        // Try multiple permission check paths — the class name varies by Android version
        String[] permClasses = {
            "com.android.server.pm.permission.PermissionManagerServiceImpl",
            "com.android.server.pm.permission.PermissionManagerService",
        };

        for (String className : permClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "checkPermission",
                    String.class, String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String perm = (String) param.args[0];
                            String pkg = (String) param.args[1];
                            if (OUR_PACKAGE.equals(pkg) && isWfdPermission(perm)) {
                                param.setResult(PackageManager.PERMISSION_GRANTED);
                            }
                        }
                    }
                );
                XposedBridge.log(TAG + ": hooked permissions via " + className);
                return;
            } catch (Throwable ignored) {
            }
        }

        // Fallback: hook the UID-based checkUidPermission
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ActivityManager",
                lpparam.classLoader,
                "checkComponentPermission",
                String.class, int.class, int.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String perm = (String) param.args[0];
                        if (perm != null && isWfdPermission(perm)) {
                            param.setResult(PackageManager.PERMISSION_GRANTED);
                        }
                    }
                }
            );
            XposedBridge.log(TAG + ": hooked permissions via ActivityManager fallback");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook permissions");
            XposedBridge.log(t);
        }
    }

    private static boolean isWfdPermission(String perm) {
        return "android.permission.CONFIGURE_WIFI_DISPLAY".equals(perm)
            || "android.permission.CONTROL_WIFI_DISPLAY".equals(perm);
    }
}
