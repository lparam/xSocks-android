package io.github.xSocks.ui;

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

import io.github.xSocks.aidl.IxSocksService;
import io.github.xSocks.service.xSocksVpnService;
import io.github.xSocks.utils.ConfigUtils;
import io.github.xSocks.utils.Constants;

public class xSocksRunnerActivity extends Activity {

    private static final String TAG = "xSocks";

    private SharedPreferences settings = null;
    private BroadcastReceiver receiver;
    private IxSocksService bgService  = null;

    Handler handler = new Handler();

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bgService = IxSocksService.Stub.asInterface(service);
            handler.postDelayed(xSocksRunnerActivity.this::startBackgroundService, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bgService = null;
        }
    };

    private void startBackgroundService() {
        Intent intent = VpnService.prepare(xSocksRunnerActivity.this);
        int REQUEST_CONNECT = 1;
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
        } else {
            onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
        }
    }

    private void attachService() {
        if (bgService == null) {
            Intent intent = new Intent(this, xSocksVpnService.class);
            intent.setAction(Constants.Action.SERVICE);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, xSocksVpnService.class));
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
