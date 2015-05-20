package io.github.xsocks.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import io.github.xsocks.R;
import io.github.xsocks.aidl.Config;

public class ConfigUtils {

    public static boolean printToFile(java.io.File file, String content) {
        try {
            java.io.PrintWriter printer = new java.io.PrintWriter(file);
            printer.println(content);
            printer.flush();
            return true;
        } catch (Exception ex) {
            Log.e("XSOCKS", ex.getMessage());
            return false;
        }
    }

    public static String getRejectList(Context context) {
        return context.getString(R.string.reject);
    }

    public static String getBlackList(Context context) {
        return context.getString(R.string.black_list);
    }

    public static Config load(SharedPreferences settings) {
        boolean isGlobalProxy = settings.getBoolean(Constants.Key.isGlobalProxy, false);
        boolean isBypassApps = settings.getBoolean(Constants.Key.isBypassApps, false);
        boolean isUdpDns = settings.getBoolean(Constants.Key.isUdpDns, false);

        String profileName = settings.getString(Constants.Key.profileName, "Default");
        String proxy = settings.getString(Constants.Key.proxy, "");
        String sitekey = settings.getString(Constants.Key.sitekey, "");
        String route = settings.getString(Constants.Key.route, "all");

        int remotePort = Integer.parseInt(settings.getString(Constants.Key.remotePort, "1073"));
        int localPort = Integer.parseInt(settings.getString(Constants.Key.localPort, "1080"));

        String proxiedAppString = settings.getString(Constants.Key.proxied, "");

        return new Config(isGlobalProxy, isBypassApps, isUdpDns, profileName, proxy,
                sitekey, proxiedAppString, route, remotePort, localPort);
    }

}
