package io.github.xsocks.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import io.github.xsocks.R;
import io.github.xsocks.model.ProxiedApp;
import io.github.xsocks.utils.Constants;
import io.github.xsocks.utils.Utils;


public class AppManagerActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private Handler handler = null;
    private TextView overlay = null;
    private boolean appsLoaded;
    private ListAdapter adapter;
    private ListView appListView;
    private ProgressDialog progressDialog;

    private class AppIconDownloader extends BaseImageDownloader {

        public AppIconDownloader(Context context, int connectTimeout, int readTimeout) {
            super(context, connectTimeout, readTimeout);
        }

        public AppIconDownloader(Context context) {
            this(context, 0, 0);
        }

        @Override
        public InputStream getStreamFromOtherSource(String imageUri, Object extra) {
            String packageName = imageUri.substring(Constants.Scheme.APP.length());
            Drawable drawable = Utils.getAppIcon(getBaseContext(), packageName);
            Bitmap bitmap = Utils.drawableToBitmap(drawable);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            return new ByteArrayInputStream(os.toByteArray());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            Drawable upArrow;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha, null);
            } else {
                upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            }
            if (upArrow != null) {
                upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
            }
            ab.setHomeAsUpIndicator(upArrow);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        handler = new Handler();
        this.overlay = (TextView) View.inflate(this, R.layout.overlay, null);
        getWindowManager().addView(overlay, new
                WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT));

        ImageLoaderConfiguration config =
                new ImageLoaderConfiguration.Builder(this)
                        .imageDownloader(new AppIconDownloader(this))
                        .build();
        ImageLoader.getInstance().init(config);

        Switch bypassSwitch = (Switch) findViewById(R.id.bypassSwitch);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        bypassSwitch.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(Constants.Key.isBypassApps, checked).commit());

        bypassSwitch.setChecked(prefs.getBoolean(Constants.Key.isBypassApps, false));

        appListView = (ListView) findViewById(R.id.applistview);

    }

    @Override
    protected void onResume() {
        super.onResume();
/*        handler.post(loadStartRunnable);
        if (!appsLoaded) loadApps();
        handler.post(loadFinishRunnable);*/
        loadApps(getBaseContext());
    }

    private ProxiedApp[] loadApps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String proxiedAppString = prefs.getString(Constants.Key.proxied, "");
        String[] proxiedApps = proxiedAppString.split("|");

        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);

        ArrayList<ProxiedApp> apps = new ArrayList<>();

        for (ApplicationInfo a : appList) {
            if ((a.uid >= 10000)
                    && packageManager.getApplicationLabel(a) != null
                    && packageManager.getApplicationIcon(a) != null) {
                String userName = Integer.toString(a.uid);
                int index = Arrays.binarySearch(proxiedApps, a.packageName);
                boolean proxied = index >= 0;
                apps.add(new ProxiedApp(a.uid, packageManager.getApplicationLabel(a).toString(),
                        a.packageName, proxied));
            }
        }
        ProxiedApp[] appArray = new ProxiedApp[apps.size()];
        apps.toArray(appArray);
        return appArray;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public void onClick(View v) {

    }
}
