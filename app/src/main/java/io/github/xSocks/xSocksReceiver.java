package io.github.xSocks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import io.github.xSocks.ui.xSocksRunnerActivity;
import io.github.xSocks.utils.Constants;

public class xSocksReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences status = context.getSharedPreferences(Constants.Key.status, Context.MODE_PRIVATE);

        String versionName;
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = null;
        }
        boolean isAutoConnect = settings.getBoolean(Constants.Key.isAutoConnect, false);
        boolean isInstalled = status.getBoolean(versionName, false);
        if (isAutoConnect && isInstalled) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent i = new Intent(context, xSocksRunnerActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(i);
            }
        }
    }
}
