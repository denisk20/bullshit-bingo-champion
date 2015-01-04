package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author denisk
 * @since 21.12.14.
 */
public class DrawTestActivity extends Activity {

    public static final int GRID_WIDTH = 10;
    public static final int TEXT_SIZE = 100;
    public static final int LINE_INTERVAL = 15;
    public static final int LEFT_MARGIN = 3;
    public static final int TOP_MARGIN = 100;
    public static final int BOTTOM_MARGIN = 200;
    public static final int HEADER_FOOTER_LEFT_MARGIN = 20;

    private SurfaceView surfaceView;
    ArrayList<WordAndHits> words = new ArrayList<>();
    private SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.draw_test_activity);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        holder = surfaceView.getHolder();

        initWords();

        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                float width = surfaceView.getWidth();
                float height = surfaceView.getHeight();

                Canvas canvas = holder.lockCanvas();

                Paint paint = new Paint();
                paint.setAntiAlias(true);

                paint.setColor(Color.WHITE);

                canvas.drawRect(0, 0, width, height, paint);

                paint.setColor(0xffe5e5e5);
                canvas.drawRect(0, 0, width, TOP_MARGIN, paint);
                float bottomMarginYCoord = height - BOTTOM_MARGIN;
                canvas.drawRect(0, bottomMarginYCoord, width, height, paint);

                paint.setColor(Color.BLACK);
                paint.setTextSize(70);
                String cardText = "Card: ";
                Rect bounds = new Rect();
                paint.getTextBounds(cardText, 0, cardText.length(), bounds);
                int titleYCoord = TOP_MARGIN / 2 + bounds.height() / 2;
                canvas.drawText(cardText, HEADER_FOOTER_LEFT_MARGIN, titleYCoord, paint);

                float cardTextWidth = paint.measureText(cardText);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText("My Card", HEADER_FOOTER_LEFT_MARGIN + cardTextWidth, titleYCoord, paint);

                SimpleDateFormat format = new SimpleDateFormat("d MMMM yyyy, HH:mm:ss Z");
                String dateText = format.format(new Date());

                paint.setTextSize(50);
                paint.getTextBounds(dateText, 0, dateText.length(), bounds);
                canvas.drawText(dateText, HEADER_FOOTER_LEFT_MARGIN, bottomMarginYCoord + bounds.height() , paint);


                ApplicationInfo applicationInfo;
                try {
                    applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Toast.makeText(DrawTestActivity.this, "Can't load current application info", Toast.LENGTH_LONG).show();
                    return;
                }

                paint.setTextSize(40);
                String applicationName = Util.getApplicationName(DrawTestActivity.this);
                paint.getTextBounds(applicationName, 0, applicationName.length(), bounds);

                canvas.drawText(applicationName, HEADER_FOOTER_LEFT_MARGIN, height - 20, paint);

                Drawable iconDrawable = getResources().getDrawable(applicationInfo.icon);
                Bitmap iconBitmap = drawableToBitmap(iconDrawable);

                iconBitmap = Bitmap.createScaledBitmap(iconBitmap, BOTTOM_MARGIN, BOTTOM_MARGIN, true);
                canvas.drawBitmap(iconBitmap, width - BOTTOM_MARGIN, bottomMarginYCoord, paint);

                float origHeight = height;
                height -= TOP_MARGIN + BOTTOM_MARGIN;

                float dim = (float) Math.sqrt(words.size());
                float cellWidth = width/dim;
                float cellHeight = height/dim;

                paint.setTextAlign(Paint.Align.LEFT);

                paint.setTextSize(TEXT_SIZE);
                paint.setTypeface(Typeface.DEFAULT);
                for(int i = 0; i < dim*dim; i++) {
                    WordAndHits word = words.get(i);
                    float xCell = i % dim;
                    float yCell = (float) Math.floor(i / dim);

                    if(word.hits > 0) {
                        int color = paint.getColor();
                        paint.setColor(0xFFDBFFED);
                        canvas.drawRect(xCell * cellWidth, yCell * cellHeight + TOP_MARGIN, (xCell + 1) * cellWidth, (yCell + 1) * cellHeight + TOP_MARGIN, paint);
                        paint.setColor(color);
                    }
                    String text = (word.word == null ? "" : word.word.trim());

                    List<String> lines = splitIntoStringsThatFit(text.split("\\s+"), cellWidth, paint);

                    float textBlockHeight = lines.size() * TEXT_SIZE + (lines.size() - 1) * LINE_INTERVAL;
                    float currentLineYCoord = yCell * cellHeight + cellHeight / 2 - textBlockHeight / 2 + TEXT_SIZE + TOP_MARGIN;
                    for(String line : lines) {
                        canvas.drawText(line, xCell * cellWidth + LEFT_MARGIN, currentLineYCoord, paint);
                        currentLineYCoord += TEXT_SIZE + LINE_INTERVAL;
                    }
                }

                drawGrid(width, height, canvas, paint, dim);

                BingoData bingoData = BingoData.fromWords(words);
                paint.setColor(Color.RED);
                paint.setStrokeWidth(GRID_WIDTH * 2);
                paint.setStyle(Paint.Style.STROKE);

                for(Integer row : bingoData.bingoRows) {
                    canvas.drawRect(0, row * cellHeight + TOP_MARGIN, width, (row + 1) * cellHeight + TOP_MARGIN, paint);
                }
                for(Integer col : bingoData.bingoColumns) {
                    canvas.drawRect(col * cellWidth, TOP_MARGIN, (col + 1) * cellWidth, origHeight - BOTTOM_MARGIN, paint);
                }
                holder.unlockCanvasAndPost(canvas);
            }
        });

    }

    private void drawGrid(float width, float height, Canvas canvas, Paint paint, float dim) {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(GRID_WIDTH);
        for(int i = 1; i < dim; i++) {
            float x = width / dim * i;
            float y = height / dim * i;

            canvas.drawLine(x, TOP_MARGIN , x, height + TOP_MARGIN, paint);
            canvas.drawLine(0, y + TOP_MARGIN, width, y + TOP_MARGIN, paint);
        }
    }

    static List<String> splitIntoStringsThatFit(String source, float limitWidth, Paint paint) {
        if(TextUtils.isEmpty(source) || paint.measureText(source) <= limitWidth) {
            return Arrays.asList(source);
        }

        ArrayList<String> result = new ArrayList<>();
        int start = 0;
        for(int i = 1; i <= source.length(); i++) {
            String substr = source.substring(start, i);
            if(paint.measureText(substr) >= limitWidth) {
                //this one doesn't fit, take the previous one which fits
                String fits = source.substring(start, i - 1);
                result.add(fits);
                start = i - 1;
            }
            if (i == source.length()) {
                String fits = source.substring(start, i);
                result.add(fits);
            }
        }

        return result;
    }

    static List<String> splitIntoStringsThatFit(String[] sources, float maxWidth, Paint paint) {
        ArrayList<String> result = new ArrayList<>();

        ArrayList<String> currentLine = new ArrayList<>();

        for(String chunk : sources) {
            if(paint.measureText(chunk) < maxWidth) {
                processFitChunk(maxWidth, paint, result, currentLine, chunk);
            } else {
                List<String> chunkChunks = splitIntoStringsThatFit(chunk, maxWidth, paint);
                for(String chunkChunk : chunkChunks) {
                    processFitChunk(maxWidth, paint, result, currentLine, chunkChunk);
                }
            }
        }

        if(! currentLine.isEmpty()) {
            result.add(TextUtils.join(" ", currentLine));
        }
        return result;
    }

    private static void processFitChunk(float maxWidth, Paint paint, ArrayList<String> result, ArrayList<String> currentLine, String chunk) {
        currentLine.add(chunk);
        String currentLineStr = TextUtils.join(" ", currentLine);
        if (paint.measureText(currentLineStr) >= maxWidth) {
            //remove chunk
            currentLine.remove(currentLine.size() - 1);
            result.add(TextUtils.join(" ", currentLine));
            currentLine.clear();
            //ok because chunk fits
            currentLine.add(chunk);
        }
    }

    private void initWords() {
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello", 1));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 0));
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
}
