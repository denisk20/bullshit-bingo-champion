package com.denisk.bullshitbingochampion;

import android.app.*;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * @author denisk
 * @since 03.08.14.
 */
public abstract class EditTextDialogFragment extends DialogFragment {
    protected CharSequence currentCellValue;
    protected EditText editText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        editText = new EditText(activity);
        editText.setText(currentCellValue);
        Resources res = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(getTitle())
                .setView(editText)
                .setPositiveButton(res.getString(R.string.ok), getOKListener()).setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                });

        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.selectAll();
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Service.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, 0);
            }
        });

        return builder.create();
    }

    public void setCurrentCellValue(CharSequence currentCellValue) {
        this.currentCellValue = currentCellValue;
    }

    protected abstract DialogInterface.OnClickListener getOKListener();

    public abstract String getTitle();
}
