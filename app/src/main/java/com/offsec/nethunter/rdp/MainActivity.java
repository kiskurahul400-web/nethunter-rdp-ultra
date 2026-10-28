package com.offsec.nethunter.rdp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.offsec.nethunter.rdp.engine.RdpEngineConfig;
import com.offsec.nethunter.rdp.security.ConnectionStorage;

public class MainActivity extends AppCompatActivity {

    private EditText editHost;
    private EditText editPort;
    private EditText editUsername;
    private EditText editPassword;
    private EditText editDomain;
    private Spinner spinnerPreset;
    private Spinner spinnerInputMode;
    private MaterialButton btnConnect;

    private ConnectionStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new ConnectionStorage(this);
        initViews();
        loadSavedConfig();
    }

    private void initViews() {
        editHost = findViewById(R.id.edit_host);
        editPort = findViewById(R.id.edit_port);
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        editDomain = findViewById(R.id.edit_domain);
        spinnerPreset = findViewById(R.id.spinner_perf_preset);
        spinnerInputMode = findViewById(R.id.spinner_input_mode);
        btnConnect = findViewById(R.id.btn_connect);

        // Populate Performance Presets
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        getString(R.string.preset_ultra),
                        getString(R.string.preset_balanced),
                        getString(R.string.preset_low_bandwidth)
                });
        spinnerPreset.setAdapter(presetAdapter);

        // Populate Input Modes
        ArrayAdapter<String> inputAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        getString(R.string.mode_touchpad),
                        getString(R.string.mode_direct)
                });
        spinnerInputMode.setAdapter(inputAdapter);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptConnect();
            }
        });
    }

    private void loadSavedConfig() {
        RdpEngineConfig saved = storage.loadConfig();
        if (!TextUtils.isEmpty(saved.getHost())) {
            editHost.setText(saved.getHost());
            editPort.setText(String.valueOf(saved.getPort()));
            editUsername.setText(saved.getUsername());
            editPassword.setText(saved.getPassword());
            editDomain.setText(saved.getDomain());
            spinnerPreset.setSelection(saved.getPreset().ordinal());
            spinnerInputMode.setSelection(saved.getInputMode().ordinal());
        }
    }

    private void attemptConnect() {
        String host = editHost.getText().toString().trim();
        String portStr = editPort.getText().toString().trim();
        String user = editUsername.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();
        String domain = editDomain.getText().toString().trim();

        if (TextUtils.isEmpty(host)) {
            editHost.setError("Host IP or hostname is required");
            editHost.requestFocus();
            return;
        }

        int port = 3389;
        try {
            if (!TextUtils.isEmpty(portStr)) {
                port = Integer.parseInt(portStr);
            }
        } catch (NumberFormatException e) {
            port = 3389;
        }

        RdpEngineConfig.Preset preset = RdpEngineConfig.Preset.values()[spinnerPreset.getSelectedItemPosition()];
        RdpEngineConfig.InputMode inputMode = RdpEngineConfig.InputMode.values()[spinnerInputMode.getSelectedItemPosition()];

        RdpEngineConfig config = new RdpEngineConfig(host, port, user, pass, domain, preset, inputMode);
        storage.saveConfig(config);

        Toast.makeText(this, R.string.connecting_msg, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, RdpSessionActivity.class);
        intent.putExtra("EXTRA_HOST", host);
        intent.putExtra("EXTRA_PORT", port);
        intent.putExtra("EXTRA_USER", user);
        intent.putExtra("EXTRA_PASS", pass);
        intent.putExtra("EXTRA_DOMAIN", domain);
        intent.putExtra("EXTRA_PRESET", preset.ordinal());
        intent.putExtra("EXTRA_INPUT_MODE", inputMode.ordinal());
        startActivity(intent);
    }
}
