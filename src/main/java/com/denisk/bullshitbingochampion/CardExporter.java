package com.denisk.bullshitbingochampion;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author denisk
 * @since 04.01.15.
 */
public class CardExporter {
    public static final int TOP_MARGIN = 100;
    public static final int BOTTOM_MARGIN = 100;
    public static final int HEADER_FOOTER_LEFT_MARGIN = 20;
    public static final int HEADER_FOOTER_COLOR = 0xffe5e5e5;
    public static final int HEADER_TEXT_SIZE = 50;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM yyyy, HH:mm:ss Z");
    public static final int DATE_TEXT_SIZE = 35;
    public static final int DATE_TOP_MARGIN = 5;
    public static final int BBC_TEXT_SIZE = 30;
    public static final int BBC_BOTTOM_MARGIN = 10;
    public static final int BBC_LOGO_PADDING = 5;
    private static final float BINGO_STROKE_WIDTH = 15;

    private String cardName = "<unknown>";
    private int lineInterval = 15;
    private int gridWidth = 10;
    private int gridColor = Color.BLACK;
    private int selectedCardBackgroundColor = 0xFF64FAAF;
    private int unselectedCardBackgroundColor = Color.WHITE;
    private int bingoStrokeColor = Color.RED;
    private int unselectedCardTextColor = Color.BLACK;
    private int selectedCardTextColor = Color.BLACK;

    private int width = 1024;
    private int height = 768;

    private List<WordAndHits> words;
    private Canvas canvas;
    private Context context;

    public CardExporter(List<WordAndHits> words, Canvas canvas, Context context) {
        this.words = words;
        this.canvas = canvas;
        this.context = context;
    }

    public CardExporter withWidth(int width) {
        this.width = width;
        return this;
    }

    public CardExporter withHeight(int height) {
        this.height = height;
        return this;
    }

    public CardExporter withCardName(String cardName) {
        this.cardName = cardName;
        return this;
    }

    public CardExporter withLineInterval(int lineInterval) {
        this.lineInterval = lineInterval;
        return this;
    }

