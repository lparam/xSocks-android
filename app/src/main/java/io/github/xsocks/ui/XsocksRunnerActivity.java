package io.github.xsocks.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import io.github.xsocks.aidl.IXsocksService;
import io.github.xsocks.service.XsocksVpnService;
import io.github.xsocks.utils.ConfigUtils;
import io.github.xsocks.utils.Constants;

public class XsocksRunnerActivity extends Activity {

    private static final String TAG = "XSOCKS";

    private SharedPreferences settings = null;
    private BroadcastReceiver receiver;
    private IXsocksService bgService  = null;

    Handler handler = new Handler();

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bgService = IXsocksService.Stub.asInterface(service);
            handler.postDelayed(XsocksRunnerActivity.this::startBackgroundService, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bgService = null;
        }
    };

    private void startBackgroundService() {
        Intent intent = VpnService.prepare(XsocksRunnerActivity.this);
        int REQUEST_CONNECT = 1;
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
        } else {
            onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
        }
    }

    private void attachService() {
        if (bgService == null) {
            Intent intent = new Intent(this, XsocksVpnService.class);
            intent.setAction(Constants.Action.SERVICE);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, XsocksVpnService.class));
        }
    }

    private void deattachService() {
        if (bgService != null) {
            unbindService(connection);
            bgService = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km.inKeyguardRestrictedInputMode();
        if (locked) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equalsIgnoreCase(Intent.ACTION_USER_PRESENT)) {
                        attachService();
                    }
                }
            };
            registerReceiver(receiver, filter);
        } else {
            attachService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deattachService();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                if (bgService != null) {
                    if (settings == null) {
                        settings = PreferenceManager.getDefaultSharedPreferences(this);
                    }
                    try {
                        bgService.start(ConfigUtils.load(settings));
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to start VpnService");
                    }
                }
                break;
            default:
                Log.e(TAG, "Failed to start VpnService");
                break;
        }
        finish();
    }
}
