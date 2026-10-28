@echo off
:: NetHunter RDP Ultra: Windows 60 FPS GPU Acceleration Setup
:: Right-click and choose "Run as administrator"

echo ==========================================================
echo    NetHunter RDP Ultra: Windows 60 FPS GPU Accelerator    
echo ==========================================================

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Please right-click this batch file and select "Run as administrator".
    pause
    exit /b 1
)

echo [+] Unlocking 60 FPS DWM Frame Interval...
reg add "HKLM\SYSTEM\CurrentControlSet\Control\Terminal Server\WinStations" /v DWMFRAMEINTERVAL /t REG_DWORD /d 15 /f >nul

echo [+] Enforcing H.264 / AVC 444 GPU Hardware Encoding...
reg add "HKLM\SOFTWARE\Policies\Microsoft\Windows NT\Terminal Services" /v fSelectAVC444Mode /t REG_DWORD /d 1 /f >nul
reg add "HKLM\SOFTWARE\Policies\Microsoft\Windows NT\Terminal Services" /v fEnableH264Encoding /t REG_DWORD /d 1 /f >nul
reg add "HKLM\SOFTWARE\Policies\Microsoft\Windows NT\Terminal Services" /v fEnableWddmDriver /t REG_DWORD /d 1 /f >nul
reg add "HKLM\SOFTWARE\Policies\Microsoft\Windows NT\Terminal Services" /v bEnumerateFUserSettings /t REG_DWORD /d 1 /f >nul

echo [+] Enabling Remote Desktop in Windows Firewall...
netsh advfirewall firewall set rule group="remote desktop" new enable=Yes >nul

echo ==========================================================
echo [SUCCESS] Windows 60 FPS GPU Optimization Complete!
echo Please restart your Windows PC to apply all changes.
echo ==========================================================
pause
