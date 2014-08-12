package com.denisk.bullshitbingochampion;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * @author denisk
 * @since 12.08.14.
 */
public class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
