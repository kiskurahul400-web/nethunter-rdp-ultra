package com.offsec.nethunter.rdp.input;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.offsec.nethunter.rdp.R;

/**
 * NetHunter Floating Hacker SuperBar.
 * Provides on-screen modifier keys (Ctrl, Alt, Shift, Win, Esc, Tab, Arrows, CAD).
 * Supports state toggling (latching), dragging, and collapsing.
 */
public class SuperBarOverlay {

    public interface KeyboardBridge {
        void sendKeyDown(int rdpScancode);
        void sendKeyUp(int rdpScancode);
        void sendSpecialAction(String action);
    }

    // Windows RDP Scancodes (Set 1 Make/Break)
    public static final int SCANCODE_ESC       = 0x0001;
    public static final int SCANCODE_TAB       = 0x000F;
    public static final int SCANCODE_CTRL      = 0x001D;
    public static final int SCANCODE_ALT       = 0x0038;
    public static final int SCANCODE_SHIFT     = 0x002A;
    public static final int SCANCODE_WIN       = 0x005B | 0x0100; // Extended Super
    public static final int SCANCODE_DELETE    = 0x0053 | 0x0100;
    public static final int SCANCODE_UP        = 0x0048 | 0x0100;
    public static final int SCANCODE_DOWN      = 0x0050 | 0x0100;
    public static final int SCANCODE_LEFT      = 0x004B | 0x0100;
    public static final int SCANCODE_RIGHT     = 0x004D | 0x0100;

    private final View superbarContainer;
    private final View restorePillButton;
    private final KeyboardBridge keyboardBridge;

    // Sticky Modifier States
    private boolean isCtrlActive = false;
    private boolean isAltActive = false;
    private boolean isShiftActive = false;
    private boolean isWinActive = false;

    // Drag tracking
    private float dX = 0f;
    private float dY = 0f;

    public SuperBarOverlay(final View container, final View restorePill, KeyboardBridge bridge) {
        this.superbarContainer = container;
        this.restorePillButton = restorePill;
        this.keyboardBridge = bridge;

        setupDragHandle();
        setupModifierButtons();
        setupActionButtons();
    }

    private void setupDragHandle() {
        View dragHandle = superbarContainer.findViewById(R.id.btn_drag_handle);
        if (dragHandle != null) {
            dragHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            dX = superbarContainer.getX() - event.getRawX();
                            dY = superbarContainer.getY() - event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            superbarContainer.animate()
                                    .x(event.getRawX() + dX)
                                    .y(event.getRawY() + dY)
                                    .setDuration(0)
                                    .start();
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    private void setupModifierButtons() {
        final Button btnCtrl = superbarContainer.findViewById(R.id.btn_ctrl);
        btnCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCtrlActive = !isCtrlActive;
                btnCtrl.setSelected(isCtrlActive);
                toggleKey(SCANCODE_CTRL, isCtrlActive);
            }
        });

        final Button btnAlt = superbarContainer.findViewById(R.id.btn_alt);
        btnAlt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAltActive = !isAltActive;
                btnAlt.setSelected(isAltActive);
                toggleKey(SCANCODE_ALT, isAltActive);
            }
        });

        final Button btnShift = superbarContainer.findViewById(R.id.btn_shift);
        btnShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShiftActive = !isShiftActive;
                btnShift.setSelected(isShiftActive);
                toggleKey(SCANCODE_SHIFT, isShiftActive);
            }
        });

        final Button btnWin = superbarContainer.findViewById(R.id.btn_win);
        btnWin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWinActive = !isWinActive;
                btnWin.setSelected(isWinActive);
                toggleKey(SCANCODE_WIN, isWinActive);
            }
        });
    }

    private void setupActionButtons() {
        bindInstantKey(R.id.btn_esc, SCANCODE_ESC);
        bindInstantKey(R.id.btn_tab, SCANCODE_TAB);
        bindInstantKey(R.id.btn_del, SCANCODE_DELETE);
        bindInstantKey(R.id.btn_up, SCANCODE_UP);
        bindInstantKey(R.id.btn_down, SCANCODE_DOWN);
        bindInstantKey(R.id.btn_left, SCANCODE_LEFT);
        bindInstantKey(R.id.btn_right, SCANCODE_RIGHT);

        // Ctrl + Alt + Del (CAD)
        Button btnCad = superbarContainer.findViewById(R.id.btn_taskmgr);
        btnCad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (keyboardBridge != null) {
                    keyboardBridge.sendSpecialAction("CTRL_ALT_DEL");
                }
            }
        });

        // Toggle Android Soft Keyboard
        Button btnToggleKbd = superbarContainer.findViewById(R.id.btn_toggle_kbd);
        btnToggleKbd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        // Minimize SuperBar
        Button btnCollapse = superbarContainer.findViewById(R.id.btn_collapse);
        btnCollapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                superbarContainer.setVisibility(View.GONE);
                restorePillButton.setVisibility(View.VISIBLE);
            }
        });

        // Restore SuperBar from Pill
        restorePillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restorePillButton.setVisibility(View.GONE);
                superbarContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindInstantKey(int viewId, final int scancode) {
        View btn = superbarContainer.findViewById(viewId);
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (keyboardBridge != null) {
                        keyboardBridge.sendKeyDown(scancode);
                        v.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                keyboardBridge.sendKeyUp(scancode);
                            }
                        }, 50);
                    }
                }
            });
        }
    }

    private void toggleKey(int scancode, boolean isDown) {
        if (keyboardBridge != null) {
            if (isDown) {
                keyboardBridge.sendKeyDown(scancode);
            } else {
                keyboardBridge.sendKeyUp(scancode);
            }
        }
    }
}
