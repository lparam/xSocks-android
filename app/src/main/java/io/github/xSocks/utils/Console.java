package io.github.xSocks.utils;

public class Console {

    public static void runCommand(String[] cmds) {
        for (String cmd : cmds) {
            io.github.xSocks.System.exec(cmd);
        }
    }
}
