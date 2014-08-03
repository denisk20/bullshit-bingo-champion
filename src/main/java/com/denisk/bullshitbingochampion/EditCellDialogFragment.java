package com.denisk.bullshitbingochampion;

import android.content.DialogInterface;
import android.widget.Toast;

/**
 * @author denisk
 * @since 27.07.14.
 */
public class EditCellDialogFragment extends EditTextDialogFragment {
    private int position;

    public interface CellEditFinishedListener {
        void onCellEditFinished(CharSequence newValue, int position);
    }

    @Override
    protected DialogInterface.OnClickListener getOKListener() {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String text = editText.getText().toString();
                if(text.startsWith(BullshitBingoActivity.COMMENT_MARK)) {
                    Toast.makeText(getActivity(), R.string.error_cant_start_comment + BullshitBingoActivity.COMMENT_MARK, Toast.LENGTH_SHORT).show();
                    return;
                }
                ((CellEditFinishedListener) getActivity()).onCellEditFinished(text, position);
            }
        };
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.edit_cell_title);
    }

    public void setPosition(int position) {
        this.position = position;
    }

}
