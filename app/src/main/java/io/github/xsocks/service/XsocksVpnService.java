package io.github.xsocks.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Scanner;

import io.github.xsocks.BuildConfig;
import io.github.xsocks.R;
import io.github.xsocks.aidl.Config;
import io.github.xsocks.aidl.IXsocksService;
import io.github.xsocks.aidl.IXsocksServiceCallback;
import io.github.xsocks.ui.MainActivity;
import io.github.xsocks.ui.XsocksRunnerActivity;
import io.github.xsocks.utils.ConfigUtils;
import io.github.xsocks.utils.Constants;
import io.github.xsocks.utils.Utils;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

public class XsocksVpnService extends VpnService {

    private String TAG = "XSOCKS";

    private int pdnsdPort = 1053;
    private int forwarderPort = 5533;
    private static final String VPN_ADDRESS = "26.26.26.1";

    private Config config = null;
    private ParcelFileDescriptor vpnInterface;

    private BroadcastReceiver closeReceiver = null;
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

    private void notifyForegroundAlert(String title, String info, Boolean visible) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, 0);
        Intent closeIntent = new Intent(Constants.Action.CLOSE);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, closeIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setWhen(0)
            .setTicker(title)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(info)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_logo)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.stop), actionIntent);

        if (visible) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }

        startForeground(1, builder.build());
    }

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

    private String readFromRaw(int resId) {
        InputStream in = this.getResources().openRawResource(resId);
        Scanner scanner = new Scanner(in,"UTF-8").useDelimiter("\\A");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.next());
        }
        scanner.close();
        return sb.toString();
    }

    private void startXsocksDaemon() {
        if (!config.route.equals(Constants.Route.ALL)) {
            InputStream in;
            OutputStream out;
            if (config.route.equals(Constants.Route.BYPASS_LAN)) {
                in = this.getResources().openRawResource(R.raw.route_lan);
            } else {
                in = this.getResources().openRawResource(R.raw.route_chn);
            }

            try {
                out = new FileOutputStream(Constants.Path.BASE + "acl.list");
                Utils.copyFile(in, out);
                in.close();
                out.flush();
                out.close();

            } catch (IOException e) {
                Log.e(TAG, "Copy file error: " + e.getMessage());
            }
        }

        String cmd = String.format("%sxsocks -s %s:%d -k %s -p %sxsocks.pid -t 600",
                Constants.Path.BASE, config.proxy, config.remotePort, config.sitekey,
                Constants.Path.BASE);

        if (!config.route.equals(Constants.Route.ALL)) {
            cmd += " --acl ";
            cmd += Constants.Path.BASE + "acl.list";
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, cmd);
        }

        io.github.xsocks.System.exec(cmd);
    }

    private void startDnsForwarder() {
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

    private String getRejectList() {
        return readFromRaw(R.raw.dns_reject_list);
    }

    private String getBlackList() {
        return readFromRaw(R.raw.dns_black_list);
    }

    private void startDnsDaemon() {
        String rejectList = getRejectList();
        String blackList = getBlackList();

        String content;
        String conf;

        if (config.route.equals(Constants.Route.BYPASS_CHN)) {
            content = readFromRaw(R.raw.pdnsd_direct);
            conf = String.format(Locale.ENGLISH, content, pdnsdPort, rejectList, blackList, forwarderPort, blackList);

        } else {
            content = readFromRaw(R.raw.pdnsd_local);
            conf = String.format(Locale.ENGLISH, content, pdnsdPort, forwarderPort);
        }

        ConfigUtils.printToFile(new File(Constants.Path.BASE + "pdnsd.conf"), conf);

        String cmd = Constants.Path.BASE + "pdnsd -c " + Constants.Path.BASE + "pdnsd.conf";

        if (BuildConfig.DEBUG) {
            Log.d(TAG, cmd);
        }

        io.github.xsocks.System.exec(cmd);
    }

    private void route_bypass(Builder builder) {
        String line;
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(this.getResources().openRawResource(R.raw.route_bypass)));

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

    private void startVpn(){
        int VPN_MTU = 1500;
        Builder builder = new Builder();
        builder.setSession(config.profileName);
        builder.setMtu(VPN_MTU);
        builder.addAddress(VPN_ADDRESS, 24);
        builder.addDnsServer("8.8.4.4");

        if (Utils.isLollipopOrAbove()) {
            builder.allowFamily(android.system.OsConstants.AF_INET6);
            try {
                String packageName = getPackageName();
                builder.addDisallowedApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package name not found");
            }
        }

        if (config.route.equals(Constants.Route.ALL)) {
            builder.addRoute("0.0.0.0", 0);

        } else {
            route_bypass(builder);
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
                        + "--loglevel 3 "
                        + "--pid %stun2socks.pid",
                Constants.Path.BASE, "26.26.26.2", config.localPort, fd, VPN_MTU,
                Constants.Path.BASE);

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
            startDnsForwarder();
        }
        startVpn();
        return true;
    }

    private void startRunner(Config c) {
        config = c;

        // register close closeReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Constants.Action.CLOSE);
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, R.string.stopping, Toast.LENGTH_SHORT).show();
                stopRunner();
            }
        };
        registerReceiver(closeReceiver, filter);

        // ensure the VPNService is prepared
        if (VpnService.prepare(this) != null) {
            Intent i = new Intent(this, XsocksRunnerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }

        changeState(Constants.State.CONNECTING);

        Async.runAsync(Schedulers.newThread(), (observer, subscription) -> {
            if (config != null) {
                killProcesses();

                boolean resolved = false;
                if (!Utils.isIPv4Address(config.proxy) && !Utils.isIPv6Address(config.proxy)) {
                    String addr = Utils.resolve(config.proxy, true);
                    if (addr != null) {
                        config.proxy = addr;
                        resolved = true;
                    }

                } else {
                    resolved = true;
                }
                if (resolved && startDaemons()) {
                    notifyForegroundAlert(getString(R.string.forward_success),
                            getString(R.string.service_running, config.profileName),
                            false);
                    changeState(Constants.State.CONNECTED);

                } else {
                    changeState(Constants.State.STOPPED, getString(R.string.service_failed));
                    stopRunner();
                }
            }
        });
    }

    private void stopRunner() {
        stopForeground(true);

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

        // clean up receiver
        if (closeReceiver != null) {
            unregisterReceiver(closeReceiver);
            closeReceiver = null;
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
