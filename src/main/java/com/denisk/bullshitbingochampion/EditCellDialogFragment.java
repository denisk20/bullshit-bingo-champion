package com.denisk.bullshitbingochampion;

import android.app.*;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * @author denisk
 * @since 27.07.14.
 */
public class EditCellDialogFragment extends DialogFragment {
    private CharSequence currentCellValue;
    private int position;

    public interface CellEditFinishedListener {
        void onCellEditFinished(CharSequence newValue, int position);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        final EditText editText = new EditText(activity);
        editText.setText(currentCellValue);
        Resources res = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(res.getString(R.string.edit_cell_title))
                .setView(editText)
                .setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((CellEditFinishedListener) activity).onCellEditFinished(editText.getText(), position);
                    }
                }).setNegativeButton(res.getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
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

    public void setPosition(int position) {
        this.position = position;
    }

    public void setCurrentCellValue(CharSequence currentCellValue) {
        this.currentCellValue = currentCellValue;
    }
}
