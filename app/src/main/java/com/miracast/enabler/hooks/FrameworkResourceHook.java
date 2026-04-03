package com.miracast.enabler.hooks;

import android.content.res.Resources;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks Resources.getBoolean() globally (in zygote) so that every process —
 * including system_server, Settings, and SystemUI — sees
 * config_enableWifiDisplay = true and
 * config_wifiDisplaySupportsProtectedBuffers = true.
 *
 * This is the primary mechanism: WifiDisplayAdapter in DisplayManagerService
 * checks these resources at startup to decide whether to register the WFD
 * display adapter at all.
 */
public class FrameworkResourceHook {

    private static final String TAG = "MiracastEnabler/Resource";

    public static void hookZygote() {
        try {
            XposedHelpers.findAndHookMethod(
                Resources.class,
                "getBoolean",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        int resId = (int) param.args[0];
                        try {
                            Resources res = (Resources) param.thisObject;
                            String name = res.getResourceEntryName(resId);
                            if ("config_enableWifiDisplay".equals(name)) {
                                param.setResult(true);
                            } else if ("config_wifiDisplaySupportsProtectedBuffers".equals(name)) {
                                param.setResult(true);
                            }
                        } catch (Resources.NotFoundException ignored) {
                        }
                    }
                }
            );
            XposedBridge.log(TAG + ": hooked Resources.getBoolean");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Resources.getBoolean");
            XposedBridge.log(t);
        }
    }
}
