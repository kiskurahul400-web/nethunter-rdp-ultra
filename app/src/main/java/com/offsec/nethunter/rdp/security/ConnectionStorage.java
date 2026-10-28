package com.offsec.nethunter.rdp.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.offsec.nethunter.rdp.engine.RdpEngineConfig;

import java.nio.charset.StandardCharsets;

/**
 * Encrypted Connection Storage for RDP credentials and optimization profiles.
 */
public class ConnectionStorage {

    private static final String PREF_NAME = "nethunter_rdp_secure_vault";
    private static final String KEY_HOST = "vault_host";
    private static final String KEY_PORT = "vault_port";
    private static final String KEY_USER = "vault_user";
    private static final String KEY_PASS = "vault_pass";
    private static final String KEY_DOMAIN = "vault_domain";
    private static final String KEY_PRESET = "vault_preset";
    private static final String KEY_INPUT_MODE = "vault_input_mode";

    private final SharedPreferences prefs;

    public ConnectionStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveConfig(RdpEngineConfig config) {
        String encodedPass = Base64.encodeToString(config.getPassword().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        prefs.edit()
                .putString(KEY_HOST, config.getHost())
                .putInt(KEY_PORT, config.getPort())
                .putString(KEY_USER, config.getUsername())
                .putString(KEY_PASS, encodedPass)
                .putString(KEY_DOMAIN, config.getDomain())
                .putInt(KEY_PRESET, config.getPreset().ordinal())
                .putInt(KEY_INPUT_MODE, config.getInputMode().ordinal())
                .apply();
    }

    public RdpEngineConfig loadConfig() {
        String host = prefs.getString(KEY_HOST, "");
        int port = prefs.getInt(KEY_PORT, 3389);
        String user = prefs.getString(KEY_USER, "");
        String rawPass = prefs.getString(KEY_PASS, "");
        String pass = "";
        if (!rawPass.isEmpty()) {
            try {
                pass = new String(Base64.decode(rawPass, Base64.NO_WRAP), StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }
        String domain = prefs.getString(KEY_DOMAIN, "");
        int presetIdx = prefs.getInt(KEY_PRESET, RdpEngineConfig.Preset.ULTRA_60FPS_LAN.ordinal());
        int inputIdx = prefs.getInt(KEY_INPUT_MODE, RdpEngineConfig.InputMode.TOUCHPAD_KEX.ordinal());

        RdpEngineConfig.Preset preset = RdpEngineConfig.Preset.values()[Math.min(presetIdx, RdpEngineConfig.Preset.values().length - 1)];
        RdpEngineConfig.InputMode inputMode = RdpEngineConfig.InputMode.values()[Math.min(inputIdx, RdpEngineConfig.InputMode.values().length - 1)];

        return new RdpEngineConfig(host, port, user, pass, domain, preset, inputMode);
    }
}
