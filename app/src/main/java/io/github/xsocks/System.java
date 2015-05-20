package io.github.xsocks;

public class System {
    static {
        java.lang.System.loadLibrary("system");
    }

    public static native void exec(String cmd);
    public static native String getABI();
}
