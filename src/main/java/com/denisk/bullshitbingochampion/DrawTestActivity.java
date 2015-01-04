package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author denisk
 * @since 21.12.14.
 */
public class DrawTestActivity extends Activity {

    public static final int GRID_WIDTH = 1;
    public static final int TEXT_SIZE = 100;
    public static final int LINE_INTERVAL = 15;
    public static final int LEFT_MARGIN = 0;
    public static final int TOP_MARGIN = 300;
    public static final int BOTTOM_MARGIN = 300;


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
                paint.setColor(Color.WHITE);

                canvas.drawRect(0, 0, width, height, paint);

                float dim = (float) Math.sqrt(words.size());
                float cellWidth = width/dim;
                float cellHeight = height/dim;

                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(GRID_WIDTH);
                for(int i = 1; i < dim; i++) {
                    float x = width / dim * i;
                    float y = height / dim * i;

                    canvas.drawLine(x, 0, x, height, paint);
                    canvas.drawLine(0, y, width, y, paint);
                }

                paint.setTextAlign(Paint.Align.LEFT);
                paint.setAntiAlias(true);

                paint.setTextSize(TEXT_SIZE);
                for(int i = 0; i < dim*dim; i++) {
                    WordAndHits word = words.get(i);
                    float xCell = i % dim;
                    float yCell = (float) Math.floor(i / dim);

                    String text = (word.word == null ? "" : word.word.trim());

                    Rect bounds = new Rect();
                    paint.getTextBounds(text, 0, text.length(), bounds);

                    List<String> lines = splitIntoStringsThatFit(text.split("\\s+"), cellWidth, paint);

                    float textBlockHeight = lines.size() * TEXT_SIZE + (lines.size() - 1) * LINE_INTERVAL;
                    float y = yCell * cellHeight + cellHeight / 2 - textBlockHeight / 2 + TEXT_SIZE;
                    for(String line : lines) {
                        canvas.drawText(line, xCell * cellWidth + LEFT_MARGIN, y, paint);
                        y += TEXT_SIZE + LINE_INTERVAL;
                    }
                }
                holder.unlockCanvasAndPost(canvas);
            }
        });

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
        words.add(new WordAndHits("Hello hello you how are you", 0));
        words.add(new WordAndHits("Another hello world letmeoutofhereplease!!! you all lie to me I kno it :) I o u k l m n j j c", 2));
        words.add(new WordAndHits("o Longlongwordswithoutspaceswhichwontfit", 1));
    }
}
