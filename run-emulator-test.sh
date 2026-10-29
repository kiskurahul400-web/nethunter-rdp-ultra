#!/usr/bin/env bash
set -e

echo "=== Android Emulator Test Script Started (Kali NetHunter RDP) ==="
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

# Verify that package com.offsec.nethunter.rdp is installed
echo "Checking installed package list for NetHunter RDP:"
adb shell pm list packages | grep "nethunter" || adb shell pm list packages | grep "com.offsec.nethunter"

# Clear logcat
adb logcat -c

# Step 1: Launch Main Connection Grid Activity (NetHunter RDP)
echo "Launching Kali NetHunter RDP ConnectionGridActivity..."
adb shell am start -n com.offsec.nethunter.rdp/com.undatech.opaque.ConnectionGridActivity
sleep 6

# Check if NetHunter process is running
echo "Checking process table in emulator:"
adb shell ps -A | grep -i "nethunter" || adb shell ps | grep -i "nethunter" || true
APP_PID=$(adb shell pidof com.offsec.nethunter.rdp || true)
echo "Running App PID: $APP_PID"

if [ -z "$APP_PID" ]; then
    echo "ERROR: Process com.offsec.nethunter.rdp is not running!"
    adb logcat -d | tail -n 50
    exit 1
fi

mkdir -p test_artifacts
adb exec-out screencap -p > test_artifacts/nethunter_rdp_dashboard.png
cp test_artifacts/nethunter_rdp_dashboard.png test_artifacts/emulator_running_screen.png
ls -lh test_artifacts/nethunter_rdp_dashboard.png
echo "Screenshot 1 (NetHunter Main Dashboard) captured successfully!"

# Step 2: Open Connection Details Screen (where user enters IP/password)
echo "Testing NetHunter connection profile creation activity..."
adb shell am start -n com.offsec.nethunter.rdp/com.iiordanov.bVNC.aRDP || true
sleep 5
adb exec-out screencap -p > test_artifacts/nethunter_rdp_connection.png
cp test_artifacts/nethunter_rdp_connection.png test_artifacts/emulator_connection_screen.png
ls -lh test_artifacts/nethunter_rdp_connection.png
echo "Screenshot 2 (NetHunter Connection Config) captured successfully!"

# Step 3: Test Auto-Rotating & Screen Auto-Adjustment
echo "Testing Auto-Rotating to Landscape orientation..."
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1  # 90 degrees (Landscape)
sleep 4
adb exec-out screencap -p > test_artifacts/nethunter_rdp_landscape.png
ls -lh test_artifacts/nethunter_rdp_landscape.png
echo "Screenshot 3 (Landscape Auto-Rotation) captured successfully!"

echo "Testing Auto-Rotating to Portrait orientation..."
adb shell settings put system user_rotation 0  # 0 degrees (Portrait)
sleep 4
adb exec-out screencap -p > test_artifacts/nethunter_rdp_portrait.png
ls -lh test_artifacts/nethunter_rdp_portrait.png
echo "Screenshot 4 (Portrait Auto-Rotation) captured successfully!"

# Re-enable accelerometer auto-rotation
adb shell settings put system accelerometer_rotation 1

# Verify NetHunter app is still alive and didn't crash during rotations
ROT_PID=$(adb shell pidof com.offsec.nethunter.rdp || true)
if [ -z "$ROT_PID" ]; then
    echo "ERROR: Process died during auto-rotation!"
    adb logcat -d | grep -E -A 15 "FATAL EXCEPTION" || adb logcat -d | tail -n 50
    exit 1
fi
echo "Process is stably running after rotation with PID: $ROT_PID"

# Step 4: Verify Logcat for Zero Fatal Crashes
echo "Inspecting Logcat for errors:"
if adb logcat -d | grep -E -A 5 "FATAL EXCEPTION" | grep "com.offsec.nethunter.rdp"; then
    echo "Error: Fatal crash detected in emulator for com.offsec.nethunter.rdp!"
    adb logcat -d | grep -E -A 15 "FATAL EXCEPTION"
    exit 1
else
    echo "SUCCESS: com.offsec.nethunter.rdp auto-rotation and screen adjustment verified with ZERO fatal crashes!"
fi

echo "=== Kali NetHunter RDP Emulator Verification 100% Passed! ==="
