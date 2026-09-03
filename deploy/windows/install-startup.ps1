# Register a startup task so the portal starts after reboot.
# Prefer deploy\windows\install-service.ps1 (Jenkins uses net stop / net start).
# MUST run elevated.
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\deploy\windows\install-startup.ps1

$ErrorActionPreference = 'Stop'

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error 'Run this script in an elevated PowerShell.'
    exit 1
}

$start = 'C:\exhibition-portal\start-portal.ps1'
if (-not (Test-Path $start)) {
    Write-Error "Missing $start. Run deploy\windows\deploy.ps1 first."
}

$action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$start`""
$trigger = New-ScheduledTaskTrigger -AtStartup
$principalTask = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)

Register-ScheduledTask -TaskName 'SarvExhibitionPortal' -Action $action -Trigger $trigger -Principal $principalTask -Settings $settings -Force | Out-Null
Write-Host 'Registered scheduled task SarvExhibitionPortal (At startup, SYSTEM).'
Write-Host 'Start now: Start-ScheduledTask -TaskName SarvExhibitionPortal'
