package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import de.cketti.library.changelog.ChangeLog;

/**
 * @author denisk
 * @since 12.08.14.
 */
public class PreferencesFragment extends PreferenceFragment {

    private Activity parent;
    private Preference reloadCardsButton;
    private Preference resetColorsButton;
    private Preference changelogButton;

    private Preference.OnPreferenceClickListener onReloadCardsClickListener = new Preference.OnPreferenceClickListener() {
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

    private Preference.OnPreferenceClickListener onResetColorsClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

            new AlertDialog.Builder(parent)
                    .setTitle(R.string.pref_title_reset_colors)
                    .setMessage(R.string.pref_dialog_reset_colors)
                    .setPositiveButton(R.string.ok, resetColorsDialogListener)
                    .setNegativeButton(R.string.cancel, resetColorsDialogListener)
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

    private DialogInterface.OnClickListener resetColorsDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    String[] colorKeys = getResources().getStringArray(R.array.prefs_colors);
                    SharedPreferences.Editor edit = getPreferenceManager().getSharedPreferences().edit();
                    for(String colorKey : colorKeys) {
                        edit.remove(colorKey);
                    }
                    edit.commit();

                    setPreferenceScreen(null);
                    initPrefs();

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

        initPrefs();

        parent = getActivity();

        reloadCardsButton = findPreference(getString(R.string.pref_reload_default_cards_key));
        reloadCardsButton.setOnPreferenceClickListener(onReloadCardsClickListener);

        resetColorsButton = findPreference(getString(R.string.pref_reset_colors_key));
        resetColorsButton.setOnPreferenceClickListener(onResetColorsClickListener);

        changelogButton = findPreference(getString(R.string.pref_show_changelog_key));
        changelogButton.setOnPreferenceClickListener(onShowChangelogListener);

    }

    private void initPrefs() {
        addPreferencesFromResource(R.xml.preferences);
        addPreferencesFromResource(R.xml.preferences_colors);
    }
}
