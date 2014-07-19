package com.denisk.bingo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class BullshitBingoActivity extends Activity {

    public static final String BULLSHIT_FILES_PATH_KEY = "bullshitFilesPath";
    public static final String DIR_NAME = "bullshitbingochamp";
    public static final String PREFS_NAME = "bullshitbingochamp";

    int dim = 5;
    private DynamicGridView gridView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDirIfNeeded();

        setContentView(R.layout.bingo_activity);

        gridView = (DynamicGridView) findViewById(R.id.gridview);
        gridView.setNumColumns(dim);

        final ArrayList<String> words = new ArrayList<>(dim*dim);
        for (int i = 0; i < dim; i++) {
            for(int j = 0; j < dim; j++) {
                words.add(i + "_" + j);
            }
        }
        gridView.setOnDragListener(new DynamicGridView.OnDragListener() {
            @Override
            public void onDragStarted(int position) {
                Log.d("===", "drag started at position " + position);
            }

            @Override
            public void onDragPositionsChanged(int oldPosition, int newPosition) {
                Log.d("===", String.format("drag item position changed from %d to %d", oldPosition, newPosition));
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                gridView.startEditMode(position);
                return true;
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(BullshitBingoActivity.this, parent.getAdapter().getItem(position).toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        gridView.setAdapter(new BaseDynamicGridAdapter(this, words, dim) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                String text = words.get(position);
                Log.i("===", "here");
                TextView textView;
                if (!(convertView instanceof TextView)) {
                    textView = new TextView(BullshitBingoActivity.this);
                } else {
                    textView = (TextView) convertView;
                }

                textView.setText(text);
                return textView;
            }
        });

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
            Log.i(getClass().getName(), "===Bullshit dir: " + bullshitDir);
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
