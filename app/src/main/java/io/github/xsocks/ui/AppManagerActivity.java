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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private int STUB = android.R.drawable.sym_def_app_icon;
    private ProxiedApp[] apps;

    private Runnable loadStartRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog = ProgressDialog
                    .show(AppManagerActivity.this, "", getString(R.string.loading), true, true);
        }
    };

    private Runnable loadFinishRunnable = new Runnable() {
        @Override
        public void run() {
            appListView.setAdapter(adapter);
            appListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (visible) {
                        String name = apps[firstVisibleItem].getName();
                        if (name != null && name.length() > 1) {
                            overlay.setText(apps[firstVisibleItem].getName().substring(0, 1));
                        } else {
                            overlay.setText("*");
                        }
                        overlay.setVisibility(View.VISIBLE);
                    }
                }

                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    visible = true;
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        overlay.setVisibility(View.INVISIBLE);
                    }
                }

                boolean visible = false;
            });

            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    };

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

    private class ListEntry {
        private CheckBox box;
        private TextView text;
        private ImageView icon;

        public ListEntry(CheckBox box, TextView text, ImageView icon) {
            this.box = box;
            this.text = text;
            this.icon = icon;
        }

        public CheckBox getBox() {
            return box;
        }

        public TextView getText() {
            return text;
        }

        public ImageView getIcon() {
            return icon;
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
        handler.post(loadStartRunnable);
        if (!appsLoaded) loadApps();
        handler.post(loadFinishRunnable);
    }

    private ProxiedApp[] loadApps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String proxiedAppString = prefs.getString(Constants.Key.proxied, "");
        String[] appString = proxiedAppString.split("\\|");
        Arrays.sort(appString);

        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> infoList = packageManager.getInstalledApplications(0);
        ArrayList<ProxiedApp> appList = new ArrayList<>();

        for (ApplicationInfo a : infoList) {
            if ((a.uid >= 10000)
                    && packageManager.getApplicationLabel(a) != null
                    && packageManager.getApplicationIcon(a) != null) {
                String name = packageManager.getApplicationLabel(a).toString();
                String userName = Integer.toString(a.uid);
                int index = Arrays.binarySearch(appString, userName);
                boolean proxied = index >= 0;
                if (a.uid == 10060 || a.uid == 10058) {
                    Log.d("test", "hello");
                }
                ProxiedApp app = new ProxiedApp(a.uid, name, a.packageName, proxied);
                appList.add(app);
            }
        }
        ProxiedApp[] appArray = new ProxiedApp[appList.size()];
        appList.toArray(appArray);
        return appArray;
    }

    private void loadApps() {
        apps = loadApps(this);
        Arrays.sort(apps, (a, b) -> {
/*            if (a == null || b == null || a.getName() == null || b.getName() == null) {
                return 1;
            } else if (a.getProxied() == b.getProxied()) {
                return a.getName().compareTo(b.getName());
            } else if (a.getProxied()) {
                return 1;
            } else {
                return 0;
            }*/
            if (a.getProxied()) {
                if (b.getProxied()) {
                    return a.getName().compareTo(b.getName());
                }
                return -1;

            } else if (a.getProxied() == b.getProxied()) {
                return a.getName().compareTo(b.getName());

            } else {
                return 1;
            }
        });

        adapter = new ArrayAdapter<ProxiedApp>(this, R.layout.apps_item, R.id.itemtext, apps) {
          @Override
          public View getView(int position, View view, ViewGroup parent) {
              View convertView = view;
              ListEntry entry;
              if (convertView == null) {
                  convertView = getLayoutInflater().inflate(R.layout.apps_item, parent, false);
                  TextView text = (TextView) convertView.findViewById(R.id.itemtext);
                  CheckBox box = (CheckBox) convertView.findViewById(R.id.itemcheck);
                  ImageView icon = (ImageView) convertView.findViewById(R.id.itemicon);
                  entry = new ListEntry(box, text, icon);
                  entry.getText().setOnClickListener(AppManagerActivity.this);
                  entry.getBox().setOnCheckedChangeListener(AppManagerActivity.this);
                  convertView.setTag(entry);

              } else {
                  entry = (ListEntry) convertView.getTag();
              }

              ProxiedApp app = apps[position];
              DisplayImageOptions options =
                      new DisplayImageOptions.Builder()
                              .showStubImage(STUB)
                              .showImageForEmptyUri(STUB)
                              .showImageOnFail(STUB)
                              .resetViewBeforeLoading()
                              .cacheInMemory()
                              .cacheOnDisc()
                              .displayer(new FadeInBitmapDisplayer(300))
                              .build();
              ImageLoader.getInstance().displayImage(Constants.Scheme.APP + app.getPackageName(), entry.icon, options);

              entry.text.setText(app.getName());
              CheckBox box = entry.getBox();
              box.setTag(app);
              box.setChecked(app.getProxied());
              entry.text.setTag(box);

              return convertView;
          }
        };

        appsLoaded = true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ProxiedApp app = (ProxiedApp) buttonView.getTag();
        if (app != null) {
            app.setProxied(isChecked);
        }
        saveAppSettings(this);
    }

    @Override
    public void onClick(View v) {
        CheckBox box = (CheckBox) v.getTag();
        ProxiedApp app = (ProxiedApp) box.getTag();
        if (app != null) {
            app.setProxied(!app.getProxied());
            box.setChecked(app.getProxied());
        }
        saveAppSettings(this);
    }

    private void saveAppSettings(Context context) {
        if (apps == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        StringBuilder proxiedApps = new StringBuilder();
        for (ProxiedApp app : apps) {
            if (app.getProxied()) {
                proxiedApps.append(Integer.toString(app.getId()));
                proxiedApps.append("|");
            }
        }
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Constants.Key.proxied, proxiedApps.toString());
        edit.apply();
    }

    public static ProxiedApp[] getProxiedApps(Context context, String proxiedAppString) {
        String[] proxiedApps = proxiedAppString.split("\\|");
        Arrays.sort(proxiedApps);

        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> infoList = packageManager.getInstalledApplications(0);
        ArrayList<ProxiedApp> appList = new ArrayList<>();

        for (ApplicationInfo a : infoList) {
            if (a.uid >= 10000) {
                int uid = a.uid;
                String name = packageManager.getApplicationLabel(a).toString();
                String packageName = a.packageName;
                int index = Arrays.binarySearch(proxiedApps, Integer.toString(uid));
                boolean proxied = index >= 0;
                if (proxied) {
                    ProxiedApp app = new ProxiedApp(uid, name, packageName, proxied);
                    appList.add(app);
                }
            }
        }
        ProxiedApp[] appArray = new ProxiedApp[appList.size()];
        appList.toArray(appArray);
        return appArray;
    }

}
