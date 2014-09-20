package com.denisk.bullshitbingochampion;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.tjeannin.apprate.AppRate;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BullshitBingoActivity extends Activity
        implements SelectDimensionDialogFragment.DimensionSelectedListener, EditCellDialogFragment.CellEditFinishedListener, SaveCardDialogFragment.SaveCardDialogListener {

    public static final String DIR_NAME = "bullshitbingochamp";

    public static final String BUNDLE_DIM = "dim";
    public static final String BUNDLE_WORDS = "words";
    public static final String BUNDLE_IS_EDITING = "isEditing";
    public static final String BUNDLE_CURRENT_CARD_NAME = "currentCard";
    public static final String BUNDLE_MARKS = "marks";
    public static final String BUNDLE_IS_PLAYING_BINGO = "isBingoPlaying";
    public static final String BUNDLE_IS_BINGO_ROW = "isBingoRow";
    public static final String BUNDLE_BINGO_INDEX = "bingoIndex";

    public static final String COMMENT_MARK = "#";
    public static final String NEW_CARD_PREFIX = "<";
    public static final String NEW_CARD_SUFFIX = ">";
    public static final String FILE_SUFFIX = ".bullshit";
    public static final float IDEAL_FONT_SIZE_PX_FOR_1280_800 = 120f;
    public static final double LANDSCAPE_WIDTH_HEIGHT_COEFF = 1280./800;

    public static final int FONT_STEP = 2;
    public static final String UTF_8 = "UTF-8";

    public static final int[] DEFAULT_CARDS = new int[]{
            R.raw._manager_talk_5x5,
            R.raw._standup_4x4,
            R.raw._top_manager_4x4,
            R.raw._ultimate_10x10,
            R.raw._everyday_status_3x3
    };
    public static final int SETTINGS_REQUEST_CODE = 1;

    private SharedPreferences sharedPreferences;

    private final static String FIRST_RUN_KEY = "firstRun";

    private DynamicGridView gridView;

    private BaseDynamicGridAdapter gridAdapter;

    private SelectDimensionDialogFragment dimensionDialog;
    private SaveCardDialogFragment saveCardDialog;
    private EditCellDialogFragment editCellDialog;

    boolean isEditing; //are we in edit mode?

    //current card dimension
    private int dim;
    private MenuItem newMenuItem;
    private MenuItem editMenuItem;
    private MenuItem saveAsMenuItem;
    private MenuItem acceptItemMenuItem;
    private MenuItem shareMenuItem;
    private MenuItem deleteMenuItem;
    private MenuItem settingsMenuItem;
    private MenuItem increaseFontMenuItem;
    private MenuItem decreaseFontMenuItem;

    private MenuItem shuffleMenuItem;
    //the width and the height (not counting action bar) of the grid
    private int gridWidth;
    private int gridHeight;
    //this is constant, got from resources
    private float shift;

    //Offset between cells. This is calculated dynamically based on dim size
    private float offset;

    private String currentCardName;

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
    private File bullshitDir;
    private ArrayAdapter<String> cardListAdapter;

    private float finalFontSize;

    private boolean[] cardState;
    private Vibrator vibrator;

    private boolean isPlayingBingo = false;
    private int bingoIndex = -1;
    private boolean isBingoRow; //if false then it is column
    private AnimatorSet bingoAnimatorSet;
    private TextView bingoMark;
    private TextView bingoTitle;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createDirIfNeeded();


        gridViewInitFinished = false;

        setContentView(R.layout.bingo_activity);

        actionBar = getActionBar();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        bingoMark = (TextView) findViewById(R.id.game_bingo_mark);
        bingoTitle = (TextView) findViewById(R.id.game_bingo_title);

        initActionBar();

        initCardList();

        initGridView();

        if(getIntent() != null && getIntent().getData() != null) {
            openFromIntent();
        } else {
            restoreFromBundle(savedInstanceState);
        }

        if(Boolean.TRUE.equals(sharedPreferences.getBoolean(FIRST_RUN_KEY, Boolean.TRUE)) && checkDir()) {
            sharedPreferences.edit().putBoolean(FIRST_RUN_KEY, Boolean.FALSE).apply();

            copyDefaultCards(DEFAULT_CARDS);

            reloadCardList();
        }

        new AppRate(this)
                .setMinDaysUntilPrompt(10)
                .setMinLaunchesUntilPrompt(15)
                .setShowIfAppHasCrashed(false)
                .init();
    }

    private void copyDefaultCards(int[] resources) {
        for (int resId: resources) {
            InputStream inputStream = getResources().openRawResource(resId);

            BufferedReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));

                File dest = new File(bullshitDir, getResources().getResourceEntryName(resId) + FILE_SUFFIX);
                if(dest.exists()) {
                    dest.delete();
                }
                writer = new BufferedWriter(new FileWriter(dest));

                String line;
                while((line = reader.readLine()) != null) {
                    writer.write(line + "\n");
                }
            } catch (IOException e) {
                Log.e(BullshitBingoActivity.class.getName(), "Can't create default cards", e);
                Toast.makeText(this, "Can't create default cards", Toast.LENGTH_LONG).show();
            } finally {
                closeQuietly(reader);
                closeQuietly(writer);
            }
        }
    }

    private void closeQuietly(Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class CardDynamicGridAdapter extends BaseDynamicGridAdapter implements View.OnTouchListener {
         protected CardDynamicGridAdapter(Context context, int columnCount) {
             super(context, columnCount);
         }

         public CardDynamicGridAdapter(Context context, List<?> items, int columnCount) {
             super(context, items, columnCount);
         }

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
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalFontSize);
            textView.setTranslationX(0);
            textView.setTranslationY(0);

            setCardColor(position, textView);

            setViewVisibilityOnPosition(position, textView);

            textView.setOnTouchListener(this);

            textView.setTag(position);

            return textView;
        }


         @Override
         public boolean onTouch(View view, MotionEvent event) {
             if (event.getAction() == MotionEvent.ACTION_DOWN) {
                 int position = (int) view.getTag();
                 if (!isEditing) {
                     if(isPlayingBingo) {
                         cancelBingoAnimation();

                         return false;
                     }
                     cardState[position] = !cardState[position];
                     setCardColor(position, view);
                     if (shouldVibrate()) {
                         vibrator.vibrate(30);
                     }
                     //look for bingo among columns and lines
                     for(int i = 0; i < dim; i++) {
                         boolean bingo = true;
                         //check i-th row for bingo
                         for(int j = 0; j < dim; j++) {
                            bingo &= cardState[i*dim + j];
                         }
                         if(bingo) {
                             isBingoRow = true;
                             bingoIndex = i;

                             animateBingoIfNeeded();

                             return true;
                         }
                         //check i-th column for bingo
                         bingo = true;
                         for(int j = 0; j < dim; j++) {
                             bingo &= cardState[j*dim + i];
                         }
                         if(bingo) {
                             isBingoRow = false;
                             bingoIndex = i;

                             animateBingoIfNeeded();

                             return true;
                         }
                     }
                     return true;
                 }

                 return false;
             }
             return false;
         }
     }

    private void cancelBingoAnimation() {
        if(bingoAnimatorSet != null) {
            bingoAnimatorSet.cancel();
        }
        bingoMark.setVisibility(View.INVISIBLE);
        bingoTitle.setVisibility(View.INVISIBLE);

        cancelVibration();

        isPlayingBingo = false;

        bingoIndex = -1;
    }

    private void animateBingoIfNeeded() {
        if(bingoIndex < 0) {
            return;
        }
        isPlayingBingo = true;

        int width;
        int height;
        int leftMargin;
        int topMargin;
        if(isBingoRow) {
            for (int j = 0; j < dim; j++) {
                cardState[bingoIndex * dim + j] = false;
            }


            width = gridWidth;
            height = gridHeight / dim;
            leftMargin = 0;
            topMargin = (gridHeight / dim) * bingoIndex;
        } else {
            for(int j = 0; j < dim; j++) {
                cardState[j*dim + bingoIndex] = false;
            }

            width = gridWidth / dim;
            height = gridHeight;
            leftMargin = (gridWidth / dim) * bingoIndex;
            topMargin = 0;
        }

        gridAdapter.notifyDataSetChanged();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bingoMark.getLayoutParams();
        layoutParams.setMargins(leftMargin, topMargin, 0, 0);
        layoutParams.width = width;
        layoutParams.height = height;

        bingoMark.setAlpha(0);

        ObjectAnimator bingoMarkAnimator = ObjectAnimator.ofFloat(bingoMark, "alpha", 0, 1);
        bingoMarkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        bingoMarkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        bingoMarkAnimator.setDuration(400);


        TypedValue titleFinalSize = new TypedValue();
        getResources().getValue(R.dimen.bingo_title_size, titleFinalSize, true);

        ValueAnimator textAnimator = ValueAnimator.ofFloat(0, titleFinalSize.getFloat());
        textAnimator.setDuration(1000);
        textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bingoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (Float) animation.getAnimatedValue());
            }
        });

        bingoAnimatorSet = new AnimatorSet();
        bingoAnimatorSet.playTogether(bingoMarkAnimator, textAnimator);

        bingoMark.setVisibility(View.VISIBLE);
        bingoTitle.setVisibility(View.VISIBLE);

        bingoAnimatorSet.start();
        if(shouldVibrate()) {
            vibrator.vibrate(VibrationPatterns.PUTIN_PATTERN, -1);
        }
    }

    @Override
    protected void onPause() {
        cancelVibration();
        super.onPause();
    }

    private void cancelVibration() {
        vibrator.cancel();
    }

    private boolean shouldVibrate() {
        return sharedPreferences.getBoolean("pref_vibrate", true);
    }

    private void initGridView() {
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

        gridAdapter = new CardDynamicGridAdapter(BullshitBingoActivity.this, new ArrayList<>(), dim);

        gridView.setAdapter(gridAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, long id) {
                if(! isEditing) {
                    return;
                }
                StringHolder itemAtPosition = (StringHolder) gridView.getItemAtPosition(position);
                if (itemAtPosition == null) {
                    return;
                }
                CharSequence currentCellValue = itemAtPosition.s;

                if (editCellDialog == null) {
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
                invalidateOptionsMenu();
            }
        });
    }

    private void setCardColor(int position, View view) {
        int background;
        if(cardState[position]) {
            background = R.drawable.back_selected;
        } else {
            background = R.drawable.back;
        }

        view.setBackgroundResource(background);
    }

    private void initCardList() {
        final ListView cardListView = (ListView) findViewById(R.id.left_drawer);
        cardListAdapter = new ArrayAdapter<>(this, R.layout.card_name, getCardNames());
        cardListView.setAdapter(cardListAdapter);
        cardListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cancelBingoAnimation();
                String card = (String) parent.getItemAtPosition(position);
                List<String> words = getWordsForCard(card);
                if(words == null) {
                    return;
                }
                isEditing = false;
                setDimAndRenderWords(card, words);
            }
        });
        cardListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteCardDialog((CharSequence) parent.getItemAtPosition(position));
                return true;
            }
        });
    }

    private void openFromIntent() {
        Uri data = getIntent().getData();

        String cardName = null;
        String scheme = data.getScheme();
        if (scheme.equals("file")) {
            cardName = data.getLastPathSegment();
        } else if (scheme.equals("content")) {
            //known column names. Need to add more for more apps to support
            String[] knownColNames = {
                    "_display_name", //gmail
                    "filename"      //evernote
            };
            Cursor cursor = getContentResolver().query(data, null, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                for(String p: knownColNames) {
                    int columnIndex = cursor.getColumnIndex(p);
                    if(columnIndex > -1) {
                        cardName = cursor.getString(columnIndex);
                        break;
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        if(cardName == null) {
            cardName = "imported_" + System.currentTimeMillis();
        }
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(data);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Can't open input stream to data", Toast.LENGTH_SHORT).show();
        }

        if(inputStream != null) {
            if (cardName.contains(FILE_SUFFIX)) {
                cardName = cardName.substring(0, cardName.indexOf(FILE_SUFFIX));
            }
            List<String> words = readCardFromInputStream(inputStream);
            if(words == null) {
                return;
            }
            if(!setDimAndRenderWords(cardName, words)) {
                return;
            }
            initBoardFromWords(getStringHolders(words));

            persistWords(cardName);
            reloadCardList();
        }
    }

    private boolean setDimAndRenderWords(String card, List<String> words) {
        if(words.size() == 0) {
            Toast.makeText(this, getResources().getString(R.string.error_empty_cart) + card, Toast.LENGTH_LONG).show();
            return false;
        }
        double sqrt = Math.sqrt(words.size());
        double floor = Math.floor(sqrt + 0.5);
        if(Math.abs(floor - sqrt) > 0.1) {
            Toast.makeText(this, getResources().getString(R.string.error_wrong_word_count) + card, Toast.LENGTH_LONG).show();
            return false;
        }

        dim = (int) Math.round(sqrt);

        initCardState();

        currentCardName = card;
        updateTitle();
        initBoardFromWords(getStringHolders(words));

        drawerLayout.closeDrawers();

        reloadCardList();

        invalidateOptionsMenu();

        return true;
    }

    private void initCardState() {
        cardState = new boolean[dim*dim];
    }

    private void reloadCardList() {
        cardListAdapter.clear();
        cardListAdapter.addAll(getCardNames());
        cardListAdapter.notifyDataSetChanged();
    }

    private List<String> getWordsForCard(String pureCard) {
        if (! checkDir()) {
            return new ArrayList<>();
        }
        File file = new File(bullshitDir, pureCard + FILE_SUFFIX);
        FileInputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.error_cant_read_file) + file, Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
        return readCardFromInputStream(is);
    }

    private List<String> readCardFromInputStream(InputStream is) {
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(is, UTF_8));
        } catch (UnsupportedEncodingException e) {
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
            Log.e(BullshitBingoActivity.class.getName(), "Can't open card from input stream", e);
            Toast.makeText(this, R.string.error_wrong_card_format, Toast.LENGTH_SHORT).show();
            return null;
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
        if (! checkDir()) {
            return new ArrayList<>();
        }
        String[] cards = bullshitDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(FILE_SUFFIX);
            }
        });
        ArrayList<String> result = new ArrayList<>(cards.length);
        for (String card: cards) {
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
            currentCardName = savedInstanceState.getString(BUNDLE_CURRENT_CARD_NAME);
            cardState = savedInstanceState.getBooleanArray(BUNDLE_MARKS);
            isPlayingBingo = savedInstanceState.getBoolean(BUNDLE_IS_PLAYING_BINGO);
            isBingoRow = savedInstanceState.getBoolean(BUNDLE_IS_BINGO_ROW);
            bingoIndex = savedInstanceState.getInt(BUNDLE_BINGO_INDEX);
            updateTitle();
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

        outState.putStringArrayList(BUNDLE_WORDS, getStringListFromCurrentWords());
        outState.putInt(BUNDLE_DIM, dim);
        outState.putBoolean(BUNDLE_IS_EDITING, isEditing);
        outState.putString(BUNDLE_CURRENT_CARD_NAME, currentCardName);
        outState.putBooleanArray(BUNDLE_MARKS, cardState);
        outState.putBoolean(BUNDLE_IS_PLAYING_BINGO, isPlayingBingo);
        outState.putBoolean(BUNDLE_IS_BINGO_ROW, isBingoRow);
        outState.putInt(BUNDLE_BINGO_INDEX, bingoIndex);
    }

    private ArrayList<String> getStringListFromCurrentWords() {
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
        cancelBingoAnimation();

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_new:
                showSelectNewCardDimensionDialog();
                return true;
            case R.id.action_edit:
                prepareForEdit();
                return true;
            case R.id.action_save_as:
                if (saveCardDialog == null) {
                    saveCardDialog = new SaveCardDialogFragment();
                }
                saveCardDialog.show(getFragmentManager(), "saveCard");
                return true;
            case R.id.action_accept:
                accept();
                return true;
            case R.id.action_share:
                shareCurrentCard();
                return true;
            case R.id.action_delete:
                showDeleteCardDialog(currentCardName);
                return true;
            case R.id.action_shuffle:
                initCardState();
                gridView.shuffle();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivityForResult(intent, SETTINGS_REQUEST_CODE);
                return true;
            case R.id.action_increase_font:
                finalFontSize += FONT_STEP;
                persistFont();

                return true;
            case R.id.action_decrease_font:
                if (finalFontSize > 3) {
                    finalFontSize -= FONT_STEP;
                    persistFont();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SETTINGS_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                if(data != null && data.hasExtra(PreferencesActivity.RELOAD_DEFAULT_CARDS)) {
                    copyDefaultCards(DEFAULT_CARDS);
                    reloadCardList();

                    for(int id: DEFAULT_CARDS) {
                        String name = getResources().getResourceEntryName(id);
                        if (clearCardIfCurrentCardNameEquals(name)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void persistFont() {
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        String prefix = getFontPrefix(land);

        String key = getFontKey(dim, prefix);

        sharedPreferences.edit().putFloat(key, finalFontSize).commit();

        gridAdapter.notifyDataSetChanged();
    }

    private void accept() {
        exitEditMode();
        if(isPersisted()) {
            persistWords(currentCardName);
        } else if(! currentCardName.endsWith("*")) {
            currentCardName += "*";
            updateTitle();
        }
    }

    private void shareCurrentCard() {
        if (! checkDir()) {
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String cardName = currentCardName + FILE_SUFFIX;
        String title = getResources().getString(R.string.share_title) + " " + cardName;
        sendIntent.putExtra(Intent.EXTRA_TITLE, title);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        File file = new File(bullshitDir, cardName);
        if(! file.exists()) {
            throw new IllegalStateException("No bullshit file: " + file);
        }
        //todo this seems to be useless, gmail strips it
        sendIntent.setType("text/bullshit");
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_with)));
    }

    private void showDeleteCardDialog(final CharSequence cardName) {
        DialogInterface.OnClickListener deleteCardListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (! checkDir()) {
                            return;
                        }
                        File cardFile = new File(bullshitDir, cardName + FILE_SUFFIX);
                        cardFile.delete();

                        reloadCardList();

                        clearCardIfCurrentCardNameEquals(cardName);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //do nothing
                        break;
                }

            }
        };
        new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.delete_prompt)+ " " + cardName)
                .setPositiveButton(R.string.ok, deleteCardListener)
                .setNegativeButton(R.string.cancel, deleteCardListener)
                .show();
    }

    private boolean clearCardIfCurrentCardNameEquals(CharSequence cardName) {
        if (currentCardName != null && currentCardName.equals(cardName)) {
            dim = 0;
            initCleanBoard();
            currentCardName = "";
            updateTitle();

            return true;
        }
        return false;
    }

    private boolean isPersisted() {
        return !currentCardName.startsWith(NEW_CARD_PREFIX);
    }

    private void updateTitle() {
        actionBar.setTitle(currentCardName);
    }

    private void prepareForEdit() {
        isEditing = true;
        Toast.makeText(this, R.string.long_press_to_drag, Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
        gridView.setOnItemLongClickListener(itemLongClickListener);
        initCardState();
        gridAdapter.notifyDataSetChanged();
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
        deleteMenuItem = menu.findItem(R.id.action_delete);
        settingsMenuItem = menu.findItem(R.id.action_settings);
        increaseFontMenuItem = menu.findItem(R.id.action_increase_font);
        decreaseFontMenuItem = menu.findItem(R.id.action_decrease_font);

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
            deleteMenuItem.setVisible(false);
            settingsMenuItem.setVisible(true);
            increaseFontMenuItem.setVisible(true);
            decreaseFontMenuItem.setVisible(true);
        } else {
            newMenuItem.setVisible(true);
            editMenuItem.setVisible(isGridFilled());
            saveAsMenuItem.setVisible(isGridFilled());
            acceptItemMenuItem.setVisible(false);
            shareMenuItem.setVisible(isGridFilled() && isPersisted());
            shuffleMenuItem.setVisible(false);
            deleteMenuItem.setVisible(isGridFilled() && isPersisted());
            settingsMenuItem.setVisible(true);
            increaseFontMenuItem.setVisible(isGridFilled());
            decreaseFontMenuItem.setVisible(isGridFilled());
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDimensionSelected(final int dim) {
        this.dim = dim;

        cancelBingoAnimation();

        initCardState();
        //go into edit mode immediately
        prepareForEdit();

        setNewCardName();

        updateTitle();

        initCleanBoard();
    }

    private void initCardFontSize(int dim) {
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        String prefix = getFontPrefix(land);

        String key = getFontKey(dim, prefix);
        float fontSize = sharedPreferences.getFloat(key, -1);

        if (fontSize < 0) {
            finalFontSize = IDEAL_FONT_SIZE_PX_FOR_1280_800 /dim;
            if(land) {
                finalFontSize *= LANDSCAPE_WIDTH_HEIGHT_COEFF;
            }

            sharedPreferences.edit().putFloat(key, finalFontSize).commit();
        } else {
            finalFontSize = fontSize;
        }
    }

    private String getFontKey(int dim, String prefix) {
        return prefix + dim;
    }

    private String getFontPrefix(boolean land) {
        return land ? "land" : "portrait";
    }

    private void initCleanBoard() {
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
        initCardFontSize(dim);

        gridView.setNumColumns(dim);

        shift = offset * ((float) (dim - 2) / (dim));

        gridAdapter.setColumnCount(dim);

        gridAdapter.set(currentWords);
        invalidateOptionsMenu();
        //hack: need to repeat it, otherwise it doesn't work when rotating the screen
        gridView.post(new Runnable() {
            public void run() {
                gridAdapter.set(currentWords);
                animateBingoIfNeeded();
            }
        });
    }

    @Override
    public void onCellEditFinished(CharSequence newValue, int position) {
        gridAdapter.setItemAtPosition(new StringHolder(newValue), position);
        invalidateOptionsMenu();
    }

    @Override
    public void onCardNamePopulated(String name) {
        persistWords(name);

        exitEditMode();
        currentCardName = name;
        updateTitle();
        reloadCardList();
    }

    private void persistWords(CharSequence fileName) {
        if (! checkDir()) {
            return;
        }

        File file = new File(bullshitDir, fileName.toString() + FILE_SUFFIX);
        if(file.exists()) {
            file.delete();
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UTF_8));
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

    private boolean checkDir() {
        if(bullshitDir == null || ! bullshitDir.exists()) {
            Toast.makeText(this, getString(R.string.error_directory_does_not_exist) + bullshitDir, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
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

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("s=").append(s);
            return sb.toString();
        }
    }
    @Override
    public void onBackPressed() {
        if (isEditing) {
            accept();
        } else if(isPlayingBingo) {
            cancelBingoAnimation();
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
