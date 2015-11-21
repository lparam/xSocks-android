package io.github.xSocks.preferences;

import android.content.Context;
import android.content.Intent;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import io.github.xSocks.utils.Constants;

public class ProfileEditTextPreference extends EditTextPreference {
    private CharSequence mDefaultSummary = getSummary();
    private Context context;

    public ProfileEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextPreferenceStyle);
        mDefaultSummary = getSummary();
    }

    public ProfileEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        setSummary(text);
    }

    @Override
    public void setSummary(CharSequence summary) {
        if (summary.toString().isEmpty()) {
            super.setSummary(mDefaultSummary);
        } else {
            super.setSummary(summary);
        }
        context.sendBroadcast(new Intent(Constants.Action.UPDATE_PREFS));
    }

    public void resetSummary(CharSequence summary) {
        if (summary.toString().isEmpty()) {
            super.setSummary(mDefaultSummary);
        } else {
            super.setSummary(summary);
        }
    }
}
