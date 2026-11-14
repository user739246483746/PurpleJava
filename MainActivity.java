package com.purplejava;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.purplejava.databinding.ActivityMainBinding;

/**
 * Tela principal: exibe conta local ativa, lista versões e inicia download/jogo.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProfileManager profileManager;
    private VersionFetcher versionFetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileManager = new ProfileManager(this);
        versionFetcher = new VersionFetcher(this);

        // Mostra nome do jogador local
        String username = profileManager.getUsername();
        binding.tvUsername.setText(username.isEmpty() ? "Sem conta" : username);

        // Botão editar conta local
        binding.btnEditProfile.setOnClickListener(v -> showProfileDialog());

        // Carrega lista de versões da Mojang
        loadVersionList();

        // Botão jogar
        binding.btnPlay.setOnClickListener(v -> {
            String selected = (String) binding.spinnerVersions.getSelectedItem();
            if (selected == null || selected.isEmpty()) {
                Toast.makeText(this, "Selecione uma versão", Toast.LENGTH_SHORT).show();
                return;
            }
            String currentUser = profileManager.getUsername();
            if (currentUser.isEmpty()) {
                Toast.makeText(this, "Configure uma conta local primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
            startGame(selected, currentUser);
        });
    }

    private void loadVersionList() {
        binding.btnPlay.setEnabled(false);
        binding.tvStatus.setText("Buscando versões...");

        versionFetcher.fetchVersionList(versions -> runOnUiThread(() -> {
            if (versions == null || versions.isEmpty()) {
                binding.tvStatus.setText("Falha ao buscar versões");
                return;
            }
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    versions
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerVersions.setAdapter(adapter);
            binding.tvStatus.setText("Pronto");
            binding.btnPlay.setEnabled(true);
        }));
    }

    private void showProfileDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Conta Local");

        final android.widget.EditText input = new android.widget.EditText(this);
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
            binding.tvUsername.setText(name);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void startGame(String version, String username) {
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.putExtra("version", version);
        intent.putExtra("username", username);
        startActivity(intent);
    }
}
