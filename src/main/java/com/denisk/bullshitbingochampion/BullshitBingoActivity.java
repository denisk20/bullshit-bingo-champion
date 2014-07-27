package com.denisk.bullshitbingochampion;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class BullshitBingoActivity extends Activity implements SelectDimensionDialogFragment.DimensionSelectedListener, EditCellDialogFragment.CellEditFinishedListener {

    public static final String BULLSHIT_FILES_PATH_KEY = "bullshitFilesPath";
    public static final String DIR_NAME = "bullshitbingochamp";
    public static final String PREFS_NAME = "bullshitbingochamp";

    public static final String BUNDLE_DIM = "dim";
    public static final String BUNDLE_WORDS = "words";
    public static final String BUNDLE_IS_EDITING = "isEditing";

    private DynamicGridView gridView;

    private BaseDynamicGridAdapter gridAdapter;

    private SelectDimensionDialogFragment dimensionDialog;
    private EditCellDialogFragment editCellDialog;

    boolean isEditing;
    boolean isDirty;

    private int dim;

    private MenuItem newMenuItem;
    private MenuItem editMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem cancelMenuItem;
    private MenuItem acceptItemMenuItem;


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
    private int gridWidth;
    private int gridHeight;
    private float shift;

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

        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridWidth = gridView.getWidth();
                gridHeight = gridView.getHeight();
            }
        });

        gridAdapter = new BaseDynamicGridAdapter(BullshitBingoActivity.this, new ArrayList<>(), dim) {
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

                if(editCellDialog == null) {
                    editCellDialog = new EditCellDialogFragment();
                }
                editCellDialog.setCurrentCellValue(currentCellValue);
                editCellDialog.setPosition(position);

                editCellDialog.show(getFragmentManager(), "editCell");
            }
        });

        //we need this to be empty because https://github.com/askerov/DynamicGrid/issues/27
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

        restoreFromBundle(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        restoreFromBundle(savedInstanceState);
    }

    private void restoreFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            dim = savedInstanceState.getInt(BUNDLE_DIM);
            isEditing = savedInstanceState.getBoolean(BUNDLE_IS_EDITING);
            if(isEditing) {
                prepareForEdit();
            }
            ArrayList<String> wordsArrayList = savedInstanceState.getStringArrayList(BUNDLE_WORDS);
            if(wordsArrayList == null) {
                return;
            }
            ArrayList<StringHolder> currentWords = new ArrayList<>(wordsArrayList.size());
            for(String s: wordsArrayList) {
                currentWords.add(new StringHolder(s));
            }

            initBoardFromPresetData(currentWords);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(BUNDLE_WORDS, getCharSequenceListFromCurrentWords());
        outState.putInt(BUNDLE_DIM, dim);
        outState.putBoolean(BUNDLE_IS_EDITING, isEditing);
    }

    private ArrayList<String> getCharSequenceListFromCurrentWords() {
        ArrayList<String> words = new ArrayList<>();
        for (Object o : gridAdapter.getItems()) {
            StringHolder h = (StringHolder) o;
            words.add(h.s.toString());
        }

        return words;
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
                prepareForEdit();
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

    private void prepareForEdit() {
        Toast.makeText(this, R.string.long_press_to_drag, Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
        gridView.setOnItemLongClickListener(itemLongClickListener);
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
        acceptItemMenuItem = menu.findItem(R.id.action_accept);

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
        this.dim = dim;

        initCleanBoard();
    }

    private void initCleanBoard() {
        ArrayList<StringHolder> currentWords = new ArrayList<>(dim*dim);
        for (int i = 0; i < dim; i++) {
            for(int j = 0; j < dim; j++) {
                currentWords.add(new StringHolder(""));
            }
        }

        initBoardFromPresetData(currentWords);
        invalidateOptionsMenu();
    }

    private void initBoardFromPresetData(final ArrayList<StringHolder> currentWords) {
        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridView.setNumColumns(dim);

                final float offset = getResources().getDimension(R.dimen.cell_spacing);
                shift = offset * ((float) (dim - 2) / (dim));

                gridAdapter.set(currentWords);
                gridAdapter.setColumnCount(dim);
            }
        });
    }

    @Override
    public void onCellEditFinished(CharSequence newValue, int position) {
        gridAdapter.setItemAtPosition(new StringHolder(newValue), position);
    }

    /**
     * DynamicGridView can't hold equal objects, so we wrap
     * Strings so that even equal strings look like unequal
     * objects for it
     */
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
