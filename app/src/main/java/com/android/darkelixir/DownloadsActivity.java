package com.android.darkelixir;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.recyclerview.widget.ItemTouchHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity {

    private RecyclerView downloadedFilesRecyclerView;
    private DownloadedFilesAdapter adapter;
    private FloatingActionButton refreshFab;
    private FloatingActionButton fabToggleTheme;
    private SharedPreferences prefs;

    private static final String DOWNLOAD_SUBFOLDER = "DarkElixir";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_downloads);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                File fileToDelete = adapter.getFileAt(position);

                if (fileToDelete.exists()) {
                    boolean deleted = fileToDelete.delete();
                    if (deleted) {
                        Toast.makeText(DownloadsActivity.this, "Deleted: " + fileToDelete.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DownloadsActivity.this, "Failed to delete: " + fileToDelete.getName(), Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.removeFileAt(position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(downloadedFilesRecyclerView);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        adjustStatusBarIconsForTheme(isDarkMode);

        refreshFab = findViewById(R.id.refresh_fab);
        fabToggleTheme = findViewById(R.id.fabToggleTheme);

        fabToggleTheme.setOnClickListener(v -> {
            boolean darkModeEnabled = prefs.getBoolean("dark_mode", false);
            SharedPreferences.Editor editor = prefs.edit();

            if (darkModeEnabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("dark_mode", false);
                updateFabIcon(false);
                adjustStatusBarIconsForTheme(false);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("dark_mode", true);
                updateFabIcon(true);
                adjustStatusBarIconsForTheme(true);
            }

            editor.apply();
        });

        refreshFab.setOnClickListener(v -> {
            RotateAnimation rotate = new RotateAnimation(
                    0, 360,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(500);
            v.startAnimation(rotate);

            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            refreshDownloadList();
        });

        downloadedFilesRecyclerView = findViewById(R.id.downloadedFilesRecyclerView);
        downloadedFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DownloadedFilesAdapter(this, new ArrayList<>(), -1);
        downloadedFilesRecyclerView.setAdapter(adapter);

        updateFabIcon(isDarkMode);

        // Initial list
        refreshDownloadList();
    }

    private void refreshDownloadList() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBFOLDER);
        List<File> files;
        if (dir.exists() && dir.listFiles() != null) {
            files = Arrays.asList(dir.listFiles());
        } else {
            files = new ArrayList<>();
            Toast.makeText(this, "No downloads found", Toast.LENGTH_SHORT).show();
        }

        adapter.updateFiles(files);
    }

    private void updateFabIcon(boolean isDarkMode) {
        if (fabToggleTheme == null) return;
        if (isDarkMode) {
            fabToggleTheme.setImageResource(android.R.drawable.ic_menu_day);
        } else {
            fabToggleTheme.setImageResource(android.R.drawable.ic_menu_day);
        }
    }

    private void adjustStatusBarIconsForTheme(boolean isDarkMode) {
        View decor = getWindow().getDecorView();
        if (!isDarkMode) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(0);
        }
    }
}