    public CardExporter withGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
        return this;
    }

    public CardExporter withGridColor(int gridColor) {
        this.gridColor = gridColor;
        return this;
    }

    public CardExporter withSelectedCardBackgroundColor(int selectedCardBackgroundColor) {
        this.selectedCardBackgroundColor = selectedCardBackgroundColor;
        return this;
    }

    public CardExporter withUnselectedCardBackgroundColor(int unselectedCardBackgroundColor) {
        this.unselectedCardBackgroundColor = unselectedCardBackgroundColor;
        return this;
    }

    public CardExporter withBingoStrokeColor(int bingoStrokeColor) {
        this.bingoStrokeColor = bingoStrokeColor;
        return this;
    }

    public CardExporter withUnselectedCardTextColor(int unselectedCardTextColor) {
        this.unselectedCardTextColor = unselectedCardTextColor;
        return this;
    }

    public CardExporter withSelectedCardTextColor(int selectedCardTextColor) {
        this.selectedCardTextColor = selectedCardTextColor;
        return this;
    }

    public void drawCard() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        drawBackgrounds(paint);

        drawHeader(paint);

        drawFooter(paint);

        float contentHeight = height - TOP_MARGIN - BOTTOM_MARGIN;

        float dim = Util.getDim(words.size());
        if(dim < 0) {
            return;
        }
        float cellWidth = width/dim;
        float cellHeight = contentHeight/dim;

        drawWords(paint, dim, cellWidth, cellHeight);

        drawGrid(paint, dim, contentHeight);

        drawBingo(paint, cellWidth, cellHeight);
    }

    private void drawBingo(Paint paint, float cellWidth, float cellHeight) {
        BingoData bingoData = BingoData.fromWords(words);
        paint.setColor(bingoStrokeColor);
        paint.setStrokeWidth(BINGO_STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);

        for(Integer row : bingoData.bingoRows) {
            canvas.drawRect(0, row * cellHeight + TOP_MARGIN, width, (row + 1) * cellHeight + TOP_MARGIN, paint);
        }
        for(Integer col : bingoData.bingoColumns) {
            canvas.drawRect(col * cellWidth, TOP_MARGIN, (col + 1) * cellWidth, height - BOTTOM_MARGIN, paint);
        }
    }

    private void drawWords(Paint paint, float dim, float cellWidth, float cellHeight) {

        float textSize = Util.getCardFontSize(context, (int) dim, width > height);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.DEFAULT);

        for(int i = 0; i < dim*dim; i++) {
            WordAndHits word = words.get(i);
            float xCell = i % dim;
            float yCell = (float) Math.floor(i / dim);

            boolean selected = word.hits > 0;

            paint.setColor(selected ? selectedCardBackgroundColor : unselectedCardBackgroundColor);
            canvas.drawRect(xCell * cellWidth, yCell * cellHeight + TOP_MARGIN, (xCell + 1) * cellWidth, (yCell + 1) * cellHeight + TOP_MARGIN, paint);

            String text = (word.word == null ? "" : word.word.trim());

            List<String> lines = splitIntoStringsThatFit(text.split("\\s+"), cellWidth, paint);

            float textBlockHeight = lines.size() * textSize + (lines.size() - 1) * lineInterval;
            float currentLineYCoord = yCell * cellHeight + cellHeight / 2 - textBlockHeight / 2 + textSize + TOP_MARGIN;

            paint.setColor(selected ? selectedCardTextColor : unselectedCardTextColor);

            for(String line : lines) {
                canvas.drawText(line, xCell * cellWidth + cellWidth / 2, currentLineYCoord, paint);
                currentLineYCoord += textSize + lineInterval;
            }
        }
    }

    private void drawFooter(Paint paint) {
        String dateText = DATE_FORMAT.format(new Date());

        paint.setTextSize(DATE_TEXT_SIZE);
        Rect bounds = new Rect();
        paint.getTextBounds(dateText, 0, dateText.length(), bounds);
        canvas.drawText(dateText, HEADER_FOOTER_LEFT_MARGIN, height - BOTTOM_MARGIN + bounds.height() + DATE_TOP_MARGIN , paint);


        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(context, "Can't load current application info", Toast.LENGTH_LONG).show();
            return;
        }

        paint.setTextSize(BBC_TEXT_SIZE);
        String applicationName = Util.getApplicationName(context);
        paint.getTextBounds(applicationName, 0, applicationName.length(), bounds);

        canvas.drawText(applicationName, HEADER_FOOTER_LEFT_MARGIN, height - BBC_BOTTOM_MARGIN, paint);

        Drawable iconDrawable = context.getResources().getDrawable(applicationInfo.icon);
        Bitmap iconBitmap = Util.drawableToBitmap(iconDrawable);

        iconBitmap = Bitmap.createScaledBitmap(iconBitmap, BOTTOM_MARGIN - BBC_LOGO_PADDING, BOTTOM_MARGIN - BBC_LOGO_PADDING, true);
        canvas.drawBitmap(iconBitmap, width - BOTTOM_MARGIN, height - BOTTOM_MARGIN + BBC_LOGO_PADDING, paint);
    }

    private void drawHeader(Paint paint) {
        paint.setColor(Color.BLACK);
        paint.setTextSize(HEADER_TEXT_SIZE);
        String cardText = context.getString(R.string.share_picture_card) + " ";
        Rect bounds = new Rect();
        paint.getTextBounds(cardText, 0, cardText.length(), bounds);
        int titleYCoord = TOP_MARGIN / 2 + bounds.height() / 2;
        canvas.drawText(cardText, HEADER_FOOTER_LEFT_MARGIN, titleYCoord, paint);

        float cardTextWidth = paint.measureText(cardText);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(cardName, HEADER_FOOTER_LEFT_MARGIN + cardTextWidth, titleYCoord, paint);
    }

    private void drawBackgrounds(Paint paint) {
        paint.setColor(Color.WHITE);

        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(HEADER_FOOTER_COLOR);
        canvas.drawRect(0, 0, width, TOP_MARGIN, paint);
        canvas.drawRect(0, height - BOTTOM_MARGIN, width, height, paint);
    }

    private void drawGrid(Paint paint, float dim, float contentHeight) {
        paint.setColor(gridColor);
        paint.setStrokeWidth(gridWidth);
        for(int i = 1; i < dim; i++) {
            float x = width / dim * i;
            float y = contentHeight / dim * i;

            canvas.drawLine(x, TOP_MARGIN , x, contentHeight + TOP_MARGIN, paint);
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

}
