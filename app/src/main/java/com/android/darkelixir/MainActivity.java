package com.android.darkelixir;

import android.Manifest;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String HOME_URL = "https://dark-elixir-project.vercel.app/";
    private static final String DOWNLOAD_SUBFOLDER = "DarkElixir";

    private WebView webView;
    private long downloadID = -1;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            setupWebView();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.statusBarColorDark));
            } else {
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.statusBarColorLight));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    decor.setSystemUiVisibility(0);
                } else {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        }

        FloatingActionButton fab = findViewById(R.id.openDownloadsButton);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DownloadsActivity.class);
            startActivity(intent);
        });
    }

    private void setupWebView() {
        webView = findViewById(R.id.myWeb);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.loadUrl(HOME_URL);

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                String guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                if (url.contains(".zip") || "application/zip".equalsIgnoreCase(mimetype)) {
                    guessedName = guessedName.replaceAll("\\.bin$", ".zip");
                    if (!guessedName.endsWith(".zip")) {
                        guessedName += ".zip";
                    }
                }

                File downloadFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBFOLDER);
                if (!downloadFolder.exists()) {
                    boolean created = downloadFolder.mkdirs();
                    if (!created) {
                        Toast.makeText(MainActivity.this, "Failed to create download directory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                File file = new File(downloadFolder, guessedName);

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                request.setTitle(guessedName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationUri(Uri.fromFile(file));

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                downloadID = dm.enqueue(request);

                handler.post(progressRunnable);

                Toast.makeText(MainActivity.this, "Downloading: " + guessedName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (downloadID == -1) {
                handler.removeCallbacks(this);
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadID);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            try (Cursor cursor = dm.query(query)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        handler.removeCallbacks(this);
                        downloadID = -1;
                    } else {
                        handler.postDelayed(this, 500);
                    }
                } else {
                    handler.removeCallbacks(this);
                    downloadID = -1;
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupWebView();
            } else {
                Toast.makeText(this, "Storage permission denied, downloads may not work", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}