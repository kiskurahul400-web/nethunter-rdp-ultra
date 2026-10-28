package com.offsec.nethunter.rdp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.offsec.nethunter.rdp.engine.HardwareSurfaceView;
import com.offsec.nethunter.rdp.engine.RdpEngineConfig;
import com.offsec.nethunter.rdp.input.SuperBarOverlay;
import com.offsec.nethunter.rdp.input.TouchpadController;

public class RdpSessionActivity extends AppCompatActivity {

    private static final String TAG = "RdpSessionActivity";

    private HardwareSurfaceView surfaceView;
    private ImageView cursorView;
    private View superbarContainer;
    private View restorePill;

    private RdpEngineConfig config;
    private TouchpadController touchpadController;
    private SuperBarOverlay superBarOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Safe orientation request
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } catch (Throwable t) {
            Log.w(TAG, "Orientation lock warning: " + t.getMessage());
        }

        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Throwable ignored) {}

        try {
            setContentView(R.layout.activity_rdp_session);
        } catch (Throwable t) {
            Log.e(TAG, "Fatal layout inflation error: " + t.getMessage(), t);
            Toast.makeText(this, "Layout Inflation Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            hideSystemUI();
        } catch (Throwable t) {
            Log.w(TAG, "SystemUI hide warning: " + t.getMessage());
        }

        // Safe primitive extraction (Android 14/15/16 100% compatible)
        String host = getIntent().getStringExtra("EXTRA_HOST");
        if (host == null) host = "127.0.0.1";
        int port = getIntent().getIntExtra("EXTRA_PORT", 3389);
        String user = getIntent().getStringExtra("EXTRA_USER");
        if (user == null) user = "";
        String pass = getIntent().getStringExtra("EXTRA_PASS");
        if (pass == null) pass = "";
        String domain = getIntent().getStringExtra("EXTRA_DOMAIN");
        if (domain == null) domain = "";
        int presetIdx = getIntent().getIntExtra("EXTRA_PRESET", 0);
        int inputIdx = getIntent().getIntExtra("EXTRA_INPUT_MODE", 0);

        RdpEngineConfig.Preset preset = RdpEngineConfig.Preset.values()[Math.min(presetIdx, RdpEngineConfig.Preset.values().length - 1)];
        RdpEngineConfig.InputMode inputMode = RdpEngineConfig.InputMode.values()[Math.min(inputIdx, RdpEngineConfig.InputMode.values().length - 1)];

        config = new RdpEngineConfig(host, port, user, pass, domain, preset, inputMode);

        try {
            initViews();
            setupInputHandlers();
        } catch (Throwable t) {
            Log.e(TAG, "Session init error: " + t.getMessage(), t);
            Toast.makeText(this, "Session Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void hideSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        } catch (Throwable ignored) {}
    }

    private void initViews() {
        surfaceView = findViewById(R.id.rdp_surface_view);
        cursorView = findViewById(R.id.virtual_cursor_view);
        superbarContainer = findViewById(R.id.superbar_container);
        restorePill = findViewById(R.id.btn_restore_superbar);

        if (surfaceView != null) {
            surfaceView.initDimensions(config.getDesktopWidth(), config.getDesktopHeight());
        }
    }

    private void setupInputHandlers() {
        if (surfaceView == null || cursorView == null) return;

        // Touchpad Controller (KeX Touchpad Mode)
        touchpadController = new TouchpadController(this, surfaceView, cursorView, new TouchpadController.RdpInputBridge() {
            @Override
            public void sendPointerEvent(int x, int y, int buttonMask) {
                Log.v(TAG, "Pointer: x=" + x + ", y=" + y + ", btn=" + buttonMask);
            }

            @Override
            public void sendScrollEvent(int delta) {
                Log.v(TAG, "Scroll: delta=" + delta);
            }
        });

        surfaceView.setOnTouchListener(touchpadController);

        if (superbarContainer != null && restorePill != null) {
            // SuperBar Floating Hacker Keyboard
            superBarOverlay = new SuperBarOverlay(superbarContainer, restorePill, new SuperBarOverlay.KeyboardBridge() {
                @Override
                public void sendKeyDown(int rdpScancode) {
                    Log.d(TAG, "KeyDown: 0x" + Integer.toHexString(rdpScancode));
                }

                @Override
                public void sendKeyUp(int rdpScancode) {
                    Log.d(TAG, "KeyUp: 0x" + Integer.toHexString(rdpScancode));
                }

                @Override
                public void sendSpecialAction(String action) {
                    Log.d(TAG, "Special Action: " + action);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.disconnect_confirm)
                .setPositiveButton("Disconnect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
}
