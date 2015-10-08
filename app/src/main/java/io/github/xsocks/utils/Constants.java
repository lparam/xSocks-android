package io.github.xsocks.utils;

public class Constants {
    public class Route {
        public static final String ALL = "all";
        public static final String BYPASS_LAN = "bypass-lan";
        public static final String BYPASS_CHN = "bypass-china";
    }

    public enum State {
        INIT,
        CONNECTING,
        CONNECTED,
        STOPPING,
        STOPPED;

        public static boolean isAvailable(int state) {
            return state != CONNECTED.ordinal() && state != CONNECTING.ordinal();
        }
    }

    public static String[] executables = {"xsocks", "xforwarder", "pdnsd", "tun2socks"};

    public class Path {
        public static final String BASE = "/data/data/io.github.xsocks/";
    }

    public class Action {
        public static final String SERVICE = "io.github.xsocks.SERVICE";
        public static final String CLOSE = "io.github.xsocks.CLOSE";
        public static final String UPDATE_PREFS = "io.github.xsocks.ACTION_UPDATE_PREFS";
    }

    public class Key {
        public static final String profileId = "profileId";
        public static final String profileName = "profileName";

        public static final String proxied = "Proxyed";

        public static final String status = "status";
        public static final String proxyedApps = "proxyedApps";
        public static final String route = "route";

        public static final String isRunning = "isRunning";
        public static final String isAutoConnect = "isAutoConnect";

        public static final String isGlobalProxy = "isGlobalProxy";
        public static final String isBypassApps = "isBypassApps";
        public static final String isUdpDns = "isUdpDns";

        public static final String proxy = "proxy";
        public static final String sitekey = "sitekey";
        public static final String remotePort = "remotePort";
        public static final String localPort = "port";
    }

    public class Scheme {
        public static final String APP = "app://";
    }

}

