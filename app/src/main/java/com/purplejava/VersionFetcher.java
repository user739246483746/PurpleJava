package com.purplejava;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VersionFetcher {

    private static final String TAG = "VersionFetcher";
    private static final String MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    private final OkHttpClient http;
    private final ExecutorService executor;
    private final Gson gson;

    public interface Callback {
        void onResult(List<String> versions);
    }

    public VersionFetcher(Context ctx) {
        http     = new OkHttpClient();
        executor = Executors.newSingleThreadExecutor();
        gson     = new Gson();
    }

    public void fetchVersionList(Callback callback) {
        executor.submit(() -> {
            List<String> versions = new ArrayList<>();
            try {
                Request req = new Request.Builder().url(MANIFEST_URL).build();
                try (Response resp = http.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Log.e(TAG, "HTTP " + resp.code());
                        callback.onResult(null);
                        return;
                    }
                    String json = resp.body().string();
                    JsonObject root = gson.fromJson(json, JsonObject.class);
                    JsonArray arr = root.getAsJsonArray("versions");

                    for (JsonElement el : arr) {
                        JsonObject ver = el.getAsJsonObject();
                        String type = ver.get("type").getAsString();
                        if (type.equals("release") || type.equals("snapshot")) {
                            versions.add(ver.get("id").getAsString());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Falha ao buscar manifest", e);
                callback.onResult(null);
                return;
            }
            callback.onResult(versions);
        });
    }

    public void fetchClientJarUrl(String versionId, java.util.function.Consumer<String> callback) {
        executor.submit(() -> {
            try {
                Request req = new Request.Builder().url(MANIFEST_URL).build();
                String manifestJson;
                try (Response resp = http.newCall(req).execute()) {
                    manifestJson = resp.body().string();
                }

                JsonObject root = gson.fromJson(manifestJson, JsonObject.class);
                String versionUrl = null;
                for (JsonElement el : root.getAsJsonArray("versions")) {
                    JsonObject v = el.getAsJsonObject();
                    if (v.get("id").getAsString().equals(versionId)) {
                        versionUrl = v.get("url").getAsString();
                        break;
                    }
                }
                if (versionUrl == null) { callback.accept(null); return; }

                Request metaReq = new Request.Builder().url(versionUrl).build();
                String metaJson;
                try (Response resp = http.newCall(metaReq).execute()) {
                    metaJson = resp.body().string();
                }

                JsonObject meta      = gson.fromJson(metaJson, JsonObject.class);
                JsonObject downloads = meta.getAsJsonObject("downloads");
                JsonObject client    = downloads.getAsJsonObject("client");
                String jarUrl        = client.get("url").getAsString();
                callback.accept(jarUrl);

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar URL do jar", e);
                callback.accept(null);
            }
        });
    }
}
