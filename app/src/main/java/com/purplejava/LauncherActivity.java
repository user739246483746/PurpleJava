package com.purplejava;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    private TextView tvLog;
    private GameDownloader downloader;
    private GameLauncher launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        tvLog = findViewById(R.id.tv_log);

        String version  = getIntent().getStringExtra("version");
        String username = getIntent().getStringExtra("username");

        downloader = new GameDownloader(this);
        launcher   = new GameLauncher(this);

        log("Versão: " + version);
        log("Jogador: " + username);
        log("Verificando arquivos...");

        downloader.ensureVersion(version, success -> runOnUiThread(() -> {
            if (!success) {
                log("[ERRO] Falha no download. Verifique a conexão.");
                return;
            }
            log("Arquivos OK. Iniciando...");
            launcher.launch(version, username, msg -> runOnUiThread(() -> log(msg)));
        }));
    }

    private void log(String msg) {
        tvLog.append(msg + "\n");
    }
}
