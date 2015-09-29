package io.github.xsocks.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import io.github.xsocks.BuildConfig;
import io.github.xsocks.R;
import io.github.xsocks.aidl.Config;
import io.github.xsocks.aidl.IXsocksService;
import io.github.xsocks.aidl.IXsocksServiceCallback;
import io.github.xsocks.ui.XsocksRunnerActivity;
import io.github.xsocks.utils.ConfigUtils;
import io.github.xsocks.utils.Constants;
import io.github.xsocks.utils.Utils;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

public class XsocksVpnService extends VpnService {

    private String TAG = "XSOCKS";
    private static final String VPN_ADDRESS = "26.26.26.1";
    int forwarderPort = 5533;
    int pdnsdPort = 1053;

    private Config config = null;
    private ParcelFileDescriptor vpnInterface;

    private BroadcastReceiver receiver = null;
    private Constants.State state = Constants.State.INIT;
    private int callbackCount = 0;
    private final RemoteCallbackList<IXsocksServiceCallback> callbacks = new RemoteCallbackList<>();

    private IXsocksService.Stub binder = new IXsocksService.Stub() {
        @Override
        public int getState() throws RemoteException {
            return state.ordinal();
        }

        @Override
        public void registerCallback(IXsocksServiceCallback cb) throws RemoteException {
            if (cb != null) {
                callbacks.register(cb);
                callbackCount += 1;
            }
        }

        @Override
        public void unregisterCallback(IXsocksServiceCallback cb) throws RemoteException {
            if (cb != null ) {
                callbacks.unregister(cb);
                callbackCount -= 1;
            }
            if (callbackCount == 0 && state != Constants.State.CONNECTING && state != Constants.State.CONNECTED) {
                stopSelf();
            }
        }

        @Override
        public void start(Config config) {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                startRunner(config);
            }
        }

        @Override
        public void stop() throws RemoteException {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                stopRunner();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (VpnService.SERVICE_INTERFACE.equals(action)) {
            return super.onBind(intent);
        } else if (Constants.Action.SERVICE.equals(action)) {
            return binder;
        }
        return null;
    }

    @Override
    public void onRevoke() {
        stopRunner();
    }

    private void killProcesses() {
        for (String task : Constants.executables) {
            try {
                String content = Utils.readFromFile(String.format(Locale.ENGLISH, "%s.pid", task));
                if (!content.isEmpty()) {
                    int pid = Integer.parseInt(content);
                    android.os.Process.killProcess(pid);
                }
            } catch (Exception e) {
                Log.e(TAG, "unable to kill " + task);
            }
        }
    }

