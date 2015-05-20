package io.github.xsocks.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import io.github.xsocks.utils.Constants;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class ProfileManager {

    private SharedPreferences settings;
    private final Context context;
    private String TAG = "XSOCKS";

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

    public Profile firstCreate() {
        final Realm realm = Realm.getInstance(context);
        Profile profile = new Profile();
        profile = loadFromPreferences(profile);
        int nextId = (int) (realm.where(Profile.class).maximumInt("id") + 1);
        if (nextId > 1) return profile;
        profile.setId(nextId);
        realm.beginTransaction();
        Profile realmProfile = realm.copyToRealm(profile);
        realm.commitTransaction();
        setPreferences(profile);
        return realmProfile;
    }

    public Profile create() {
        final Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        Profile profile = new Profile();
        int nextId = (int) (realm.where(Profile.class).maximumInt("id") + 1);
        profile.setId(nextId);
        Profile realmProfile = realm.copyToRealm(profile);
        realm.commitTransaction();
        setPreferences(profile);
        return realmProfile;
    }

    public Profile save() {
        final Realm realm = Realm.getInstance(context);
        int id = settings.getInt(Constants.Key.profileId, -1);
        Profile profile = getProfile(id);
        if (profile != null) {
            realm.beginTransaction();
            profile = loadFromPreferences(profile);
            realm.commitTransaction();
        }
        return profile;
    }

    public RealmResults<Profile> getAllProfile() {
        final Realm realm = Realm.getInstance(context);
        final RealmQuery<Profile> query = realm.where(Profile.class);
        return query.findAll();
    }

    public Profile getProfile(int id) {
        try {
            final Realm realm = Realm.getInstance(context);
            final RealmQuery<Profile> query = realm.where(Profile.class);
            query.equalTo("id", id);
            return query.findFirst();

        } catch (Exception ex) {
            Log.e(TAG, "getProfile", ex);
            return null;
        }
    }

    public boolean delProfile(int id) {
        try {
            final Realm realm = Realm.getInstance(context);
            final RealmQuery<Profile> query = realm.where(Profile.class);
            realm.beginTransaction();
            RealmResults<Profile> result = query.equalTo("id", id).findAll();
            result.clear();
            realm.commitTransaction();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "getProfile", ex);
            return false;
        }
    }

    public Profile load(int id) {
        Profile profile = getProfile(id);
        if (profile == null) {
            profile = create();
        }
        setPreferences(profile);
        return profile;
    }

    public Profile reload(int id) {
        save();
        return load(id);
    }

}
