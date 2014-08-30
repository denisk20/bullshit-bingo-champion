package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.NumberPicker;

/**
 * @author denisk
 * @since 7/21/14.
 */
public class SelectDimensionDialogFragment extends DialogFragment {

    public interface DimensionSelectedListener {
        void onDimensionSelected(int dim);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final NumberPicker numberPicker = new NumberPicker(activity);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Resources res = getResources();
        numberPicker.setMinValue(res.getInteger(R.integer.min_cells));
        numberPicker.setMaxValue(res.getInteger(R.integer.max_cells));
        builder.setMessage(R.string.select_dim)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((DimensionSelectedListener) activity).onDimensionSelected(numberPicker.getValue());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setView(numberPicker);

        return builder.create();
    }
}
