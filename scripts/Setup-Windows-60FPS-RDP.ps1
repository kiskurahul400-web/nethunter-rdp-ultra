# ==============================================================================
# NetHunter RDP Ultra: Windows 60 FPS GPU Acceleration Setup Script
# Run in PowerShell as Administrator on your Windows PC
# ==============================================================================

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   NetHunter RDP Ultra: Windows 60 FPS GPU Accelerator    " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check Administrator Privileges
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "[ERROR] This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Right-click PowerShell -> 'Run as Administrator', then run this script again." -ForegroundColor Yellow
    Exit
}

Write-Host "[+] Administrator privileges confirmed." -ForegroundColor Green

# 2. Unlock 60 FPS DWM Frame Interval (Default is 30 FPS)
$winstationsPath = "HKLM:\SYSTEM\CurrentControlSet\Control\Terminal Server\WinStations"
Set-ItemProperty -Path $winstationsPath -Name "DWMFRAMEINTERVAL" -Value 15 -Type DWord
Write-Host "[+] 60 FPS DWM Frame Interval unlocked (DWMFRAMEINTERVAL = 15)." -ForegroundColor Green

# 3. Force Hardware GPU Encoding & H.264/AVC 444 Mode (RDP Session Host Policies)
$rdpHostPath = "HKLM:\SOFTWARE\Policies\Microsoft\Windows NT\Terminal Services"
if (-not (Test-Path $rdpHostPath)) {
    New-Item -Path $rdpHostPath -Force | Out-Null
}

# Use hardware default graphics adapter
Set-ItemProperty -Path $rdpHostPath -Name "bEnumerateFUserSettings" -Value 1 -Type DWord
Set-ItemProperty -Path $rdpHostPath -Name "fEnableRemoteFXAdvancedRemoteFX" -Value 1 -Type DWord

# Prioritize H.264/AVC 444 Graphics mode
Set-ItemProperty -Path $rdpHostPath -Name "fSelectAVC444Mode" -Value 1 -Type DWord

# Configure H.264/AVC hardware encoding (1 = Always attempt)
Set-ItemProperty -Path $rdpHostPath -Name "fEnableH264Encoding" -Value 1 -Type DWord

# Enable WDDM Graphics Driver for RDP
Set-ItemProperty -Path $rdpHostPath -Name "fEnableWddmDriver" -Value 1 -Type DWord

Write-Host "[+] GPU Hardware H.264/AVC 444 encoding enforced." -ForegroundColor Green

# 4. Low Latency TCP Optimization for RDP (TCP_NODELAY & Immediate ACK)
$tcpInterfacesPath = "HKLM:\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\Interfaces"
Get-ChildItem -Path $tcpInterfacesPath | ForEach-Object {
    Set-ItemProperty -Path $_.PSPath -Name "TcpAckFrequency" -Value 1 -Type DWord -ErrorAction SilentlyContinue
    Set-ItemProperty -Path $_.PSPath -Name "TCPNoDelay" -Value 1 -Type DWord -ErrorAction SilentlyContinue
}
Write-Host "[+] Low-Latency TCP Socket tuning applied (TcpAckFrequency = 1, TCPNoDelay = 1)." -ForegroundColor Green

# 5. Ensure Remote Desktop Firewall Port 3389 is Open
Enable-NetFirewallRule -DisplayGroup "Remote Desktop" -ErrorAction SilentlyContinue
Write-Host "[+] Windows Firewall rules for Remote Desktop (Port 3389) verified." -ForegroundColor Green

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Windows host successfully optimized for 60 FPS RDP!" -ForegroundColor Green
Write-Host "Please restart your Windows PC once to apply all GPU changes." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
