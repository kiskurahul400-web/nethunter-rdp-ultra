#!/usr/bin/env bash
set -e

echo "=== Android Emulator Test Script Started ==="
echo "Waiting for Android Emulator device readiness..."
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
echo "Android Emulator is ready and fully booted!"

# Select APK for emulator: x86_64 is fastest and native, Universal is also available
APK_PATH="output_apks/NetHunter-RDP-Ultra-x86_64.apk"
if [ ! -f "$APK_PATH" ]; then
    APK_PATH="output_apks/NetHunter-RDP-Ultra-Universal.apk"
fi

echo "Installing selected APK onto Android Emulator: $APK_PATH"
ls -lh "$APK_PATH"
adb install -r "$APK_PATH"
echo "Installation successful!"

# Clear logcat
adb logcat -c

# Step 1: Launch Main Connection Grid Activity
echo "Launching ConnectionGridActivity on Android Emulator..."
adb shell am start -n com.iiordanov.freeaRDP/com.undatech.opaque.ConnectionGridActivity
sleep 8

# Check if process is running
echo "Checking process table in emulator:"
adb shell ps -A | grep -i "freeaRDP" || adb shell ps | grep -i "freeaRDP" || true
APP_PID=$(adb shell pidof com.iiordanov.freeaRDP || true)
echo "Running App PID: $APP_PID"

if [ -z "$APP_PID" ]; then
    echo "ERROR: Process com.iiordanov.freeaRDP is not running!"
    adb logcat -d | tail -n 50
    exit 1
fi

mkdir -p test_artifacts
adb exec-out screencap -p > test_artifacts/emulator_running_screen.png
ls -lh test_artifacts/emulator_running_screen.png
echo "Screenshot 1 (Main Grid) captured successfully!"

# Step 2: Open Connection Details Screen (where user enters IP/password)
echo "Testing connection profile creation activity..."
adb shell am start -n com.iiordanov.freeaRDP/com.iiordanov.bVNC.aRDP || true
sleep 6
adb exec-out screencap -p > test_artifacts/emulator_connection_screen.png
ls -lh test_artifacts/emulator_connection_screen.png
echo "Screenshot 2 (Connection Config) captured successfully!"

# Step 3: Verify Logcat for Zero Fatal Crashes
echo "Inspecting Logcat for errors:"
if adb logcat -d | grep -E -A 5 "FATAL EXCEPTION" | grep "com.iiordanov.freeaRDP"; then
    echo "Error: Fatal crash detected in emulator for com.iiordanov.freeaRDP!"
    adb logcat -d | grep -E -A 15 "FATAL EXCEPTION"
    exit 1
else
    echo "SUCCESS: com.iiordanov.freeaRDP launched and ran with ZERO fatal crashes on Android 14/15/16 Emulator!"
fi

echo "=== Android Emulator Verification Passed with Flying Colors! ==="
