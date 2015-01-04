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
                int width = surfaceView.getWidth();
                int height = surfaceView.getHeight();

                Canvas canvas = holder.lockCanvas();

                CardExporter exporter = new CardExporter(words, canvas, DrawTestActivity.this)
                        .withCardName("My Card")
                        .withWidth(width)
                        .withHeight(height);
                exporter.drawCard();

                holder.unlockCanvasAndPost(canvas);
            }
        });

    }


    private void initWords() {
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello", 1));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello hello hello hello!", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello", 1));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 0));
        words.add(new WordAndHits("Hello", 2));
        words.add(new WordAndHits("Hello", 2));
    }
}
