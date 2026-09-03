# Install Windows service exhibition-portal (Jenkins: net stop / net start), same idea as pharma-erp.
# MUST run elevated. Requires C:\exhibition-portal\start-portal.ps1 and portal.env.ps1.
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\deploy\windows\install-service.ps1
# Staging:
#   .\deploy\windows\install-service.ps1 -Staging

[CmdletBinding()]
param(
    [switch] $Staging
)

$ErrorActionPreference = 'Stop'

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error 'Run this script in an elevated PowerShell.'
    exit 1
}

if ($Staging) {
    $installDir = 'C:\exhibition-portal-staging'
    $serviceName = 'exhibition-portal-staging'
    $displayName = 'Sarv Exhibition Portal (staging)'
} else {
    $installDir = 'C:\exhibition-portal'
    $serviceName = 'exhibition-portal'
    $displayName = 'Sarv Exhibition Portal'
}

$start = Join-Path $installDir 'start-portal.ps1'
if (-not (Test-Path $start)) {
    Write-Error "Missing $start. Run deploy\windows\deploy.ps1 first (or copy start-portal.ps1 + portal.env.ps1)."
}

$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if ($existing) {
    if ($existing.Status -eq 'Running') {
        Stop-Service -Name $serviceName -Force
    }
    sc.exe delete $serviceName | Out-Null
    Start-Sleep -Seconds 2
}

$powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$binPath = "`"$powershell`" -NoProfile -ExecutionPolicy Bypass -File `"$start`""

New-Service -Name $serviceName -BinaryPathName $binPath -DisplayName $displayName -StartupType Automatic | Out-Null
Write-Host "Installed service $serviceName -> $start"
Write-Host "Start: net start $serviceName"
Write-Host "Jenkins copies exhibition-portal.jar into $installDir then net stop / net start."
