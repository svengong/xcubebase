package org.xtgo.xcube.base;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class XcubeBase implements IXposedHookLoadPackage {
    public static String TAG = "Xcube_xposed";
    private static String configPath = "/data/local/tmp/xcube/xcube.yaml";
    public static boolean hooked = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String remoteName = Utils.getRemoteName();
        XcubeConfig config = new XcubeConfig(configPath);
        if (!config.active || !config.contains(loadPackageParam.packageName)) {
            return;
        }
        String script = config.getScriptPath(loadPackageParam.packageName);
        Log.e(TAG, "current app packageName : " + loadPackageParam.packageName + ":" + remoteName);

        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.d(TAG, "attach beforeHookedMethod script:" + script);
                if (hooked || !remoteName.isEmpty()) {
                    //只hook主进程一次
                    return;
                }
                try {
                    Context base = (Context) param.args[0];
                    File toPath = base.getDir("libs", Context.MODE_PRIVATE);
                    String libpath = "/data/local/tmp/xcube/";
                    String ABI = android.os.Process.is64Bit() ? "arm64-v8a" : "armeabi-v7a";
                    //System.loadlibrary使用的classloader是当前classloader，而param.args[0]是目标应用的classloader，二者不同
                    Log.d(TAG, "attach beforeHookedMethod toPath:" + toPath);
                    LoadLibraryUtil.loadSoFile(this.getClass().getClassLoader(), libpath + ABI, toPath);
                    System.loadLibrary("xcubebase");
                    Log.d(TAG, "script path :" + script);
                    gumjsHook(script);
                    hooked = true;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }


        });
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ((Activity) param.thisObject).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    public static class Utils {
        public static String getRemoteName() {
            try {
                File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
                BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
                String processName = mBufferedReader.readLine().trim();
                int index = processName.indexOf(':');
                String romote = "";
                if (index > -1) {
                    romote = processName.substring(index + 1);
                }
                mBufferedReader.close();
                return romote;
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        public static ArrayList<String> getPackages(String path) {
            // Construct BufferedReader from FileReader
            File f = new File(path);
            BufferedReader br = null;
            ArrayList<String> ret = new ArrayList<String>();
            try {
                br = new BufferedReader(new FileReader(f));
                String line = null;
                while ((line = br.readLine()) != null) {
                    ret.add(line);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ret;
        }

        public static String readFile(File file) {
            StringBuilder builder = new StringBuilder();
            try {
                FileReader fr = new FileReader(file);
                int ch = 0;
                while ((ch = fr.read()) != -1) {
                    builder.append((char) ch);
                }
            } catch (IOException e) {
                Log.e(XcubeBase.TAG, "FileNotFoundException" + e.getMessage());
                e.printStackTrace();
            }
            return builder.toString();
        }
    }

    public static class LoadLibraryUtil {
        private static final String TAG = "Xcube_xposed ";
        private static File lastSoDir = null;


        /**
         * 清除so路径，实际上是设置一个无效的path，用户测试无so库的情况
         *
         * @param classLoader
         */
        public static void clearSoPath(ClassLoader classLoader) {
            try {
                final String testDirNoSo = Environment.getExternalStorageDirectory().getAbsolutePath() + "/duqian/";
                new File(testDirNoSo).mkdirs();
                installNativeLibraryPath(classLoader, testDirNoSo);
            } catch (Throwable throwable) {
                Log.e(TAG, "dq clear path error" + throwable.toString());
                throwable.printStackTrace();
            }
        }

        public static synchronized boolean installNativeLibraryPath(ClassLoader classLoader, String folderPath) throws Throwable {
            return installNativeLibraryPath(classLoader, new File(folderPath));
        }

        public static synchronized boolean installNativeLibraryPath(ClassLoader classLoader, File folder)
                throws Throwable {
            if (classLoader == null || folder == null || !folder.exists()) {
                Log.e(TAG, "classLoader or folder is illegal " + folder);
                return false;
            }
            final int sdkInt = Build.VERSION.SDK_INT;
            final boolean aboveM = (sdkInt == 25 && getPreviousSdkInt() != 0) || sdkInt > 25;
            if (aboveM) {
                try {
                    V25.install(classLoader, folder);
                } catch (Throwable throwable) {
                    try {
                        V23.install(classLoader, folder);
                    } catch (Throwable throwable1) {
                        V14.install(classLoader, folder);
                    }
                }
            } else if (sdkInt >= 23) {
                try {
                    V23.install(classLoader, folder);
                } catch (Throwable throwable) {
                    V14.install(classLoader, folder);
                }
            } else if (sdkInt >= 14) {
                V14.install(classLoader, folder);
            }
            lastSoDir = folder;
            return true;
        }

        private static final class V23 {
            private static void install(ClassLoader classLoader, File folder) throws Throwable {
                Field pathListField = ReflectUtil.findField(classLoader, "pathList");
                Object dexPathList = pathListField.get(classLoader);

                Field nativeLibraryDirectories = ReflectUtil.findField(dexPathList, "nativeLibraryDirectories");
                List<File> libDirs = (List<File>) nativeLibraryDirectories.get(dexPathList);

                //去重
                if (libDirs == null) {
                    libDirs = new ArrayList<>(2);
                }
                final Iterator<File> libDirIt = libDirs.iterator();
                while (libDirIt.hasNext()) {
                    final File libDir = libDirIt.next();
                    if (folder.equals(libDir) || folder.equals(lastSoDir)) {
                        libDirIt.remove();
                        Log.d(TAG, "dq libDirIt.remove() " + folder.getAbsolutePath());
                        break;
                    }
                }

                libDirs.add(0, folder);
                Field systemNativeLibraryDirectories =
                        ReflectUtil.findField(dexPathList, "systemNativeLibraryDirectories");
                List<File> systemLibDirs = (List<File>) systemNativeLibraryDirectories.get(dexPathList);

                //判空
                if (systemLibDirs == null) {
                    systemLibDirs = new ArrayList<>(2);
                }
                Log.d(TAG, "dq systemLibDirs,size=" + systemLibDirs.size());

                Method makePathElements = ReflectUtil.findMethod(dexPathList, "makePathElements", List.class, File.class, List.class);
                ArrayList<IOException> suppressedExceptions = new ArrayList<>();
                libDirs.addAll(systemLibDirs);

                Object[] elements = (Object[]) makePathElements.invoke(dexPathList, libDirs, null, suppressedExceptions);
                Field nativeLibraryPathElements = ReflectUtil.findField(dexPathList, "nativeLibraryPathElements");
                nativeLibraryPathElements.setAccessible(true);
                nativeLibraryPathElements.set(dexPathList, elements);
            }
        }

        /**
         * 把自定义的native库path插入nativeLibraryDirectories最前面，即使安装包libs目录里面有同名的so，也优先加载指定路径的外部so
         */
        private static final class V25 {
            private static void install(ClassLoader classLoader, File folder) throws Throwable {
                Field pathListField = ReflectUtil.findField(classLoader, "pathList");
                Object dexPathList = pathListField.get(classLoader);
                Field nativeLibraryDirectories = ReflectUtil.findField(dexPathList, "nativeLibraryDirectories");

                List<File> libDirs = (List<File>) nativeLibraryDirectories.get(dexPathList);
                //去重
                if (libDirs == null) {
                    libDirs = new ArrayList<>(2);
                }
                final Iterator<File> libDirIt = libDirs.iterator();
                while (libDirIt.hasNext()) {
                    final File libDir = libDirIt.next();
                    if (folder.equals(libDir) || folder.equals(lastSoDir)) {
                        libDirIt.remove();
                        Log.d(TAG, "dq libDirIt.remove()" + folder.getAbsolutePath());
                        break;
                    }
                }
                libDirs.add(0, folder);
                //system/lib
                Field systemNativeLibraryDirectories = ReflectUtil.findField(dexPathList, "systemNativeLibraryDirectories");
                List<File> systemLibDirs = (List<File>) systemNativeLibraryDirectories.get(dexPathList);

                //判空
                if (systemLibDirs == null) {
                    Log.d(TAG, "systemLibDirs == null");
                    systemLibDirs = new ArrayList<>(2);
                }

                Method makePathElements = ReflectUtil.findMethod(dexPathList, "makePathElements", List.class);
                libDirs.addAll(systemLibDirs);

                Object[] elements = (Object[]) makePathElements.invoke(dexPathList, libDirs);
                Field nativeLibraryPathElements = ReflectUtil.findField(dexPathList, "nativeLibraryPathElements");
                nativeLibraryPathElements.setAccessible(true);
                nativeLibraryPathElements.set(dexPathList, elements);
            }
        }


        private static final class V14 {
            private static void install(ClassLoader classLoader, File folder) throws Throwable {
                Field pathListField = ReflectUtil.findField(classLoader, "pathList");
                Object dexPathList = pathListField.get(classLoader);

                ReflectUtil.expandFieldArray(dexPathList, "nativeLibraryDirectories", new File[]{folder});
            }
        }

        /**
         * fuck部分机型删了该成员属性，兼容
         *
         * @return 被厂家删了返回1，否则正常读取
         */
        @TargetApi(Build.VERSION_CODES.M)
        private static int getPreviousSdkInt() {
            try {
                return Build.VERSION.PREVIEW_SDK_INT;
            } catch (Throwable ignore) {
            }
            return 1;
        }

        public static void loadSoFile(Context context, String fromPath) {
            String soname = "libhellofrida.so";
            try {
                File dirs = context.getDir("libs", Context.MODE_PRIVATE);
                Log.d(TAG, "loadSoFile libs : " + dirs);
                if (!isLoadSoFiles(soname, dirs)) {
                    copyFile(fromPath, dirs.getAbsolutePath());
                } else {
                    File so = new File(dirs.getAbsolutePath() + File.separator + soname);
                    so.delete();
                    copyFile(fromPath, dirs.getAbsolutePath());
                }
                installNativeLibraryPath(context.getClassLoader(), dirs);
            } catch (Throwable throwable) {
                Log.e("loadSoFile", "loadSoFile error " + throwable.getMessage());
            }
        }

        public static void loadSoFile(ClassLoader classLoader, String fromPath, File toPath) {
            try {
                File dirs = toPath;
                Log.d(TAG, "copy libs and config to : " + dirs);
                copyFile(fromPath, dirs.getAbsolutePath());
                installNativeLibraryPath(classLoader, dirs);
            } catch (Throwable throwable) {
                Log.e("loadSoFile", "loadSoFile error " + throwable.getMessage());
            }
        }

        public static void copyConfigFile(String fromFile, String toFile) {
            copySdcardFile(fromFile, toFile);
        }


        /**
         * 判断immqy so 文件是否存在
         *
         * @param name "libimmqy" so库
         * @return boolean
         */
        private static boolean isLoadSoFiles(String name, File dirs) {
            boolean getSoLib = false;
            File[] currentFiles;
            currentFiles = dirs.listFiles();
            if (currentFiles == null) {
                return false;
            }
            for (int i = 0; i < currentFiles.length; i++) {
                if (currentFiles[i].getName().contains(name)) {
                    getSoLib = true;
                }
            }
            return getSoLib;
        }

        /**
         * @param fromFiles 指定的下载目录
         * @param toFile    应用的包路径
         * @return int
         */
        private static int copyFile(String fromFiles, String toFile) {
            //要复制的文件目录
            File[] currentFiles;
            File root = new File(fromFiles);
            //如同判断SD卡是否存在或者文件是否存在,如果不存在则 return出去
            if (!root.exists()) {
                return -1;
            }
            //如果存在则获取当前目录下的全部文件 填充数组
            currentFiles = root.listFiles();
            if (currentFiles == null) {
                Log.d("soFile---", "未获取到文件");
                return -1;
            }
            //目标目录
            File targetDir = new File(toFile);
            //创建目录
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            //遍历要复制该目录下的全部文件
            for (int i = 0; i < currentFiles.length; i++) {
                if (currentFiles[i].isDirectory()) {
                    //如果当前项为子目录 进行递归
                    copyFile(currentFiles[i].getPath() + "/", toFile + currentFiles[i].getName() + "/");
                } else {
                    //如果当前项为文件则进行文件拷贝
                    int id = copySdcardFile(currentFiles[i].getPath(), toFile + File.separator + currentFiles[i].getName());

                }
            }
            return 0;
        }

        /**
         * 要复制的目录下的所有非子目录(文件夹)文件拷贝
         *
         * @param fromFiles 指定的下载目录
         * @param toFile    应用的包路径
         * @return int
         */
        private static int copySdcardFile(String fromFiles, String toFile) {
            Log.d(TAG, "复制文件到" + toFile);
            try {
                FileInputStream fileInput = new FileInputStream(fromFiles);
                FileOutputStream fileOutput = new FileOutputStream(toFile);
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024 * 1];
                int len = -1;
                while ((len = fileInput.read(buffer)) != -1) {
                    byteOut.write(buffer, 0, len);
                }
                // 从内存到写入到具体文件
                fileOutput.write(byteOut.toByteArray());
                // 关闭文件流
                byteOut.close();
                fileOutput.close();
                fileInput.close();
                return 0;
            } catch (Exception ex) {
                return -1;
            }
        }
    }

    public static class ReflectUtil {

        public static Field findField(Object instance, String name) throws NoSuchFieldException {
            for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                try {
                    Field field = clazz.getDeclaredField(name);
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    return field;
                } catch (NoSuchFieldException e) {
                }
            }

            throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
        }

        public static Field findField(Class<?> originClazz, String name) throws NoSuchFieldException {
            for (Class<?> clazz = originClazz; clazz != null; clazz = clazz.getSuperclass()) {
                try {
                    Field field = clazz.getDeclaredField(name);
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    return field;
                } catch (NoSuchFieldException e) {
                }
            }
            throw new NoSuchFieldException("Field " + name + " not found in " + originClazz);
        }

        public static Method findMethod(Object instance, String name, Class<?>... parameterTypes)
                throws NoSuchMethodException {
            for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                try {
                    Method method = clazz.getDeclaredMethod(name, parameterTypes);
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    return method;
                } catch (NoSuchMethodException e) {
                }
            }

            throw new NoSuchMethodException("Method "
                    + name
                    + " with parameters "
                    + Arrays.asList(parameterTypes)
                    + " not found in " + instance.getClass());
        }


        /**
         * 数组替换
         */
        public static void expandFieldArray(Object instance, String fieldName, Object[] extraElements)
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
            Field jlrField = findField(instance, fieldName);

            Object[] original = (Object[]) jlrField.get(instance);
            Object[] combined = (Object[]) Array.newInstance(original.getClass().getComponentType(), original.length + extraElements.length);

            // NOTE: changed to copy extraElements first, for patch load first

            System.arraycopy(extraElements, 0, combined, 0, extraElements.length);
            System.arraycopy(original, 0, combined, extraElements.length, original.length);

            jlrField.set(instance, combined);
        }

        public static void reduceFieldArray(Object instance, String fieldName, int reduceSize)
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
            if (reduceSize <= 0) {
                return;
            }
            Field jlrField = findField(instance, fieldName);
            Object[] original = (Object[]) jlrField.get(instance);
            int finalLength = original.length - reduceSize;
            if (finalLength <= 0) {
                return;
            }
            Object[] combined = (Object[]) Array.newInstance(original.getClass().getComponentType(), finalLength);
            System.arraycopy(original, reduceSize, combined, 0, finalLength);
            jlrField.set(instance, combined);
        }

        public static Constructor<?> findConstructor(Object instance, Class<?>... parameterTypes)
                throws NoSuchMethodException {
            for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                try {
                    Constructor<?> ctor = clazz.getDeclaredConstructor(parameterTypes);
                    if (!ctor.isAccessible()) {
                        ctor.setAccessible(true);
                    }
                    return ctor;
                } catch (NoSuchMethodException e) {
                }
            }
            throw new NoSuchMethodException("Constructor"
                    + " with parameters "
                    + Arrays.asList(parameterTypes)
                    + " not found in " + instance.getClass());
        }

        public static Object getActivityThread(Context context, Class<?> activityThread) {
            try {
                if (activityThread == null) {
                    activityThread = Class.forName("android.app.ActivityThread");
                }
                Method m = activityThread.getMethod("currentActivityThread");
                m.setAccessible(true);
                Object currentActivityThread = m.invoke(null);
                if (currentActivityThread == null && context != null) {
                    Field mLoadedApk = context.getClass().getField("mLoadedApk");
                    mLoadedApk.setAccessible(true);
                    Object apk = mLoadedApk.get(context);
                    Field mActivityThreadField = apk.getClass().getDeclaredField("mActivityThread");
                    mActivityThreadField.setAccessible(true);
                    currentActivityThread = mActivityThreadField.get(apk);
                }
                return currentActivityThread;
            } catch (Throwable ignore) {
                return null;
            }
        }

        public static int getValueOfStaticIntField(Class<?> clazz, String fieldName, int defVal) {
            try {
                final Field field = findField(clazz, fieldName);
                return field.getInt(null);
            } catch (Throwable thr) {
                return defVal;
            }
        }

    }

    public static void printSomeLog() {
        Log.d("Xcube", "printSomeLog");
    }

    public static native void gumjsHook(String script);

    public static native boolean checkConfigured(String jpackagename);

}
