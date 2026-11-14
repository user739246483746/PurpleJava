package com.purplejava;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvUsername;
    private TextView tvStatus;
    private Spinner spinnerVersions;
    private Button btnPlay;
    private ProfileManager profileManager;
    private VersionFetcher versionFetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUsername     = findViewById(R.id.tv_username);
        tvStatus       = findViewById(R.id.tv_status);
        spinnerVersions = findViewById(R.id.spinner_versions);
        btnPlay        = findViewById(R.id.btn_play);
        Button btnEditProfile = findViewById(R.id.btn_edit_profile);

        profileManager = new ProfileManager(this);
        versionFetcher = new VersionFetcher(this);

        String username = profileManager.getUsername();
        tvUsername.setText(username.isEmpty() ? "Sem conta" : username);

        btnEditProfile.setOnClickListener(v -> showProfileDialog());

        loadVersionList();

        btnPlay.setOnClickListener(v -> {
            String selected = (String) spinnerVersions.getSelectedItem();
            if (selected == null || selected.isEmpty()) {
                Toast.makeText(this, "Selecione uma versão", Toast.LENGTH_SHORT).show();
                return;
            }
            String currentUser = profileManager.getUsername();
            if (currentUser.isEmpty()) {
                Toast.makeText(this, "Configure uma conta local primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LauncherActivity.class);
            intent.putExtra("version", selected);
            intent.putExtra("username", currentUser);
            startActivity(intent);
        });
    }

    private void loadVersionList() {
        btnPlay.setEnabled(false);
        tvStatus.setText("Buscando versões...");

        versionFetcher.fetchVersionList(versions -> runOnUiThread(() -> {
            if (versions == null || versions.isEmpty()) {
                tvStatus.setText("Falha ao buscar versões");
                return;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    versions
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVersions.setAdapter(adapter);
            tvStatus.setText("Pronto");
            btnPlay.setEnabled(true);
        }));
    }

    private void showProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conta Local");

        final EditText input = new EditText(this);
        input.setHint("Nome de usuário");
        input.setText(profileManager.getUsername());
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Nome não pode ser vazio", Toast.LENGTH_SHORT).show();
                return;
            }
            profileManager.setUsername(name);
            tvUsername.setText(name);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}
