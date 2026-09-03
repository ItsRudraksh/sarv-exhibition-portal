# Start the packaged portal on this Windows Server.
# Port 80 requires an elevated PowerShell (or a Windows service running as LocalSystem).
#   Set-ExecutionPolicy -Scope Process Bypass
#   C:\exhibition-portal\start-portal.ps1

$ErrorActionPreference = 'Stop'

$installDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $installDir) {
    $installDir = 'C:\exhibition-portal'
}

$envFile = Join-Path $installDir 'portal.env.ps1'
if (-not (Test-Path $envFile)) {
    Write-Error "Missing $envFile. Copy deploy\windows\portal.env.example.ps1 there and set passwords."
}
. $envFile

$jar = Join-Path $installDir 'exhibition-portal.jar'
if (-not (Test-Path $jar)) {
    Write-Error "Missing $jar. Run deploy\windows\deploy.ps1 first."
}

if (-not $env:DATASOURCE_PASSWORD -or $env:DATASOURCE_PASSWORD -eq 'change-me-db') {
    Write-Error 'Set DATASOURCE_PASSWORD in portal.env.ps1 to a real database password before a public start.'
}
if (-not $env:EXHIBITION_STAFF_BOOTSTRAP_PASSWORD -or $env:EXHIBITION_STAFF_BOOTSTRAP_PASSWORD -eq 'change-me-staff') {
    Write-Error 'Set EXHIBITION_STAFF_BOOTSTRAP_PASSWORD in portal.env.ps1. Do not ship poc-staff on a public IP.'
}

$iis = Get-Service -Name W3SVC -ErrorAction SilentlyContinue
if ($iis -and $iis.Status -eq 'Running' -and $env:SERVER_PORT -eq '80') {
    Write-Host 'IIS (W3SVC) is running and usually owns port 80. Stop it or change SERVER_PORT.'
    Write-Host '  Stop-Service W3SVC'
}

New-Item -ItemType Directory -Force -Path $env:EXHIBITION_STORAGE_ROOT | Out-Null

Write-Host "Starting $jar on port $($env:SERVER_PORT) (profile $($env:SPRING_PROFILES_ACTIVE))"
& java -jar $jar
