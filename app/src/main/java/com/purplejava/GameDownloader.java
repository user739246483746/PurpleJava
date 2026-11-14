package com.purplejava;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GameDownloader {

    private static final String TAG = "GameDownloader";

    private final Context ctx;
    private final OkHttpClient http;
    private final VersionFetcher fetcher;

    public interface DownloadCallback {
        void onDone(boolean success);
    }

    public GameDownloader(Context ctx) {
        this.ctx     = ctx;
        this.http    = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.fetcher = new VersionFetcher(ctx);
    }

    public void ensureVersion(String versionId, DownloadCallback callback) {
        File jarFile = getJarFile(versionId);
        if (jarFile.exists() && jarFile.length() > 100_000) {
            Log.d(TAG, "JAR já existe: " + jarFile.getAbsolutePath());
            callback.onDone(true);
            return;
        }

        fetcher.fetchClientJarUrl(versionId, jarUrl -> {
            if (jarUrl == null) {
                callback.onDone(false);
                return;
            }
            downloadJar(jarUrl, jarFile, callback);
        });
    }

    private void downloadJar(String url, File dest, DownloadCallback callback) {
        new Thread(() -> {
            dest.getParentFile().mkdirs();
            try {
                Request req = new Request.Builder().url(url).build();
                try (Response resp = http.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Log.e(TAG, "HTTP " + resp.code() + " ao baixar jar");
                        callback.onDone(false);
                        return;
                    }
                    try (InputStream in  = resp.body().byteStream();
                         FileOutputStream out = new FileOutputStream(dest)) {
                        byte[] buf = new byte[8192];
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            out.write(buf, 0, read);
                        }
                    }
                }
                Log.d(TAG, "Download concluído: " + dest.getAbsolutePath());
                callback.onDone(true);
            } catch (IOException e) {
                Log.e(TAG, "Erro no download", e);
                if (dest.exists()) dest.delete();
                callback.onDone(false);
            }
        }).start();
    }

    public File getJarFile(String versionId) {
        return new File(ctx.getFilesDir(), "versions/" + versionId + "/client.jar");
    }
}
