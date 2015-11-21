package io.github.xSocks.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class SummaryListPreference extends ListPreference {
    public SummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setValue(String text) {
        super.setValue(text);
        CharSequence entry = getEntry();
        if (entry != null)
            setSummary(entry);
        else
            setSummary(text);
    }

    @Override
    public void setSummary(CharSequence summary) {
        if (summary == null || summary.toString().isEmpty()) {
            super.setSummary("");
        } else {
            super.setSummary(summary);
        }
    }
}
