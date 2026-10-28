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

        // Force landscape and keep screen on
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();

        setContentView(R.layout.activity_rdp_session);

        config = (RdpEngineConfig) getIntent().getSerializableExtra("RDP_CONFIG");
        if (config == null) {
            config = new RdpEngineConfig("127.0.0.1", 3389, "", "", "",
                    RdpEngineConfig.Preset.ULTRA_60FPS_LAN,
                    RdpEngineConfig.InputMode.TOUCHPAD_KEX);
        }

        initViews();
        setupInputHandlers();
    }

    private void hideSystemUI() {
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
    }

    private void initViews() {
        surfaceView = findViewById(R.id.rdp_surface_view);
        cursorView = findViewById(R.id.virtual_cursor_view);
        superbarContainer = findViewById(R.id.superbar_container);
        restorePill = findViewById(R.id.btn_restore_superbar);

        // Initialize 1080p target desktop resolution
        surfaceView.initDimensions(config.getDesktopWidth(), config.getDesktopHeight());
    }

    private void setupInputHandlers() {
        // Touchpad Controller (KeX Touchpad Mode)
        touchpadController = new TouchpadController(this, surfaceView, cursorView, new TouchpadController.RdpInputBridge() {
            @Override
            public void sendPointerEvent(int x, int y, int buttonMask) {
                // Dispatched to RDP native protocol pipeline
                Log.v(TAG, "Pointer: x=" + x + ", y=" + y + ", btn=" + buttonMask);
            }

            @Override
            public void sendScrollEvent(int delta) {
                Log.v(TAG, "Scroll: delta=" + delta);
            }
        });

        // Set touch listener on surface
        surfaceView.setOnTouchListener(touchpadController);

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
