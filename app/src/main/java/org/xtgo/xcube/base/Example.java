package org.xtgo.xcube.base;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class Example implements IXposedHookLoadPackage {
    private String TAG = "Xcube_xposed";

    private static String YAOYAO_PACKAGE_NAME= "com.cebbank.yaoyao";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        final ClassLoader classLoader = loadPackageParam.classLoader;
        if (loadPackageParam.packageName.equals(YAOYAO_PACKAGE_NAME)) {
            Log.e(TAG, "this is = " + loadPackageParam.packageName);
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws ClassNotFoundException {
                    ClassLoader cl = ((Context) param.args[0]).getClassLoader();
                    Log.e(TAG, cl.toString());

                    final Class<?> hookclass = cl.loadClass("com.magus.MagusLoad");

                    if(hookclass != null){
                        findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                Class<?> cls = (Class<?>) param.getResult();
                                if (cls != null) {
                                    String name = cls.getName();
                                    if ("com.magus.MagusLoad".equals(name)) {
                                        Log.e(TAG, ">>>com.magus.MagusLoad<<<");

                                        findAndHookMethod(cls, "isRoot", new XC_MethodHook() {
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                //XposedBridge.log("开始劫持了[MagusLoad.isRoot()]~");
                                                Log.e(TAG, "开始劫持了[MagusLoad.isRoot()]~");
                                            }

                                            @Override
                                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                                //XposedBridge.log(param.method + " return: " + param.getResult());
                                                Log.e(TAG, param.method + " return: " + param.getResult());
                                                param.setResult(false);
                                            }
                                        });
                                    }
                                }

                            }
                        });
                    }else {
                        Log.e(TAG, "find ok ");
                    }

                    AntiAntiDebug.HookProxy(cl);


                }

            });
        }


    }
}