    private void startXsocksDaemon() {
        String cmd = String.format("%sxsocks -s %s:%d -k %s -p %sxsocks.pid",
                Constants.Path.BASE, config.proxy, config.remotePort, config.sitekey, Constants.Path.BASE);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, cmd);
        }
        io.github.xsocks.System.exec(cmd);
    }

    private void startDnsTunnel() {
        String cmd = String.format("%sxforwarder -l 0.0.0.0:%d -d 8.8.8.8:53 "
                        + "-s %s:%d "
                        + "-k %s "
                        + "-p %sxforwarder.pid",
                Constants.Path.BASE, forwarderPort, config.proxy,
                config.remotePort, config.sitekey, Constants.Path.BASE);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, cmd);
        }
        io.github.xsocks.System.exec(cmd);
    }

    private void startDnsDaemon() {
        String reject = ConfigUtils.getRejectList(getBaseContext());
        String blackList = ConfigUtils.getBlackList(getBaseContext());
        String conf = String.format(Locale.ENGLISH, getResources().getString(R.string.pdnsd_conf),
                pdnsdPort, reject, blackList, forwarderPort);
        ConfigUtils.printToFile(new File(Constants.Path.BASE + "pdnsd.conf"), conf);
        String cmd = Constants.Path.BASE + "pdnsd -c " + Constants.Path.BASE + "pdnsd.conf";
        if (BuildConfig.DEBUG) {
            Log.d(TAG, cmd);
        }
        io.github.xsocks.System.exec(cmd);
    }

    private boolean isBypass(SubnetUtils net) {
        SubnetUtils.SubnetInfo info = net.getInfo();
        return info.isInRange(config.proxy);
    }

    private boolean isPrivateA(int a) {
        return a == 10 || a == 192 || a == 172;
    }

    private boolean isPrivateB(int a, int b) {
        return a == 10 || (a == 192 && b == 168) || (a == 172 && b >= 16 && b < 32);
    }

    private void route_all(Builder builder) {
        for (int i = 1; i <= 223; i++){
            if (i != 26 && i != 127) {
                String addr = Integer.toString(i) + ".0.0.0";
                String cidr = addr + "/8";
                SubnetUtils net = new SubnetUtils(cidr);
                if (!isBypass(net)) {
                    builder.addRoute(addr, 8);
                } else {
                    for (int j = 0; j <= 255; j++){
                        String subAddr = Integer.toString(i) + "." + Integer.toString(j) + ".0.0";
                        String subCidr = subAddr + "/16";
                        SubnetUtils subNet = new SubnetUtils(subCidr);
                        if (!isBypass(subNet)) {
                            builder.addRoute(subAddr, 16);
                        }
                    }
                }
            }
        }
    }

    private void route_bypass_lan(Builder builder) {
        for (int i = 1; i <= 223; i++) {
            if (i != 26 && i != 127) {
                String addr = Integer.toString(i) + ".0.0.0";
                String cidr = addr + "/8";
                SubnetUtils net = new SubnetUtils(cidr);

                if (!isBypass(net) && !isPrivateA(i)) {
                    builder.addRoute(addr, 8);
                } else {
                    for (int j = 0; j <= 255; j++){
                        String subAddr = Integer.toString(i) + "." + Integer.toString(j) + ".0.0";
                        String subCidr = subAddr + "/16";
                        SubnetUtils subNet = new SubnetUtils(subCidr);
                        if (!isBypass(subNet) && !isPrivateB(i, j)) {
                            builder.addRoute(subAddr, 16);
                        }
                    }
                }
            }
        }
    }

    private void route_bypass_chn(Builder builder) {
        String[] routes;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            routes = getResources().getStringArray(R.array.simple_route);
        } else {
            routes = getResources().getStringArray(R.array.gfw_route);
        }
        for (String cidr : routes) {
            SubnetUtils net = new SubnetUtils(cidr);
            if (!isBypass(net)) {
                String[] addr = cidr.split("/");
                builder.addRoute(addr[0], Integer.parseInt(addr[1]));
            }
        }
    }

    private void route_bypass_internal(Builder builder) {
        String line;
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(this.getResources().openRawResource(R.raw.internal)));
        try {
            while ((line = reader.readLine()) != null) {
                final String[] route = line.split("/");
                if (route.length == 2) {
                    builder.addRoute(route[0], Integer.parseInt(route[1]));
                }
            }
        } catch (final Throwable t) {
            Log.e(TAG, "", t);
        } finally {
            try {
                reader.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }
    }

    private void route_bypass(Builder builder) {
        String line;
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(this.getResources().openRawResource(R.raw.bypass)));

        try {
            while ((line = reader.readLine()) != null) {
                final String[] route = line.split("/");
                if (route.length == 2) {
                    builder.addRoute(route[0], Integer.parseInt(route[1]));
                }
            }
        } catch (final Throwable t) {
            Log.e(TAG, "", t);
        } finally {
            try {
                reader.close();
            } catch (final IOException pIOException) {
                // ignore
            }
        }
    }

    private void startVpn(){
        //android.os.Debug.waitForDebugger();
        int VPN_MTU = 1500;
        Builder builder = new Builder();
        builder.setSession(config.profileName);
        builder.setMtu(VPN_MTU);
        builder.addAddress(VPN_ADDRESS, 24);
        builder.addDnsServer("8.8.8.8");
        //builder.addDnsServer("8.8.4.4");

        if (Utils.isLollipopOrAbove()) {
            builder.allowFamily(android.system.OsConstants.AF_INET6);
            try {
                String packageName = getPackageName();
                builder.addDisallowedApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package name not found");
            }
        }

/*        if (Utils.isLollipopOrAbove() && config.route.equals(Constants.Route.ALL)) {
            builder.addRoute("0.0.0.0", 0); // Interact

        } else {
            switch (config.route) {
                case Constants.Route.ALL:
                    route_all(builder);
                    break;

                case Constants.Route.BYPASS_LAN:
                    route_bypass_lan(builder);
                    break;

                case Constants.Route.BYPASS_CHN:
                    route_bypass_chn(builder);
                    break;

                default:
                    break;
            }
        }*/

/*        if (config.route.equals(Constants.Route.ALL)) {
            builder.addRoute("0.0.0.0", 0);

        } else {
            route_bypass(builder);
        }*/

        switch (config.route) {
            case Constants.Route.ALL:
                builder.addRoute("0.0.0.0", 0);
                break;

            case Constants.Route.BYPASS_LAN:
                route_bypass_lan(builder);
                break;

            case Constants.Route.BYPASS_CHN:
                route_bypass_internal(builder);
                break;

            default:
                break;
        }

        builder.addRoute("8.8.0.0", 16);

        vpnInterface = builder.establish();
        if (vpnInterface == null) {
            Log.e(TAG, "vpn interface is null");
        }

        int fd = vpnInterface.getFd();
        String cmd = String.format("%stun2socks --netif-ipaddr %s "
                        + "--netif-netmask 255.255.255.0 "
                        + "--socks-server-addr 127.0.0.1:%d "
                        + "--tunfd %d "
                        + "--tunmtu %d "
                        + "--loglevel 4 "
                        + "--pid %stun2socks.pid",
                Constants.Path.BASE, "26.26.26.2", config.localPort, fd, VPN_MTU, Constants.Path.BASE);

        if (config.isUdpDns) {
            cmd += " --enable-udprelay";
        } else {
            cmd += " --dnsgw 26.26.26.1:" + Integer.toString(pdnsdPort);
        }

        if (Utils.isLollipopOrAbove()) {
            cmd += " --fake-proc";
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, cmd);
        }

        io.github.xsocks.System.exec(cmd);
    }

    private boolean startDaemons() {
        startXsocksDaemon();
        if (!config.isUdpDns) {
            startDnsDaemon();
            startDnsTunnel();
        }
        startVpn();
        return true;
    }

    private void startRunner(Config c) {
        config = c;
        // ensure the VPNService is prepared
        if (VpnService.prepare(this) != null) {
            Intent i = new Intent(this, XsocksRunnerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }

        // register close receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopRunner();
            }
        };
        registerReceiver(receiver, filter);

        changeState(Constants.State.CONNECTING);

        Async.runAsync(Schedulers.newThread(), (observer, subscription) -> {
            if (config != null) {
                killProcesses();

                boolean resolved = false;
                if (!InetAddressUtils.isIPv4Address(config.proxy) &&
                        !InetAddressUtils.isIPv6Address(config.proxy)) {
                    String addr = Utils.resolve(config.proxy, true);
                    if (addr != null) {
                        config.proxy = addr;
                        resolved = true;
                    }
                } else {
                    resolved = true;
                }

                if (resolved && startDaemons()) {
                    changeState(Constants.State.CONNECTED);
                } else {
                    changeState(Constants.State.STOPPED, getString(R.string.service_failed));
                    stopRunner();
                }
            }
        });
    }

    private void stopRunner() {
        changeState(Constants.State.STOPPING);

        killProcesses();

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                // close failed
            }
        }

        // stop the service if no callback registered
        if (callbackCount == 0) {
            stopSelf();
        }

        // clean up the context
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }

        changeState(Constants.State.STOPPED);
    }

    private void changeState(Constants.State s) {
        changeState(s, null);
    }

    private void changeState(Constants.State s, String msg) {
        Handler handler = new Handler(getBaseContext().getMainLooper());
        handler.post(() -> {
            if (state != s) {
                if (callbackCount > 0) {
                    int n = callbacks.beginBroadcast();
                    for (int i = 0; i <= n - 1; i++) {
                        try {
                            callbacks.getBroadcastItem(i).stateChanged(s.ordinal(), msg);
                        } catch (RemoteException e) {
                            // Ignore
                        }
                    }
                    callbacks.finishBroadcast();
                }
                state = s;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

}
