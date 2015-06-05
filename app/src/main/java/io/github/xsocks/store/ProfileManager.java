package io.github.xsocks.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import io.github.xsocks.model.Profile;
import io.github.xsocks.model.Profiles;
import io.github.xsocks.utils.Constants;

public class ProfileManager {
    private SharedPreferences settings;
    private final Context context;
    private String TAG = "XSOCKS";
    private Profiles profiles;
    private String filename = "profiles.json";


    public ProfileManager(final Context context, SharedPreferences settings) {
        this.context = context;
        this.settings = settings;
    }

    private Profile loadFromPreferences(Profile profile) {
        int id = settings.getInt(Constants.Key.profileId, -1);
        profile.setId(id);
        profile.setGlobal(settings.getBoolean(Constants.Key.isGlobalProxy, false));
        profile.setBypass(settings.getBoolean(Constants.Key.isBypassApps, false));
        profile.setUdpdns(settings.getBoolean(Constants.Key.isUdpDns, false));
        profile.setName(settings.getString(Constants.Key.profileName, "Default"));
        profile.setHost(settings.getString(Constants.Key.proxy, ""));
        profile.setPassword(settings.getString(Constants.Key.sitekey, ""));
        profile.setRoute(settings.getString(Constants.Key.route, "all"));

        try {
            profile.setRemotePort(Integer.valueOf(settings.getString(Constants.Key.remotePort, "1073")));
        } catch (NumberFormatException ex) {
            profile.setRemotePort(1073);
        }

        try {
            profile.setLocalPort(Integer.valueOf(settings.getString(Constants.Key.localPort, "1080")));
        } catch (NumberFormatException ex) {
            profile.setRemotePort(1080);
        }

        profile.setIndividual(settings.getString(Constants.Key.proxied, ""));

        return profile;
    }

    private void setPreferences(Profile profile) {
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean(Constants.Key.isBypassApps, profile.isBypass());
        edit.putBoolean(Constants.Key.isUdpDns, profile.isUdpdns());
        edit.putString(Constants.Key.profileName, profile.getName());
        edit.putString(Constants.Key.proxy, profile.getHost());
        edit.putString(Constants.Key.sitekey, profile.getPassword());
        edit.putString(Constants.Key.remotePort, Integer.toString(profile.getRemotePort()));
        edit.putString(Constants.Key.localPort, Integer.toString(profile.getLocalPort()));
        edit.putString(Constants.Key.proxied, profile.getIndividual());
        edit.putInt(Constants.Key.profileId, profile.getId());
        edit.putString(Constants.Key.route, profile.getRoute());
        edit.apply();
    }

    public List<Profile> getAllProfile() {
        profiles = reloadAll();
        return profiles.getProfiles();
    }

    private Profiles reloadAll() {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            Gson gson = new Gson();
            return gson.fromJson(json, Profiles.class);

        } catch (IOException e) {
            return new Profiles();
        }
    }

    private void createProfile(Profile profile) {
        profiles.addProfile(profile);
        saveAll();
        profiles = reloadAll();
    }

    public Profile firstCreate() {
        Profile profile = new Profile();
        profile = loadFromPreferences(profile);
        int nextId = profiles.getMaxId() + 1;
        if (nextId > 1) return profile;
        profile.setId(nextId);
        createProfile(profile);
        setPreferences(profile);
        return profile;
    }

    public Profile create() {
        Profile profile = new Profile();
        int nextId = profiles.getMaxId() + 1;
        profile.setId(nextId);
        createProfile(profile);
        setPreferences(profile);
        return profile;
    }

    private void saveAll() {
        FileOutputStream outputStream;

        try {
            Gson gson = new Gson();
            String json = gson.toJson(profiles);
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(json.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "save", e);
        }
    }

    public Profile save() {
        int id = settings.getInt(Constants.Key.profileId, -1);
        Profile profile = profiles.getProfile(id);
        if (profile != null) {
            profile = loadFromPreferences(profile);
            saveAll();
        }
        return profile;
    }

    public Profile getProfile(int id) {
        return profiles.getProfile(id);
    }

    public void delProfile(int id) {
        try {
            profiles.remove(id);
            saveAll();
        } catch (Exception ex) {
            Log.e(TAG, "getProfile", ex);
        }
    }

    public Profile load(int id) {
        Profile profile = profiles.getProfile(id);
        if (profile == null) {
            profile = create();
        }
        setPreferences(profile);
        return profile;
    }

    public Profile reload(int id) {
        saveAll();
        return load(id);
    }

}
