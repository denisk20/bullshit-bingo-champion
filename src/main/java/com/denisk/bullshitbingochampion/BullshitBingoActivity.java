package com.denisk.bullshitbingochampion;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class BullshitBingoActivity extends Activity implements SelectDimensionDialogFragment.DimensionSelectedListener {

    public static final String BULLSHIT_FILES_PATH_KEY = "bullshitFilesPath";
    public static final String DIR_NAME = "bullshitbingochamp";
    public static final String PREFS_NAME = "bullshitbingochamp";

    private DynamicGridView gridView;
    private SelectDimensionDialogFragment dimensionDialog;

    boolean isEditing;
    boolean isDirty;

    private MenuItem newMenuItem;
    private MenuItem editMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem cancelMenuItem;

    private AdapterView.OnItemLongClickListener itemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Log.e("***", "Long click");
            if (isEditing) {
                gridView.startEditMode(position);
            }
            return true;
        }
    };
    private ArrayList<StringHolder> currentWords = new ArrayList<>();
    private BaseDynamicGridAdapter gridAdapter;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e("***", "Creating");
        super.onCreate(savedInstanceState);
        createDirIfNeeded();

        setContentView(R.layout.bingo_activity);

        gridView = (DynamicGridView) findViewById(R.id.gridview);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, long id) {
                if(! isEditing) {
                    //todo handle the game
                    return;
                }
                StringHolder itemAtPosition = (StringHolder) parent.getItemAtPosition(position);
                if(itemAtPosition == null) {
                    return;
                }
                CharSequence currentCellValue = itemAtPosition.s;
                final EditText editText = new EditText(BullshitBingoActivity.this);
                editText.setText(currentCellValue);
                Resources res = getResources();
                AlertDialog.Builder builder = new AlertDialog.Builder(BullshitBingoActivity.this)
                        .setTitle(res.getString(R.string.edit_cell_title))
                        .setView(editText)
                        .setPositiveButton(res.getString(R.string.action_save), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                gridAdapter.setItemAtPosition(new StringHolder(editText.getText()), position);
                            }
                        }).setNegativeButton(res.getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        });
                builder.show();

                editText.post(new Runnable() {
                    @Override
                    public void run() {
                        editText.selectAll();
                        InputMethodManager imm = (InputMethodManager) BullshitBingoActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(editText, 0);
                    }
                });
            }
        });

        //todo we need this to be empty because https://github.com/askerov/DynamicGrid/issues/27
        gridView.setOnDragListener(new DynamicGridView.OnDragListener() {
            @Override
            public void onDragStarted(int position) {
            }

            @Override
            public void onDragPositionsChanged(int oldPosition, int newPosition) {
            }
        });

        gridView.setOnDropListener(new DynamicGridView.OnDropListener() {
            @Override
            public void onActionDrop() {
                gridView.stopEditMode();
            }
        });
    }

    private boolean isGridFilled() {
        return gridView != null && gridView.getAdapter() != null;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_new:
                showSelectNewCardDimensionDialog();
                return true;
            case R.id.action_edit:
                isEditing = true;
                Toast.makeText(this, R.string.long_press_to_drag, Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
                gridView.setOnItemLongClickListener(itemLongClickListener);
                return true;
            case R.id.action_save:
                exitEditMode();
                //todo save thing
                return true;
            case R.id.action_cancel:
                exitEditMode();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void exitEditMode() {
        isEditing = false;
        if (gridView.isEditMode()) {
            gridView.stopEditMode();
        }
        invalidateOptionsMenu();
        gridView.setOnItemLongClickListener(null);
    }

    private void showSelectNewCardDimensionDialog() {
        if(dimensionDialog == null) {
            dimensionDialog = new SelectDimensionDialogFragment();
        }
        dimensionDialog.show(getFragmentManager(), "dimension");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bbc_actions, menu);

        newMenuItem = menu.findItem(R.id.action_new);
        editMenuItem = menu.findItem(R.id.action_edit);
        saveMenuItem = menu.findItem(R.id.action_save);
        cancelMenuItem = menu.findItem(R.id.action_cancel);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.i("===", "Preparing menu");
        if (isEditing) {
            newMenuItem.setVisible(false);
            editMenuItem.setVisible(false);
            saveMenuItem.setVisible(true);
            cancelMenuItem.setVisible(true);
        } else {
            newMenuItem.setVisible(true);
            if (isGridFilled()) {
                editMenuItem.setVisible(true);
            }
            saveMenuItem.setVisible(false);
            cancelMenuItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDimensionSelected(final int dim) {
        Log.i("===", "Dimension selected:" + dim);
        gridView.setNumColumns(dim);

        if (currentWords.size() > 0) {
            currentWords.clear();
        }
        for (int i = 0; i < dim; i++) {
            for(int j = 0; j < dim; j++) {
                currentWords.add(new StringHolder(""));
            }
        }

        final int gridWidth = gridView.getWidth();
        final int gridHeight = gridView.getHeight();
        final float offset = getResources().getDimension(R.dimen.cell_spacing);
        final float shift = offset * ((float) (dim - 2) / (dim));
        gridAdapter = new BaseDynamicGridAdapter(BullshitBingoActivity.this, currentWords, dim) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                StringHolder text = (StringHolder) getItem(position);
                TextView textView;
                if (!(convertView instanceof TextView)) {
                    textView = (TextView) getLayoutInflater().inflate(R.layout.word, null);
                    //this is set in bingo_activity
                    textView.setWidth((int) (gridWidth / dim - shift));
                    textView.setHeight((int) (gridHeight / dim - shift));

                } else {
                    textView = (TextView) convertView;
                }

                textView.setText(text.s);
                setViewVisibilityOnPosition(position, textView);
                return textView;
            }
        };

        gridView.setAdapter(gridAdapter);
        invalidateOptionsMenu();
    }

    private static class StringHolder {
        CharSequence s;

        StringHolder(CharSequence s) {
            this.s = s;
        }
    }
    @Override
    public void onBackPressed() {
        if (gridView.isEditMode()) {
            gridView.stopEditMode();
        } else {
            super.onBackPressed();
        }
    }

    private void createDirIfNeeded() {
        File bullshitDir;
        if(isExternalStorageWritable()) {
            bullshitDir = Environment.getExternalStoragePublicDirectory(DIR_NAME);
        } else {
            //use internal storage
            bullshitDir = getDir(DIR_NAME, MODE_PRIVATE);
        }

        bullshitDir.mkdirs();

        try {
            new File(bullshitDir, ".nomedia").createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException("Can't create .nomedia file", e);
        }

        if(! bullshitDir.exists()) {
            throw new IllegalStateException("Can't create directory to store *.bullshit files at " + bullshitDir);
        } else {
            Log.i(BullshitBingoActivity.class.getName(), "===Bullshit dir: " + bullshitDir);
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(BULLSHIT_FILES_PATH_KEY, bullshitDir.toString()).commit();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
