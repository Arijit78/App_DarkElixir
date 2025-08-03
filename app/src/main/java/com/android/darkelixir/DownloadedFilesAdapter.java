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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class DownloadedFilesAdapter extends RecyclerView.Adapter<DownloadedFilesAdapter.ViewHolder> {

    private final Context context;
    private final List<File> files;
    private final DownloadManager downloadManager;
    private long currentDownloadId;
    private Handler handler = new Handler(Looper.getMainLooper());

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

        // Optional: show file size if needed
        long length = file.length();
        holder.fileSizeTextView.setText(formatFileSize(length));

        if (isCurrentDownloadingFile(file)) {
            holder.progressBar.setVisibility(View.VISIBLE);
            int progress = getDownloadProgress();
            holder.progressBar.setProgress(progress);

            holder.statusTextView.setVisibility(View.VISIBLE);
            holder.statusTextView.setText("Downloading...");
            holder.statusTextView.setTextColor(0xFF2196F3); // Blue color for downloading
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.statusTextView.setVisibility(View.VISIBLE);
            holder.statusTextView.setText("Done");
            holder.statusTextView.setTextColor(0xFF4CAF50); // Green color for done
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private boolean isCurrentDownloadingFile(File file) {
        if (currentDownloadId == -1) return false;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(currentDownloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
            cursor.close();
            if (fileUriString != null) {
                Uri fileUri = Uri.parse(fileUriString);
                return file.getAbsolutePath().equals(fileUri.getPath());
            }
        }
        return false;
    }

    private int getDownloadProgress() {
        if (currentDownloadId == -1) return 0;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(currentDownloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            int bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            cursor.close();
            if (bytesTotal > 0) {
                return (int) ((bytesDownloaded * 100L) / bytesTotal);
            }
        }
        return 0;
    }

    private void startProgressUpdates() {
        handler.post(progressRunnable);
    }

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentDownloadId == -1) {
                handler.removeCallbacks(this);
                return;
            }

            notifyDataSetChanged();

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(currentDownloadId);
            Cursor cursor = downloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                cursor.close();
                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    currentDownloadId = -1;
                    handler.removeCallbacks(this);
                    notifyDataSetChanged();
                    return;
                }
            }

            handler.postDelayed(this, 500);
        }
    };

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView, fileSizeTextView, statusTextView;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            fileSizeTextView = itemView.findViewById(R.id.fileSizeTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            progressBar = itemView.findViewById(R.id.fileDownloadProgressBar);
        }
    }
}
