package io.github.xsocks.utils;

import java.util.ArrayList;

import eu.chainfire.libsuperuser.Shell;

public class Console {

    private static Shell.Interactive openShell() {
        Shell.Builder builder = new Shell.Builder();
        return builder.useSH().setWatchdogTimeout(10).open();
    }

    public static void runCommand(String command) {
        ArrayList<String> sb = new ArrayList<>();
        sb.add(command);
        String[] commands = new String[sb.size()];
        sb.toArray(commands);
        runCommand(commands);
    }

    public static String runCommand(String[] commands) {
        Shell.Interactive shell = openShell();
        StringBuilder sb = new StringBuilder();
        shell.addCommand(commands, 0, (commandCode, exitCode, output) -> {
            if (exitCode < 0) {
                shell.close();
            } else {
                for (String line : output) {
                    sb.append(line).append('\n');
                }
            }
        });
        if (shell.waitForIdle()) {
            shell.close();
            return sb.toString();
        }
        else {
            shell.close();
            return null;
        }
    }
}
