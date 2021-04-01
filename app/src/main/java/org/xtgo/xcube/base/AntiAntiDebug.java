package org.xtgo.xcube.base;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class AntiAntiDebug {
    private static String TAG = "Xcube_xposed_commonHook";
    public static  void HookProxy(ClassLoader cl){
        findAndHookMethod("android.net.Proxy", cl, "getDefaultHost", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("开始劫持了[MagusLoad.getDefaultHost()]~");
                Log.e(TAG, "开始劫持了[MagusLoad.getDefaultHost()]~");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log(param.method + " return: " + param.getResult());
                Log.e(TAG, param.method + " return: " + param.getResult());
                param.setResult("");
            }
        });

        findAndHookMethod("android.net.Proxy", cl, "getDefaultPort", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("开始劫持了[MagusLoad.getDefaultPort()]~");
                Log.e(TAG, "开始劫持了[MagusLoad.getDefaultPort()]~");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(-1);
            }
        });
    }
}
