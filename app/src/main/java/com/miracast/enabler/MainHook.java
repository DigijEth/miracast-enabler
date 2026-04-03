package com.miracast.enabler;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import com.miracast.enabler.hooks.FrameworkResourceHook;
import com.miracast.enabler.hooks.DisplayManagerHook;
import com.miracast.enabler.hooks.SystemServerHook;
import com.miracast.enabler.hooks.MediaRouterHook;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "MiracastEnabler";

    @Override
    public void initZygote(StartupParam startupParam) {
        XposedBridge.log(TAG + ": initZygote");
        FrameworkResourceHook.hookZygote();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        switch (lpparam.packageName) {
            case "android":
                XposedBridge.log(TAG + ": hooking system_server");
                SystemServerHook.hook(lpparam);
                DisplayManagerHook.hook(lpparam);
                break;
            case "com.android.settings":
                XposedBridge.log(TAG + ": hooking Settings");
                MediaRouterHook.hookSettings(lpparam);
                break;
        }
    }
}
