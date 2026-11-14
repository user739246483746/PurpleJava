package com.purplejava;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameLauncher {

    private static final String TAG = "GameLauncher";

    private final Context ctx;

    public interface LogCallback {
        void onLog(String msg);
    }

    public GameLauncher(Context ctx) {
        this.ctx = ctx;
    }

    public void launch(String versionId, String username, LogCallback log) {
        new Thread(() -> {
            File jarFile    = new File(ctx.getFilesDir(), "versions/" + versionId + "/client.jar");
            File gameDir    = new File(ctx.getExternalFilesDir(null), "minecraft");
            File assetsDir  = new File(gameDir, "assets");
            File nativesDir = new File(ctx.getFilesDir(), "natives/" + versionId);

            gameDir.mkdirs();
            assetsDir.mkdirs();
            nativesDir.mkdirs();

            File javaExec = new File(ctx.getFilesDir(), "jre/bin/java");
            if (!javaExec.exists()) {
                log.onLog("[ERRO] JRE não encontrado. Coloque o JRE ARM64 em files/jre/");
                return;
            }
            javaExec.setExecutable(true);

            ProfileManager pm  = new ProfileManager(ctx);
            String offlineUUID = pm.getOfflineUUID(username);

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExec.getAbsolutePath());
            cmd.add("-Xmx512m");
            cmd.add("-Xms256m");
            cmd.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
            cmd.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
            cmd.add("-cp");
            cmd.add(jarFile.getAbsolutePath());
            cmd.add("net.minecraft.client.main.Main");
            cmd.add("--username");    cmd.add(username);
            cmd.add("--uuid");        cmd.add(offlineUUID);
            cmd.add("--accessToken"); cmd.add("0");
            cmd.add("--userType");    cmd.add("legacy");
            cmd.add("--gameDir");     cmd.add(gameDir.getAbsolutePath());
            cmd.add("--assetsDir");   cmd.add(assetsDir.getAbsolutePath());
            cmd.add("--version");     cmd.add(versionId);

            log.onLog("Executando: " + String.join(" ", cmd));

            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.environment().put("HOME", ctx.getFilesDir().getAbsolutePath());
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                byte[] buf = new byte[1024];
                int read;
                while ((read = proc.getInputStream().read(buf)) != -1) {
                    log.onLog(new String(buf, 0, read));
                }

                int exit = proc.waitFor();
                log.onLog("Processo encerrado com código: " + exit);

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Erro ao iniciar processo", e);
                log.onLog("[ERRO] " + e.getMessage());
            }
        }).start();
    }
}
