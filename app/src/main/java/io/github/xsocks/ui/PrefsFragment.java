package io.github.xsocks.ui;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import io.github.xsocks.R;
import io.github.xsocks.database.Profile;
import io.github.xsocks.preferences.PasswordEditTextPreference;
import io.github.xsocks.preferences.ProfileEditTextPreference;
import io.github.xsocks.preferences.SummaryEditTextPreference;
import io.github.xsocks.utils.Constants;

public class PrefsFragment extends PreferenceFragment {
    public static String[] PROXY_PREFS = {
            Constants.Key.profileName,
            Constants.Key.proxy,
            Constants.Key.remotePort,
            Constants.Key.localPort,
            Constants.Key.sitekey};

    public static String[] FEATURE_PREFS = {
            Constants.Key.route,
            Constants.Key.isUdpDns,
            Constants.Key.isAutoConnect};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    public void setPreferenceEnabled(boolean enabled) {
        for (String name : PROXY_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                pref.setEnabled(enabled);
            }
        }
        for (String name : FEATURE_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                pref.setEnabled(enabled);
            }
        }
    }

    public void updatePreferenceScreen(Profile profile) {
        for (String name : PROXY_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                updatePreference(pref, name, profile);
            }
        }
        for (String name : FEATURE_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                updatePreference(pref, name, profile);
            }
        }
    }

    private void updateListPreference(Preference pref, String value) {
        ((ListPreference)pref).setValue(value);
    }

    private void updatePasswordEditTextPreference(Preference pref, String value) {
        pref.setSummary(value);
        ((PasswordEditTextPreference)pref).setText(value);
    }

    private void updateSummaryEditTextPreference(Preference pref, String value) {
        pref.setSummary(value);
        ((SummaryEditTextPreference)pref).setText(value);
    }

    private void updateProfileEditTextPreference(Preference pref, String value) {
        ((ProfileEditTextPreference)pref).resetSummary(value);
        ((ProfileEditTextPreference)pref).setText(value);
    }

    private void updateCheckBoxPreference(Preference pref, boolean value) {
        ((CheckBoxPreference)pref).setChecked(value);
    }

    public void updatePreference(Preference pref, String name, Profile profile) {
        switch (name) {
            case Constants.Key.profileName: updateProfileEditTextPreference(pref, profile.getName()); break;
            case Constants.Key.proxy: updateSummaryEditTextPreference(pref, profile.getHost()); break;
            case Constants.Key.remotePort: updateSummaryEditTextPreference(pref, Integer.toString(profile.getRemotePort())); break;
            case Constants.Key.localPort: updateSummaryEditTextPreference(pref, Integer.toString(profile.getLocalPort())); break;
            case Constants.Key.sitekey: updatePasswordEditTextPreference(pref, profile.getPassword()); break;
            case Constants.Key.route: updateListPreference(pref, profile.getRoute()); break;
            case Constants.Key.isGlobalProxy: updateCheckBoxPreference(pref, profile.isGlobal()); break;
            case Constants.Key.isUdpDns: updateCheckBoxPreference(pref, profile.isUdpdns()); break;
            default: break;
        }
    }

}
