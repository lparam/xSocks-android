package io.github.xsocks.utils;

public class Console {

    public static void runCommand(String[] cmds) {
        for (String cmd : cmds) {
            io.github.xsocks.System.exec(cmd);
        }
    }
}
