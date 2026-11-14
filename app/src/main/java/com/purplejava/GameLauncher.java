package com.purplejava;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

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

            // Extrai JRE do assets se ainda não extraído
            File jreDir = new File(ctx.getFilesDir(), "jre");
            if (!jreDir.exists()) {
                log.onLog("Extraindo JRE...");
                try {
                    jreDir.mkdirs();
                    InputStream assetIn = ctx.getAssets().open("jre.tar.gz");
                    extractTarGz(assetIn, ctx.getFilesDir());
                    log.onLog("JRE extraído.");
                } catch (Exception e) {
                    log.onLog("[ERRO] Falha ao extrair JRE: " + e.getMessage());
                    return;
                }
            }

            File javaExec = new File(ctx.getFilesDir(), "jre/bin/java");
            if (!javaExec.exists()) {
                log.onLog("[ERRO] JRE não encontrado após extração.");
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

    private void extractTarGz(InputStream in, File destDir) throws Exception {
        TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(in));
        TarArchiveEntry entry;
        while ((entry = tar.getNextTarEntry()) != null) {
            File out = new File(destDir, entry.getName());
            if (entry.isDirectory()) {
                out.mkdirs();
                continue;
            }
            out.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = tar.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            }
            if (entry.getName().contains("/bin/")) {
                out.setExecutable(true);
            }
        }
        tar.close();
    }
}
