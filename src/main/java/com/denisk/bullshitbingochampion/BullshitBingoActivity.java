package com.denisk.bullshitbingochampion;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BullshitBingoActivity extends Activity
        implements SelectDimensionDialogFragment.DimensionSelectedListener, EditCellDialogFragment.CellEditFinishedListener, SaveCardDialogFragment.SaveCardDialogListener {

    public static final String PREFS_NAME = "bullshitbingochamp";
    //todo do we need it?
    public static final String DIR_NAME = "bullshitbingochamp";

    public static final String BUNDLE_DIM = "dim";
    public static final String BUNDLE_WORDS = "words";
    public static final String BUNDLE_IS_EDITING = "isEditing";
    public static final ColorDrawable LIGHT_ERROR_COLOR = new ColorDrawable(0xFFFFF0F0);
    public static final String COMMENT_MARK = "#";
    public static final String NEW_CARD_PREFIX = "<";
    public static final String NEW_CARD_SUFFIX = ">";
    public static final String FILE_SUFFIX = ".bullshit";

    private DynamicGridView gridView;

    private BaseDynamicGridAdapter gridAdapter;

    private SelectDimensionDialogFragment dimensionDialog;
    private SaveCardDialogFragment saveCardDialog;
    private EditCellDialogFragment editCellDialog;

    boolean isEditing; //are we in edit mode?
    boolean isDirty; //are there unsaved changes?

    //current card dimension
    private int dim;
    private MenuItem newMenuItem;
    private MenuItem editMenuItem;
    private MenuItem saveAsMenuItem;
    private MenuItem acceptItemMenuItem;
    private MenuItem shareMenuItem;

    private MenuItem shuffleMenuItem;
    //the width and the height (not counting action bar) of the grid
    private int gridWidth;
    private int gridHeight;
    //this is constant, got from resources
    private float shift;

    //Offset between cells. This is calculated dynamically based on dim size
    private float offset;

    private CharSequence currentCardName;

    private boolean gridViewInitFinished; //have obtained gridWidth and gridHeight?
    private AdapterView.OnItemLongClickListener itemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (isEditing) {
                gridView.startEditMode(position);
            }
            return true;
        }
    };
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ActionBar actionBar;
    private SharedPreferences sharedPreferences;
    private File bullshitDir;
    private ArrayAdapter<String> cardListAdapter;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        gridViewInitFinished = false;
        super.onCreate(savedInstanceState);

        setNewCardName();

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        createDirIfNeeded();

        setContentView(R.layout.bingo_activity);

        actionBar = getActionBar();

        initActionBar();

        final ListView cardListView = (ListView) findViewById(R.id.left_drawer);
        cardListAdapter = new ArrayAdapter<>(this, R.layout.card, getCardNames());
        cardListView.setAdapter(cardListAdapter);
        cardListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String card = (String) parent.getItemAtPosition(position);
                List<String> words = getWordsForCard(card);
                if(words.size() == 0) {
                    Toast.makeText(BullshitBingoActivity.this, getResources().getString(R.string.error_empty_cart) + card, Toast.LENGTH_LONG).show();
                    return;
                }
                double sqrt = Math.sqrt(words.size());
                double floor = Math.floor(sqrt + 0.5);
                if(Math.abs(floor - sqrt) > 0.1) {
                    Toast.makeText(BullshitBingoActivity.this, getResources().getString(R.string.error_wrong_word_count) + card, Toast.LENGTH_LONG).show();
                    return;
                }

                dim = (int) Math.round(sqrt);
                currentCardName = card;
                actionBarTitleSaved();
                initBoardFromWords(getStringHolders(words));

                drawerLayout.closeDrawers();

                reloadCardList();

                invalidateOptionsMenu();
            }
        });
        gridView = (DynamicGridView) findViewById(R.id.gridview);

        offset = getResources().getDimension(R.dimen.cell_spacing);

        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridWidth = gridView.getWidth();
                gridHeight = gridView.getHeight();
                gridViewInitFinished = true;
            }
        });

        gridAdapter = new BaseDynamicGridAdapter(BullshitBingoActivity.this, new ArrayList<>(), dim) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                StringHolder text = (StringHolder) getItem(position);
                TextView textView;
                if (!(convertView instanceof TextView)) {
                    textView = (TextView) getLayoutInflater().inflate(R.layout.word, null);
                } else {
                    textView = (TextView) convertView;
                }
                textView.setWidth((int) (gridWidth / dim - shift));
                textView.setHeight((int) (gridHeight / dim - shift));

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
                isDirty = true;
                invalidateOptionsMenu();
            }
        });

        restoreFromBundle(savedInstanceState);
    }

    private void reloadCardList() {
        cardListAdapter.clear();
        cardListAdapter.addAll(getCardNames());
    }

    private List<String> getWordsForCard(String pureCard) {
        checkDir();
        File file = new File(bullshitDir, pureCard + FILE_SUFFIX);
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        ArrayList<String> result = new ArrayList<>();
        String word;
        try {
            while((word = br.readLine()) != null) {
                if(! word.startsWith(COMMENT_MARK)) {
                    result.add(word);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't read file " + file);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private List<String> getCardNames() {
        checkDir();
        String[] cards = bullshitDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(FILE_SUFFIX);
            }
        });
        ArrayList<String> result = new ArrayList<>(cards.length);
        for (int i = 0; i < cards.length; i++) {
            String card = cards[i];
            if (card.endsWith(FILE_SUFFIX)) {
                card = card.substring(0, card.indexOf(FILE_SUFFIX));
                result.add(card);
            }
        }
        return result;
    }

    private void setNewCardName() {
        currentCardName = NEW_CARD_PREFIX + getResources().getString(R.string.new_card_name) + NEW_CARD_SUFFIX;
    }

    private void initActionBar() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.app_name){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("");
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
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
            initBoardFromWords(getStringHolders(wordsArrayList));
        }
    }

    private ArrayList<StringHolder> getStringHolders(List<String> wordsArrayList) {
        ArrayList<StringHolder> currentWords = new ArrayList<>(wordsArrayList.size());
        for(String s: wordsArrayList) {
            currentWords.add(new StringHolder(s));
        }
        return currentWords;
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
        return gridAdapter != null && gridAdapter.getItems().size() > 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(! gridViewInitFinished) {
            return false;
        }
        if(drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        drawerLayout.closeDrawers();

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_new:
                showSelectNewCardDimensionDialog();
                return true;
            case R.id.action_edit:
                isEditing = true;
                prepareForEdit();
                return true;
            case R.id.action_save_as:
                if (saveCardDialog == null) {
                    saveCardDialog = new SaveCardDialogFragment();
                }
                saveCardDialog.show(getFragmentManager(), "saveCard");
                return true;
            case R.id.action_accept:
                exitEditMode();
                if(isPersisted()) {
                    persistWords(currentCardName);
                    isDirty = false;
                } else {
                    actionBarTitleNotSaved();
                }
                return true;
            case R.id.action_share:
                //todo
                return true;
            case R.id.action_shuffle:
                isDirty = true;
                //todo
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isPersisted() {
        return ! currentCardName.toString().startsWith(NEW_CARD_PREFIX);
    }

    private void actionBarTitleNotSaved() {
        actionBar.setTitle(currentCardName + "*");
        actionBar.setBackgroundDrawable(LIGHT_ERROR_COLOR);
    }

    private void actionBarTitleSaved() {
        actionBar.setTitle(currentCardName);
        actionBar.setBackgroundDrawable(null);
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
        saveAsMenuItem = menu.findItem(R.id.action_save_as);
        acceptItemMenuItem = menu.findItem(R.id.action_accept);
        shareMenuItem = menu.findItem(R.id.action_share);
        shuffleMenuItem = menu.findItem(R.id.action_shuffle);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isEditing) {
            newMenuItem.setVisible(false);
            editMenuItem.setVisible(false);
            saveAsMenuItem.setVisible(true);
            acceptItemMenuItem.setVisible(true);
            shareMenuItem.setVisible(false);
            shuffleMenuItem.setVisible(true);
        } else {
            newMenuItem.setVisible(true);
            editMenuItem.setVisible(isGridFilled());
            saveAsMenuItem.setVisible(isGridFilled() && isDirty);
            acceptItemMenuItem.setVisible(false);
            shareMenuItem.setVisible(isGridFilled() && isPersisted());
            shuffleMenuItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDimensionSelected(final int dim) {
        this.dim = dim;

        setNewCardName();

        actionBarTitleSaved();

        initCleanBoard();
    }

    private void initCleanBoard() {
        isDirty = true;
        ArrayList<StringHolder> currentWords = new ArrayList<>(dim*dim);
        for (int i = 0; i < dim; i++) {
            for(int j = 0; j < dim; j++) {
                currentWords.add(new StringHolder(""));
            }
        }

        initBoardFromWords(currentWords);
        invalidateOptionsMenu();
    }

    private void initBoardFromWords(final List<StringHolder> currentWords) {
        gridView.setNumColumns(dim);

        shift = offset * ((float) (dim - 2) / (dim));

        gridAdapter.set(currentWords);
        gridAdapter.setColumnCount(dim);
    }

    @Override
    public void onCellEditFinished(CharSequence newValue, int position) {
        gridAdapter.setItemAtPosition(new StringHolder(newValue), position);
        isDirty = true;
        invalidateOptionsMenu();
    }

    @Override
    public void onCardNamePopulated(CharSequence name) {
        persistWords(name);

        exitEditMode();
        isDirty = false;
        currentCardName = name;
        actionBarTitleSaved();
        reloadCardList();
    }

    private void persistWords(CharSequence fileName) {
        checkDir();

        File file = new File(bullshitDir, fileName.toString() + FILE_SUFFIX);
        if(file.exists()) {
            file.delete();
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            writer.append(COMMENT_MARK + getResources().getString(R.string.file_comment) + "\n");
            for(Object o: gridAdapter.getItems()) {
                String word = ((StringHolder) o).s.toString();
                writer.append(word + "\n");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't write to bullshitbingo file " + file);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(BullshitBingoActivity.class.getName(), "===Persisted card to " + file);
        }
    }

    private void checkDir() {
        if(bullshitDir == null || ! bullshitDir.exists()) {
            throw new IllegalStateException("Directory for saving files does not exist: " + bullshitDir);
        }
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
            Log.d(BullshitBingoActivity.class.getName(), "===Bullshit dir: " + bullshitDir);
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
