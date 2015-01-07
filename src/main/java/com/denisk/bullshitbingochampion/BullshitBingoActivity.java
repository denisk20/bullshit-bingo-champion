package com.denisk.bullshitbingochampion;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.tjeannin.apprate.AppRate;
import de.cketti.library.changelog.ChangeLog;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BullshitBingoActivity extends Activity
        implements SelectDimensionDialogFragment.DimensionSelectedListener, EditCellDialogFragment.CellEditFinishedListener, SaveCardDialogFragment.SaveCardDialogListener {

    public static final String DIR_NAME = "bullshitbingochamp";

    public static final String BUNDLE_DIM = "dim";
    public static final String BUNDLE_WORDS = "words";
    public static final String BUNDLE_IS_EDITING = "isEditing";
    public static final String BUNDLE_CURRENT_CARD_NAME = "currentCard";
    public static final String BUNDLE_CARD_STATES = "cardStates";
    public static final String BUNDLE_IS_PLAYING_BINGO = "isBingoPlaying";
    public static final String BUNDLE_IS_BINGO_ROW = "isBingoRow";
    public static final String BUNDLE_BINGO_INDEX = "bingoIndex";

    public static final String COMMENT_MARK = "#";
    public static final String DELIMITER_MARK = ">";
    public static final String NEW_CARD_PREFIX = "<";
    public static final String NEW_CARD_SUFFIX = ">";
    public static final String FILE_SUFFIX = ".bullshit";

    private static final int IMAGE_LONG_EDGE = 1280;
    private static final int IMAGE_SHORT_EDGE = 800;

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
    public static final String IMAGE_PNG = "image/png";
    public static final int CARD_VIBRATION_MILLIS = 30;
    public static final int BINGO_MARK_ANIMATION_DURATION = 400;
    public static final int BINGO_TEXT_ANIMATION_DURATION = 1000;

    private SharedPreferences sharedPreferences;

    private final static String FIRST_ONCREATE_KEY = "firstOnCreate";
    private final static String FIRST_DRAWER_OPEN_KEY = "firstDrawerOpen";

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
    private MenuItem shareBullshitMenuItem;
    private MenuItem shareImageMenuItem;
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

    /**
     * Whether current card was modified
     */
    private boolean isDirty = false;

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

    private Vibrator vibrator;

    private boolean isBingoAnimationPlaying = false;
    private int bingoIndex = -1;
    private boolean isBingoRow; //if false then it is column
    private AnimatorSet bingoAnimatorSet;
    private TextView bingoMark;
    private TextView bingoTitle;
    private Button shareButton;
    //colors
    private CustomShapeDrawable selectedCardDrawable;
    private CustomShapeDrawable unselectedCardDrawable;
    private CustomShapeDrawable bingoDrawable;
    private int unselectedCardTextColor;
    private int selectedCardTextColor;
    private int bingoTextColor;

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
        shareButton = (Button) findViewById(R.id.share_button);
        shareButton.setVisibility(View.GONE);

        initActionBar();

        initCardList();

        initGridView();

        if (getIntent() != null && getIntent().getData() != null) {
            openFromIntent();
        } else {
            restoreFromBundle(savedInstanceState);
        }

        if ((!sharedPreferences.contains(FIRST_ONCREATE_KEY)) && checkDir()) {
            sharedPreferences.edit().putBoolean(FIRST_ONCREATE_KEY, false).apply();

            copyDefaultCards(DEFAULT_CARDS);

            reloadCardList();
        }

        new AppRate(this)
                .setMinDaysUntilPrompt(10)
                .setMinLaunchesUntilPrompt(15)
                .setShowIfAppHasCrashed(false)
                .init();

        ChangeLog cl = new ChangeLog(this);

        if (cl.isFirstRun()) {
            cl.getFullLogDialog().show();
        }
    }

    //todo remove this
    private int[] getHitsCount() {
        int res[] = new int[gridAdapter.getCount()];
        for (int i = 0; i < gridAdapter.getCount(); i++) {
            WordAndHits w = (WordAndHits) gridAdapter.getItem(i);
            res[i] = w.hits;
        }

        return res;
    }

    private void copyDefaultCards(int[] resources) {
        for (int resId : resources) {
            InputStream inputStream = getResources().openRawResource(resId);

            BufferedReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));

                File dest = new File(bullshitDir, getResources().getResourceEntryName(resId) + FILE_SUFFIX);
                if (dest.exists()) {
                    dest.delete();
                }
                writer = new BufferedWriter(new FileWriter(dest));

                String line;
                while ((line = reader.readLine()) != null) {
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

    private WordAndHits getWordDataAtPosition(int pos) {
        return (WordAndHits) gridAdapter.getItem(pos);
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
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
            WordAndHits wordAndHits = (WordAndHits) getItem(position);
            TextView textView;
            if (!(convertView instanceof TextView)) {
                textView = (TextView) getLayoutInflater().inflate(R.layout.word, parent, false);
            } else {
                textView = (TextView) convertView;
            }
            textView.setWidth((int) (gridWidth / dim - shift));
            textView.setHeight((int) (gridHeight / dim - shift));

            textView.setText(wordAndHits.word);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalFontSize);
            textView.setTranslationX(0);
            textView.setTranslationY(0);

            setCardColors(position, textView);

            textView.setOnTouchListener(this);

            textView.setTag(position);

            return textView;
        }


        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if(isEditing) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int position = (int) view.getTag();
                if (isBingoAnimationPlaying) {
                    cancelBingoAnimation();

                    return false;
                }
                isDirty = true;


                WordAndHits wordDataAtPosition = getWordDataAtPosition(position);
                //todo modify this to increase the number of hits
                wordDataAtPosition.hits = (wordDataAtPosition.hits == 0 ? 1 : 0);

                int hitsCount[] = getHitsCount();

                setCardColors(position, (TextView) view);
                if (shouldVibrate()) {
                    vibrator.vibrate(CARD_VIBRATION_MILLIS);
                }
                //todo use BingoData
                //look for bingo among columns and rows
                for (int i = 0; i < dim; i++) {
                    boolean bingo = true;
                    //check i-th row for bingo
                    for (int j = 0; j < dim; j++) {
                        bingo &= (hitsCount[i * dim + j] > 0);
                    }
                    if (bingo) {
                        isBingoRow = true;
                        bingoIndex = i;

                        animateBingoIfNeeded();

                        return true;
                    }
                    //check i-th column for bingo
                    bingo = true;
                    for (int j = 0; j < dim; j++) {
                        bingo &= (hitsCount[j * dim + i] > 0);
                    }
                    if (bingo) {
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
    }

    private void cancelBingoAnimation() {
        if (bingoAnimatorSet != null) {
            bingoAnimatorSet.cancel();
        }
        if (isBingoAnimationPlaying) {
            if (isBingoRow) {
                for (int j = 0; j < dim; j++) {
                    getWordDataAtPosition(bingoIndex * dim + j).hits = 0;
                }
            } else {
                for (int j = 0; j < dim; j++) {
                    getWordDataAtPosition(j * dim + bingoIndex).hits = 0;
                }
            }
            gridAdapter.notifyDataSetChanged();
        }

        bingoMark.setVisibility(View.INVISIBLE);
        bingoTitle.setVisibility(View.INVISIBLE);

        cancelVibration();

        isBingoAnimationPlaying = false;

        bingoIndex = -1;
        toggleShareButton();
    }

    private void animateBingoIfNeeded() {
        if (bingoIndex < 0) {
            return;
        }
        isBingoAnimationPlaying = true;

        int width;
        int height;
        int leftMargin;
        int topMargin;
        if (isBingoRow) {
            width = gridWidth;
            height = gridHeight / dim;
            leftMargin = 0;
            topMargin = (gridHeight / dim) * bingoIndex;
        } else {
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
        bingoMark.setBackgroundDrawable(bingoDrawable);

        ObjectAnimator bingoMarkAnimator = ObjectAnimator.ofFloat(bingoMark, "alpha", 0, 0.8f);
        bingoMarkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        bingoMarkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        bingoMarkAnimator.setDuration(BINGO_MARK_ANIMATION_DURATION);


        TypedValue titleFinalSize = new TypedValue();
        getResources().getValue(R.dimen.bingo_title_size, titleFinalSize, true);

        ValueAnimator textAnimator = ValueAnimator.ofFloat(0, titleFinalSize.getFloat());
        textAnimator.setDuration(BINGO_TEXT_ANIMATION_DURATION);
        textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bingoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (Float) animation.getAnimatedValue());
            }
        });

        bingoAnimatorSet = new AnimatorSet();
        bingoAnimatorSet.playTogether(bingoMarkAnimator, textAnimator);

        bingoTitle.setTextColor(bingoTextColor);

        bingoMark.setVisibility(View.VISIBLE);
        bingoTitle.setVisibility(View.VISIBLE);

        bingoAnimatorSet.start();
        if (shouldVibrate()) {
            vibrator.vibrate(VibrationPatterns.PUTIN_PATTERN, -1);
        }

        toggleShareButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initColors();
    }

    @Override
    protected void onPause() {
        cancelVibration();
        persistIfNeeded();
        super.onPause();
    }

    private void persistIfNeeded() {
        cancelBingoAnimation();
        if (isDirty && isWrittenToDisk()) {
            persistWords(currentCardName);
        }
    }

    private void cancelVibration() {
        vibrator.cancel();
    }

    private boolean shouldVibrate() {
        return sharedPreferences.getBoolean(getString(R.string.pref_vibrate_key), true);
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
                if (!isEditing) {
                    return;
                }
                WordAndHits itemAtPosition = (WordAndHits) gridView.getItemAtPosition(position);
                if (itemAtPosition == null) {
                    return;
                }

                CharSequence currentCellValue = itemAtPosition.word;

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

    private void initColors() {
        Resources r = getResources();
        gridView.setBackgroundColor(getGridColor());

        int strokeWidth = r.getDimensionPixelSize(R.dimen.card_border_width);

        selectedCardDrawable = new CustomShapeDrawable(new RectShape(),
                strokeWidth, sharedPreferences.getInt(getString(R.string.pref_color_card_border_selected_key), r.getColor(R.color.default_card_border_selected)));
        selectedCardDrawable.getPaint().setColor(getSelectedCardBackgroundColor());

        unselectedCardDrawable = new CustomShapeDrawable(new RectShape(),
                strokeWidth, sharedPreferences.getInt(getString(R.string.pref_color_card_border_key), r.getColor(R.color.default_card_border)));
        unselectedCardDrawable.getPaint().setColor(getUnselectedCardBackgroundColor());

        gridAdapter.notifyDataSetChanged();

        unselectedCardTextColor = sharedPreferences.getInt(getString(R.string.pref_color_card_text_key), r.getColor(R.color.default_card_text));
        selectedCardTextColor = sharedPreferences.getInt(getString(R.string.pref_color_card_text_selected_key), r.getColor(R.color.default_card_text_selected));
        
        bingoTextColor = sharedPreferences.getInt(getString(R.string.pref_color_bingo_text_key), r.getColor(R.color.default_bingo_text));

        int bingoBorderWidth = r.getDimensionPixelSize(R.dimen.bingo_border_width);
        int bingoStrokeColor = getBingoStrokeColor();
        bingoDrawable = new CustomShapeDrawable(new RectShape(),
                bingoBorderWidth, bingoStrokeColor);
        bingoDrawable.getPaint().setColor(sharedPreferences.getInt(getString(R.string.pref_color_bingo_background_key), r.getColor(R.color.default_bingo_background)));
    }

    private int getUnselectedCardBackgroundColor() {
        return sharedPreferences.getInt(getString(R.string.pref_color_card_background_key), getResources().getColor(R.color.default_card_background));
    }

    private int getSelectedCardBackgroundColor() {
        return sharedPreferences.getInt(getString(R.string.pref_color_card_background_selected_key), getResources().getColor(R.color.default_card_background_selected));
    }

    private int getGridColor() {
        return sharedPreferences.getInt(getString(R.string.pref_color_grid_key), getResources().getColor(R.color.default_grid));
    }

    private int getBingoStrokeColor() {
        return sharedPreferences.getInt(getString(R.string.pref_color_bingo_frame_key), getResources().getColor(R.color.default_bingo_frame));
    }

    private void setCardColors(int position, TextView view) {
        Drawable background;
        int textColor;
        if (getWordDataAtPosition(position).hits > 0) {
            background = selectedCardDrawable;
            textColor = selectedCardTextColor;
        } else {
            background = unselectedCardDrawable;
            textColor = unselectedCardTextColor;
        }

        view.setBackgroundDrawable(background);
        view.setTextColor(textColor);
    }

    private void initCardList() {
        final ListView cardListView = (ListView) findViewById(R.id.left_drawer);
        cardListAdapter = new ArrayAdapter<>(this, R.layout.card_name, getCardNames());
        cardListView.setAdapter(cardListAdapter);
        cardListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String card = (String) parent.getItemAtPosition(position);

                if (card.equals(currentCardName)) {
                    drawerLayout.closeDrawers();
                    return;
                }

                persistIfNeeded();

                List<WordAndHits> words = getWordsForCard(card);
                if (words == null) {
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
                for (String p : knownColNames) {
                    int columnIndex = cursor.getColumnIndex(p);
                    if (columnIndex > -1) {
                        cardName = cursor.getString(columnIndex);
                        break;
                    }
                }
            }
            closeQuietly(cursor);
        }
        if (cardName == null) {
            cardName = "imported_" + System.currentTimeMillis();
        }
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(data);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Can't open input stream to data", Toast.LENGTH_SHORT).show();
        }

        if (inputStream != null) {
            if (cardName.contains(FILE_SUFFIX)) {
                cardName = cardName.substring(0, cardName.indexOf(FILE_SUFFIX));
            }
            List<WordAndHits> words = readCardFromInputStream(inputStream);
            if (words == null) {
                return;
            }
            if (!setDimAndRenderWords(cardName, words)) {
                return;
            }
            initBoardFromWords(words);

            persistWords(cardName);
            reloadCardList();
        }
    }

    private boolean setDimAndRenderWords(String card, List<WordAndHits> words) {
        int size = words.size();
        if (size == 0) {
            Toast.makeText(this, getResources().getString(R.string.error_empty_card, card), Toast.LENGTH_LONG).show();
            return false;
        }
        int sqrt = Util.getDim(size);
        if(sqrt < 0) {
            Toast.makeText(this, getString(R.string.error_wrong_word_count, card), Toast.LENGTH_LONG).show();
            return false;
        }

        dim = sqrt;

        resetCardState();

        currentCardName = card;
        updateTitle();
        initBoardFromWords(words);

        drawerLayout.closeDrawers();

        reloadCardList();

        invalidateOptionsMenu();

        return true;
    }

    private void resetCardState() {
        for (Object o : gridAdapter.getItems()) {
            WordAndHits wordAndHits = (WordAndHits) o;
            wordAndHits.hits = 0;
        }
    }

    private void reloadCardList() {
        cardListAdapter.clear();
        cardListAdapter.addAll(getCardNames());
        cardListAdapter.notifyDataSetChanged();
    }

    private List<WordAndHits> getWordsForCard(String pureCard) {
        if (!checkDir()) {
            return new ArrayList<>();
        }
        File file = new File(bullshitDir, pureCard + FILE_SUFFIX);
        FileInputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.error_cant_read_file, file.toString()), Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
        return readCardFromInputStream(is);
    }

    private List<WordAndHits> readCardFromInputStream(InputStream is) {
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(is, UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        ArrayList<WordAndHits> result = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.startsWith(COMMENT_MARK)) {
                    continue;
                }
                /*
                   we expect the lines to be in format
                   'word > 3'
                   where 3 is the number of occurrences of the word
                 */
                String[] parts = line.split(DELIMITER_MARK);
                String word = parts[0].trim();
                //how many times
                int hits = 0;
                if (parts.length > 1) {
                    try {
                        hits = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        String message = getString(R.string.error_cant_parse_occurrences, word, parts[1].trim());
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        Log.e(BullshitBingoActivity.class.getName(), message);
                    }
                }
                WordAndHits wordAndHits = new WordAndHits(word, hits);
                result.add(wordAndHits);
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
        if (!checkDir()) {
            return new ArrayList<>();
        }
        String[] cards = bullshitDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(FILE_SUFFIX);
            }
        });
        ArrayList<String> result = new ArrayList<>(cards.length);
        for (String card : cards) {
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
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {
                if (!sharedPreferences.contains(FIRST_DRAWER_OPEN_KEY)) {
                    sharedPreferences.edit().putBoolean(FIRST_DRAWER_OPEN_KEY, false).apply();
                    Toast.makeText(BullshitBingoActivity.this, getString(R.string.drawer_first_open), Toast.LENGTH_LONG).show();
                }
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
            int[] hitsCount = savedInstanceState.getIntArray(BUNDLE_CARD_STATES);
            isBingoAnimationPlaying = savedInstanceState.getBoolean(BUNDLE_IS_PLAYING_BINGO);
            isBingoRow = savedInstanceState.getBoolean(BUNDLE_IS_BINGO_ROW);
            bingoIndex = savedInstanceState.getInt(BUNDLE_BINGO_INDEX);
            updateTitle();
            if (isEditing) {
                prepareForEdit();
            }
            ArrayList<String> wordsArrayList = savedInstanceState.getStringArrayList(BUNDLE_WORDS);
            if (wordsArrayList == null) {
                return;
            }
            ArrayList<WordAndHits> wordAndHits = new ArrayList<>(hitsCount.length);
            for (int i = 0; i < hitsCount.length; i++) {
                wordAndHits.add(i, new WordAndHits(wordsArrayList.get(i), hitsCount[i]));
            }

            initBoardFromWords(wordAndHits);

            toggleShareButton();
        }
    }

    private void toggleShareButton() {
        shareButton.setVisibility(isBingoAnimationPlaying ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(BUNDLE_WORDS, getStringListFromCurrentWords());
        outState.putInt(BUNDLE_DIM, dim);
        outState.putBoolean(BUNDLE_IS_EDITING, isEditing);
        outState.putString(BUNDLE_CURRENT_CARD_NAME, currentCardName);
        outState.putIntArray(BUNDLE_CARD_STATES, getHitsCount());
        outState.putBoolean(BUNDLE_IS_PLAYING_BINGO, isBingoAnimationPlaying);
        outState.putBoolean(BUNDLE_IS_BINGO_ROW, isBingoRow);
        outState.putInt(BUNDLE_BINGO_INDEX, bingoIndex);
    }

    private ArrayList<String> getStringListFromCurrentWords() {
        ArrayList<String> words = new ArrayList<>();
        for (Object o : gridAdapter.getItems()) {
            WordAndHits h = (WordAndHits) o;
            words.add(h.word);
        }

        return words;
    }

    private boolean isGridFilled() {
        return gridAdapter != null && gridAdapter.getItems().size() > 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!gridViewInitFinished) {
            return false;
        }

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        drawerLayout.closeDrawers();
        if (item.getItemId() != R.id.action_share_image) {
            //we don't cancel when we share
            cancelBingoAnimation();
        }

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
                saveWords();
                return true;
            case R.id.action_share_bullshit:
                shareCurrentCard();
                return true;
            case R.id.action_share_image:
                shareImage(null);
                return true;
            case R.id.action_delete:
                showDeleteCardDialog(currentCardName);
                return true;
            case R.id.action_shuffle:
                resetCardState();
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

    private List<WordAndHits> getWordsAndHits() {
        List<Object> items = gridAdapter.getItems();
        ArrayList<WordAndHits> result = new ArrayList<>(items.size());
        for(Object o : items) {
            result.add((WordAndHits) o);
        }

        return result;
    }

    public void shareImage(View v) {
        boolean isLand = Util.isLandscape(this);
        int width = isLand ? IMAGE_LONG_EDGE : IMAGE_SHORT_EDGE;
        int height = isLand ? IMAGE_SHORT_EDGE : IMAGE_LONG_EDGE;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        CardExporter cardExporter = new CardExporter(getWordsAndHits(), canvas, this)
                .withWidth(width)
                .withHeight(height)
                .withCardName(currentCardName)
                /*colors*/
                .withSelectedCardTextColor(selectedCardTextColor)
                .withUnselectedCardTextColor(unselectedCardTextColor)
                .withSelectedCardBackgroundColor(getSelectedCardBackgroundColor())
                .withUnselectedCardBackgroundColor(getUnselectedCardBackgroundColor())
                .withGridColor(getGridColor())
                .withBingoStrokeColor(getBingoStrokeColor())
                ;

        cardExporter.drawCard();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, IMAGE_PNG);
        values.put(MediaStore.Images.Media.DATE_TAKEN, new Date().getTime());

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        OutputStream os;
        try {
            os = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
            closeQuietly(os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(IMAGE_PNG);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(share, getString(R.string.action_share_image)));
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.hasExtra(PreferencesActivity.RELOAD_DEFAULT_CARDS)) {
                    copyDefaultCards(DEFAULT_CARDS);
                    reloadCardList();

                    for (int id : DEFAULT_CARDS) {
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
        boolean land = Util.isLandscape(this);
        String prefix = Util.getFontPrefix(land);

        String key = Util.getFontKey(dim, prefix);

        sharedPreferences.edit().putFloat(key, finalFontSize).commit();

        gridAdapter.notifyDataSetChanged();
    }

    private void saveWords() {
        exitEditMode();
        if (isWrittenToDisk()) {
            persistWords(currentCardName);
        } else if (!currentCardName.endsWith("*")) {
            currentCardName += "*";
            updateTitle();
        }
    }

    private void shareCurrentCard() {
        if (!checkDir()) {
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String cardName = currentCardName + FILE_SUFFIX;
        String title = getResources().getString(R.string.share_title, cardName);
        sendIntent.putExtra(Intent.EXTRA_TITLE, title);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        File file = new File(bullshitDir, cardName);
        if (!file.exists()) {
            Toast.makeText(this, "No bullshit file: " + file, Toast.LENGTH_SHORT).show();
            return;
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
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (!checkDir()) {
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
                .setMessage(getResources().getString(R.string.delete_prompt, cardName))
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

    private boolean isWrittenToDisk() {
        return currentCardName != null && !currentCardName.startsWith(NEW_CARD_PREFIX);
    }

    private void updateTitle() {
        actionBar.setTitle(currentCardName);
    }

    private void prepareForEdit() {
        isEditing = true;
        Toast.makeText(this, R.string.long_press_to_drag, Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
        gridView.setOnItemLongClickListener(itemLongClickListener);
        resetCardState();
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
        if (dimensionDialog == null) {
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
        shareBullshitMenuItem = menu.findItem(R.id.action_share_bullshit);
        shareImageMenuItem = menu.findItem(R.id.action_share_image);
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
            shareBullshitMenuItem.setVisible(false);
            shareImageMenuItem.setVisible(false);
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
            shareBullshitMenuItem.setVisible(isGridFilled() && isWrittenToDisk());
            shareImageMenuItem.setVisible(isGridFilled());
            shuffleMenuItem.setVisible(false);
            deleteMenuItem.setVisible(isGridFilled() && isWrittenToDisk());
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

        resetCardState();
        //go into edit mode immediately
        prepareForEdit();

        setNewCardName();

        updateTitle();

        initCleanBoard();
    }

    private void initCleanBoard() {
        ArrayList<WordAndHits> currentWords = new ArrayList<>(dim * dim);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                currentWords.add(new WordAndHits());
            }
        }

        initBoardFromWords(currentWords);
        invalidateOptionsMenu();
    }

    private void initBoardFromWords(final List<WordAndHits> currentWords) {
        finalFontSize = Util.getCardFontSize(this, dim, Util.isLandscape(this));

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
        gridAdapter.setItemAtPosition(new WordAndHits(newValue.toString(), 0), position);
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
        if (!checkDir()) {
            return;
        }

        Toast.makeText(this, getString(R.string.toast_saving_card, fileName), Toast.LENGTH_SHORT).show();

        File file = new File(bullshitDir, fileName.toString() + FILE_SUFFIX);
        if (file.exists()) {
            file.delete();
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UTF_8));
        } catch (IOException e) {
            Toast.makeText(this, "Can't persist words", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            writer
                    .append(COMMENT_MARK)
                    .append(getResources().getString(R.string.file_comment))
                    .append("\n");
            for (Object o : gridAdapter.getItems()) {
                WordAndHits word = (WordAndHits) o;
                writer
                        .append(word.word)
                        .append(DELIMITER_MARK)
                        .append(Integer.toString(word.hits))
                        .append("\n");
            }

            isDirty = false;

        } catch (IOException e) {
            Toast.makeText(this, "Can't write to bullshitbingo file " + file + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkDir() {
        if (bullshitDir == null || !bullshitDir.exists()) {
            Toast.makeText(this, getString(R.string.error_directory_does_not_exist, bullshitDir), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isEditing) {
            saveWords();
        } else if (isBingoAnimationPlaying) {
            cancelBingoAnimation();
        } else {
            super.onBackPressed();
        }
    }

    private void createDirIfNeeded() {
        if (isExternalStorageWritable()) {
            bullshitDir = Environment.getExternalStoragePublicDirectory(DIR_NAME);
        } else {
            //use internal storage
            bullshitDir = getDir(DIR_NAME, MODE_PRIVATE);
        }

        bullshitDir.mkdirs();

        try {
            new File(bullshitDir, ".nomedia").createNewFile();
        } catch (IOException e) {
            Toast.makeText(this, "Can't create .nomedia file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (!bullshitDir.exists()) {
            Toast.makeText(this, "Can't create directory to store *.bullshit files at " + bullshitDir, Toast.LENGTH_SHORT).show();
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
