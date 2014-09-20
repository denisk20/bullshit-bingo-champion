package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * @author denisk
 * @since 12.08.14.
 */
public class PreferencesFragment extends PreferenceFragment {

    private Activity parent;
    private Preference resetButton;

    private Preference.OnPreferenceClickListener onPreferenceClickListener= new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

            new AlertDialog.Builder(parent)
                    .setTitle(R.string.pref_title_reload)
                    .setMessage(R.string.pref_dialog_reload)
                    .setPositiveButton(R.string.ok, reloadDialogListener)
                    .setNegativeButton(R.string.cancel, reloadDialogListener)
                    .show();

            return true;
        }
    };

    private DialogInterface.OnClickListener reloadDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Intent intent = new Intent();
                    intent.putExtra(PreferencesActivity.RELOAD_DEFAULT_CARDS, true);

                    parent.setResult(Activity.RESULT_OK, intent);
                    parent.finish();

                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        parent = getActivity();

        resetButton = findPreference("reload_default_cards");

        resetButton.setOnPreferenceClickListener(onPreferenceClickListener);

    }
}
