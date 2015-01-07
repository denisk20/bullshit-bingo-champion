package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import de.cketti.library.changelog.ChangeLog;

/**
 * @author denisk
 * @since 12.08.14.
 */
public class PreferencesFragment extends PreferenceFragment {

    private Activity parent;
    private Preference resetButton;
    private Preference changelogButton;

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
    private Preference.OnPreferenceClickListener onShowChangelogListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            ChangeLog cl = new ChangeLog(parent);
            cl.getFullLogDialog().show();

            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        parent = getActivity();

        resetButton = findPreference(getString(R.string.pref_reload_default_cards_key));
        resetButton.setOnPreferenceClickListener(onPreferenceClickListener);

        changelogButton = findPreference(getString(R.string.pref_show_changelog_key));

        changelogButton.setOnPreferenceClickListener(onShowChangelogListener);

    }
}
