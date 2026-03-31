package de.robv.android.xposed;

public class XposedHelpers {
    public static void findAndHookMethod(String className, ClassLoader classLoader,
            String methodName, Object... parameterTypesAndCallback) {}

    public static void findAndHookMethod(Class<?> clazz,
            String methodName, Object... parameterTypesAndCallback) {}

    public static Object callMethod(Object obj, String methodName, Object... args) { return null; }
    public static Object getObjectField(Object obj, String fieldName) { return null; }
    public static void setObjectField(Object obj, String fieldName, Object value) {}
    public static int getIntField(Object obj, String fieldName) { return 0; }
    public static void setIntField(Object obj, String fieldName, int value) {}
    public static boolean getBooleanField(Object obj, String fieldName) { return false; }
    public static void setBooleanField(Object obj, String fieldName, boolean value) {}
}
