package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
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

        final Activity parent = getActivity();

        Preference resetButton = findPreference("reload_default_cards");
        resetButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.putExtra(PreferencesActivity.RELOAD_DEFAULT_CARDS, true);

                parent.setResult(Activity.RESULT_OK, intent);
                parent.finish();

                return true;
            }
        });

    }
}
