package com.denisk.bullshitbingochampion;

import android.content.DialogInterface;
import android.widget.Toast;

/**
 * @author denisk
 * @since 03.08.14.
 */
public class SaveCardDialogFragment extends EditTextDialogFragment {
    private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':', '#' };

    public interface SaveCardDialogListener {
        void onCardNamePopulated(String name);
    }

    @Override
    protected DialogInterface.OnClickListener getOKListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = editText.getText().toString();
                if(text.isEmpty()) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.error_empty_card_name), Toast.LENGTH_SHORT).show();
                    return;
                }
                for(char c: ILLEGAL_CHARACTERS) {
                    if(text.indexOf(c) >= 0) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.error_wrong_filename), Toast.LENGTH_SHORT).show();
                        return;
                    }

                }
                ((SaveCardDialogListener) getActivity()).onCardNamePopulated(text);
            }
        };
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.save_card_title);
    }
}
