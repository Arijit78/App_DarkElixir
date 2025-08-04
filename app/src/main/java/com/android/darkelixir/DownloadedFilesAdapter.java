package com.android.darkelixir;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadedFilesAdapter extends RecyclerView.Adapter<DownloadedFilesAdapter.ViewHolder> {

    private final Context context;
    private final List<File> files;
    private final DownloadManager downloadManager;
    private long currentDownloadId;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public DownloadedFilesAdapter(Context context, List<File> files, long currentDownloadId) {
        this.context = context;
        this.files = files;
        this.currentDownloadId = currentDownloadId;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        startProgressUpdates();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_download_clean, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.fileNameTextView.setText(file.getName());
        holder.fileSizeTextView.setText(formatFileSize(file.length()));
        holder.fileDateTextView.setText(getFormattedDate(file.lastModified()));
        holder.fileTypeTextView.setText(getMimeType(file));

        if (isCurrentDownloadingFile(file)) {
            int progress = getDownloadProgress();

            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(progress);
            holder.statusTextView.setText("Downloading...");
            holder.statusTextView.setTextColor(0xFF2196F3); // Blue
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.statusTextView.setText("Done");
            holder.statusTextView.setTextColor(0xFF4CAF50); // Green
        }

        holder.statusTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private boolean isCurrentDownloadingFile(File file) {
        if (currentDownloadId == -1) return false;

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(currentDownloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                if (localUri != null) {
                    Uri fileUri = Uri.parse(localUri);
                    return file.getAbsolutePath().equals(fileUri.getPath());
                }
            }
        }
        return false;
    }

    private int getDownloadProgress() {
        if (currentDownloadId == -1) return 0;

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(currentDownloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                if (bytesTotal > 0) {
                    return (int) ((bytesDownloaded * 100L) / bytesTotal);
                }
            }
        }
        return 0;
    }

    private void startProgressUpdates() {
        handler.post(progressRunnable);
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentDownloadId == -1) {
                handler.removeCallbacks(this);
                return;
            }
            notifyDataSetChanged();

            if (isDownloadComplete(currentDownloadId)) {
                currentDownloadId = -1;
                handler.removeCallbacks(this);
                notifyDataSetChanged();
                return;
            }
            handler.postDelayed(this, 500);
        }
    };

    private boolean isDownloadComplete(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                return status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED;
            }
        }
        return false;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String getMimeType(File file) {
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(file.getName());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) != null ?
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : "Unknown";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView fileNameTextView, fileSizeTextView, fileDateTextView, fileTypeTextView, statusTextView;
        final ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            fileSizeTextView = itemView.findViewById(R.id.fileSizeTextView);
            fileDateTextView = itemView.findViewById(R.id.fileDateTextView);
            fileTypeTextView = itemView.findViewById(R.id.fileTypeTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            progressBar = itemView.findViewById(R.id.fileDownloadProgressBar);
        }
    }

    public void updateFiles(List<File> newFiles) {
        this.files.clear();
        this.files.addAll(newFiles);
        notifyDataSetChanged();
    }

    public File getFileAt(int position) {
        return files.get(position);
    }

    public void removeFileAt(int position) {
        if (position >= 0 && position < files.size()) {
            files.remove(position);
            notifyItemRemoved(position);
        }
    }
}