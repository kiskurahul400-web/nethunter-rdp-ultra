package com.offsec.nethunter.rdp.engine;

import java.io.Serializable;

/**
 * High-Performance RDP Engine Configuration for Windows Hosts.
 * Configured specifically to negotiate 60 FPS H.264 AVC444 / RemoteFX
 * and low-latency network parameters.
 */
public class RdpEngineConfig implements Serializable {

    public enum Preset {
        ULTRA_60FPS_LAN,
        BALANCED_REMOTE,
        LOW_BANDWIDTH
    }

    public enum InputMode {
        TOUCHPAD_KEX,
        DIRECT_TOUCH
    }

    private String host;
    private int port = 3389;
    private String username;
    private String password;
    private String domain = "";
    private Preset preset = Preset.ULTRA_60FPS_LAN;
    private InputMode inputMode = InputMode.TOUCHPAD_KEX;

    // Display & Graphics
    private int desktopWidth = 1920;
    private int desktopHeight = 1080;
    private int colorDepth = 32;
    private int targetFps = 60;
    private boolean enableGfxAvc444 = true;
    private boolean enableRemoteFx = true;
    private boolean enableMediaCodecHardware = true;

    // Network & Socket
    private boolean tcpNoDelay = true; // Disable Nagle's algorithm (critical for input speed)
    private int socketRcvBufSize = 1048576; // 1 MB buffer
    private int socketSndBufSize = 524288;  // 512 KB buffer

    // Audio & Redirection
    private boolean redirectAudio = true;
    private boolean lowLatencyAudio = true;
    private boolean syncClipboard = true;

    public RdpEngineConfig(String host, int port, String username, String password, String domain, Preset preset, InputMode inputMode) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.preset = preset;
        this.inputMode = inputMode;
        applyPreset(preset);
    }

    public void applyPreset(Preset p) {
        this.preset = p;
        switch (p) {
            case ULTRA_60FPS_LAN:
                this.targetFps = 60;
                this.enableGfxAvc444 = true;
                this.enableRemoteFx = true;
                this.enableMediaCodecHardware = true;
                this.colorDepth = 32;
                this.tcpNoDelay = true;
                break;
            case BALANCED_REMOTE:
                this.targetFps = 45;
                this.enableGfxAvc444 = false;
                this.enableRemoteFx = true;
                this.enableMediaCodecHardware = true;
                this.colorDepth = 24;
                this.tcpNoDelay = true;
                break;
            case LOW_BANDWIDTH:
                this.targetFps = 30;
                this.enableGfxAvc444 = false;
                this.enableRemoteFx = false;
                this.enableMediaCodecHardware = true;
                this.colorDepth = 16;
                this.tcpNoDelay = true;
                break;
        }
    }

    // Getters and Setters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDomain() { return domain; }
    public Preset getPreset() { return preset; }
    public InputMode getInputMode() { return inputMode; }
    public int getDesktopWidth() { return desktopWidth; }
    public void setDesktopWidth(int w) { this.desktopWidth = w; }
    public int getDesktopHeight() { return desktopHeight; }
    public void setDesktopHeight(int h) { this.desktopHeight = h; }
    public int getTargetFps() { return targetFps; }
    public boolean isEnableGfxAvc444() { return enableGfxAvc444; }
    public boolean isEnableRemoteFx() { return enableRemoteFx; }
    public boolean isEnableMediaCodecHardware() { return enableMediaCodecHardware; }
    public boolean isTcpNoDelay() { return tcpNoDelay; }
    public int getSocketRcvBufSize() { return socketRcvBufSize; }
    public int getSocketSndBufSize() { return socketSndBufSize; }
    public boolean isRedirectAudio() { return redirectAudio; }
    public boolean isSyncClipboard() { return syncClipboard; }
}
