package org.mozilla.javascript;

class ContextHolder {
    private static final ThreadLocal<Context> contextLocal = new ThreadLocal<>();

    static void setContext(Context context) {
        contextLocal.set(context);
    }

    static Context getContext() {
        return contextLocal.get();
    }

    public static void clearContext() {
        contextLocal.remove();
    }
}
