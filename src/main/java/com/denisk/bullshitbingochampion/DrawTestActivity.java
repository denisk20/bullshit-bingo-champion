package com.denisk.bullshitbingochampion;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * @author denisk
 * @since 21.12.14.
 */
public class DrawTestActivity extends Activity {

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

                float strokeWidth = Util.dpToPix(getApplicationContext(), 6);
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(strokeWidth);
                for(int i = 1; i < dim; i++) {
                    float x = width / dim * i;
                    float y = height / dim * i;

                    canvas.drawLine(x, 0, x, height, paint);
                    canvas.drawLine(0, y, width, y, paint);
                }

                paint.setTextAlign(Paint.Align.CENTER);
                paint.setAntiAlias(true);

                paint.setTextSize(Util.dpToPix(getApplicationContext(), 34));
                for(int i = 0; i < dim*dim; i++) {
                    WordAndHits word = words.get(i);
                    float xCell = i % dim;
                    float yCell = (float) Math.floor(i / dim);

                    canvas.drawText(word.word, xCell* cellWidth + cellWidth / 2, yCell * cellHeight + cellHeight / 2, paint);
                }
                holder.unlockCanvasAndPost(canvas);
            }
        });

    }

    private void initWords() {
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello hello", 0));
        words.add(new WordAndHits("Another hello world", 2));
        words.add(new WordAndHits("Longlongwordswithoutspaceswhichwontfit", 1));
    }
}
