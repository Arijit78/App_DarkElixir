package com.android.darkelixir;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private final String HOME_URL = "https://dark-elixir-project.vercel.app/";
    private long downloadID = -1;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional edge-to-edge fullscreen (remove if not needed)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }

        webView = findViewById(R.id.myWeb);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webView.loadUrl(HOME_URL);

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype, long contentLength) {

                String guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                if (url.contains(".zip") || mimetype.equalsIgnoreCase("application/zip")) {
                    guessedName = guessedName.replaceAll("\\.bin$", ".zip");
                    if (!guessedName.endsWith(".zip")) {
                        guessedName += ".zip";
                    }
                }

                String fileName = guessedName;

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                request.setDestinationUri(Uri.fromFile(file));

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                downloadID = dm.enqueue(request);

                handler.post(progressRunnable);

                Toast.makeText(getApplicationContext(), "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton fab = findViewById(R.id.openDownloadsButton);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DownloadsActivity.class);
            intent.putExtra("currentDownloadId", downloadID);
            startActivity(intent);
        });

        this.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
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
            Cursor cursor = dm.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                if (bytesTotal > 0) {
                    int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                }

                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    handler.removeCallbacks(this);
                    downloadID = -1;
                } else {
                    handler.postDelayed(this, 500);
                }
                cursor.close();
            } else {
                handler.removeCallbacks(this);
                downloadID = -1;
            }
        }
    };
}
