package com.android.darkelixir;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity {

    private RecyclerView downloadedFilesRecyclerView;
    private DownloadedFilesAdapter adapter;
    private FloatingActionButton fabToggleTheme;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_downloads);

        downloadedFilesRecyclerView = findViewById(R.id.downloadedFilesRecyclerView);
        downloadedFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        List<File> files;

        if (dir != null && dir.exists() && dir.listFiles() != null) {
            files = Arrays.asList(dir.listFiles());
        } else {
            files = new ArrayList<>();
        }

        long currentDownloadId = getIntent().getLongExtra("currentDownloadId", -1);

        adapter = new DownloadedFilesAdapter(this, files, currentDownloadId);
        downloadedFilesRecyclerView.setAdapter(adapter);

        fabToggleTheme = findViewById(R.id.fabToggleTheme);
        updateFabIcon(isDarkMode);

        fabToggleTheme.setOnClickListener(v -> {
            boolean darkModeEnabled = prefs.getBoolean("dark_mode", false);
            SharedPreferences.Editor editor = prefs.edit();

            if (darkModeEnabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                updateFabIcon(false);
                editor.putBoolean("dark_mode", false);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                updateFabIcon(true);
                editor.putBoolean("dark_mode", true);
            }
            editor.apply();
        });
    }

    private void updateFabIcon(boolean isDarkMode) {
        if (fabToggleTheme == null) return;
        if (isDarkMode) {
            fabToggleTheme.setImageResource(android.R.drawable.ic_menu_day);
        } else {
            fabToggleTheme.setImageResource(android.R.drawable.ic_menu_day);
        }
    }
}
