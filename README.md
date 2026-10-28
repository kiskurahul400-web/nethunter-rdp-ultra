# NetHunter RDP Ultra (60 FPS Hardware-Accelerated Client)

A custom, ultra-optimized Remote Desktop (RDP) client for Android designed to connect to **Windows 10/11 RDP** at **60 FPS console smoothness**, while retaining the beloved **Kali NetHunter KeX** experience (Floating Hacker SuperBar, Precision Touchpad, Gesture Controls, and Encrypted Vault).

---

## ⚡ Why This Is Faster Than Standard FreeRDP / aRDP

| Problem in Standard FreeRDP / aRDP | How NetHunter RDP Ultra Fixes It |
| :--- | :--- |
| **Mouse Cursor Lag ("Rubber-banding")** | Built-in `InputEventCoalescer` throttles touch events to 60Hz VSYNC, discarding queue buildup. |
| **Frame Drops & Choppy Video (15-20 FPS)** | Dedicated `HardwareSurfaceView` with hardware double-buffering, completely bypassing UI thread locks. |
| **High Latency over WiFi / Internet** | `SocketTuner` enforces `TCP_NODELAY = 1` (disabling Nagle's algorithm) and sets high-priority QoS. |
| **Clunky Touch Experience** | NetHunter KeX **Precision Touchpad** with logarithmic mouse acceleration curve. |
| **Missing Windows Key Combinations** | Draggable **Floating SuperBar** with instant `Ctrl`, `Alt`, `Shift`, `Win`, `Esc`, `Tab`, `CAD` (Ctrl+Alt+Del). |

---

## 🚀 How to Build via GitHub Actions (Zero Local CPU Load)

All heavy C/C++ native compilation and Android SDK/NDK processing runs automatically in the cloud on **GitHub Actions**.

### Step 1: Push Code to Your GitHub Repository
Run these commands in this directory:
```bash
git init
git add .
git commit -m "feat: Initial NetHunter RDP Ultra project with 60FPS engine"
git branch -M main
git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPO>.git
git push -u origin main
```

### Step 2: Trigger the Cloud Build
1. Open your repository on GitHub.
2. Click on the **Actions** tab.
3. Select **Build NetHunter RDP Ultra APK** on the left.
4. Click **Run workflow** -> **Run workflow**.

### Step 3: Download Your APK
- Once the workflow completes (~4-6 minutes), scroll to the bottom of the run page.
- Under **Artifacts**, download **`NetHunter-RDP-Ultra-Release-APK`**.
- Unzip and install `NetHunter-RDP-Ultra-arm64.apk` directly on your Android device!

---

## 🖥️ Optimizing Your Windows PC (Crucial for 60 FPS)

Windows by default caps RDP to 30 FPS and uses CPU software rendering. To unlock true 60 FPS hardware GPU streaming:

1. Copy the `scripts/Setup-Windows-60FPS-RDP.bat` file to your Windows PC.
2. Right-click it and choose **Run as administrator**.
3. It will automatically:
   - Unlock 60 FPS RDP (`DWMFRAMEINTERVAL = 15`).
   - Force GPU H.264 / AVC 444 hardware video encoding.
   - Enforce WDDM graphics driver for RDP sessions.
   - Apply `TCPNoDelay` and `TcpAckFrequency = 1` for zero input latency.
4. Restart your Windows PC once.

---

## 📱 How to Connect from Your Phone

1. Open **NetHunter RDP Ultra** on your Android phone.
2. Enter your Windows details:
   - **Host:** Windows IP address (e.g. `192.168.1.100` or Tailscale/VPN IP).
   - **Port:** `3389` (default).
   - **Username:** Windows account name.
   - **Password:** Windows account password.
3. Select Optimization Preset:
   - **Ultra 60FPS LAN:** Full H.264 AVC444 + Zero-Copy Surface.
4. Tap **CONNECT ➔**.

### 🎮 Controls in Session:
- **Move Finger:** Move mouse pointer smoothly.
- **Single Tap:** Left Click.
- **Double Tap & Hold:** Click and Drag (window moving, text selection).
- **Two-Finger Tap:** Right Click.
- **Two-Finger Drag:** Mouse Scroll Wheel.
- **Floating SuperBar:** Drag by the `⋮⋮` handle. Tap `Ctrl`, `Alt`, `Shift`, `Win` to toggle modifier keys. Tap `✕` to minimize to a small floating pill.
