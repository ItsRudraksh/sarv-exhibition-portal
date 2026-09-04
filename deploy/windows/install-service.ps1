# Install exhibition-portal as a real Windows service via WinSW.
# Plain `New-Service` + powershell -File start-portal.ps1 is NOT a valid SCM binary:
# net start fails with NET HELPMSG 2186 ("not responding to the control function").
# MUST run elevated (Jenkins LocalSystem is fine).
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
if (-not (Test-Path -LiteralPath $start)) {
    Write-Error "Missing $start. Run Jenkins Deploy (or deploy\windows\deploy.ps1) first so start-portal.ps1 is in the install dir."
    exit 1
}

New-Item -ItemType Directory -Force -Path $installDir | Out-Null

# WinSW 2.x — proper service control for java -jar (via start-portal.ps1).
$winswUrl = 'https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe'
$winswExe = Join-Path $installDir ($serviceName + '.exe')
$winswXml = Join-Path $installDir ($serviceName + '.xml')
$powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'

if (-not (Test-Path -LiteralPath $winswExe)) {
    Write-Host "Downloading WinSW to $winswExe ..."
    try {
        Invoke-WebRequest -Uri $winswUrl -OutFile $winswExe -UseBasicParsing
    } catch {
        Write-Error ("Failed to download WinSW from " + $winswUrl + ". On the server, open that URL in a browser or copy WinSW-x64.exe to " + $winswExe + " then re-run. " + $_.Exception.Message)
        exit 1
    }
}

$xml = @"
<service>
  <id>$serviceName</id>
  <name>$displayName</name>
  <description>Sarv Exhibition Portal (Spring Boot). Env from portal.env.ps1 via start-portal.ps1.</description>
  <executable>$powershell</executable>
  <arguments>-NoProfile -ExecutionPolicy Bypass -File "%BASE%\start-portal.ps1"</arguments>
  <workingdirectory>%BASE%</workingdirectory>
  <logmode>roll</logmode>
  <onfailure action="restart" delay="10 sec"/>
</service>
"@
Set-Content -LiteralPath $winswXml -Value $xml -Encoding UTF8

function Stop-OrphanPortalJava {
    param([string] $Dir)
    $needle = Join-Path $Dir 'exhibition-portal.jar'
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and ($_.CommandLine -like "*$needle*" -or $_.CommandLine -like "*exhibition-portal.jar*") -and $_.CommandLine -like "*$Dir*" } |
        ForEach-Object {
            Write-Host ("Stopping orphan java PID {0} (manual Option A / prior start)" -f $_.ProcessId)
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
$pathName = $null
if ($existing) {
    $pathName = (Get-CimInstance Win32_Service -Filter ("Name='" + $serviceName + "'") -ErrorAction SilentlyContinue).PathName
}

$isWinsw = $pathName -and ($pathName -like ('*' + $serviceName + '.exe*'))

if ($existing -and -not $isWinsw) {
    Write-Host "Existing service $serviceName is not WinSW (PathName points at powershell only). That causes NET 2186. Replacing..."
    if ($existing.Status -eq 'Running') {
        Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
    sc.exe delete $serviceName | Out-Null
    Start-Sleep -Seconds 3
    $existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    while ($existing) {
        Start-Sleep -Seconds 2
        $existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    }
}

Stop-OrphanPortalJava -Dir $installDir

if (-not (Get-Service -Name $serviceName -ErrorAction SilentlyContinue)) {
    Write-Host "Installing WinSW service $serviceName ..."
    & $winswExe install
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        Write-Error ("WinSW install failed with exit " + $LASTEXITCODE)
        exit $LASTEXITCODE
    }
} else {
    Write-Host "WinSW service $serviceName already registered; refreshed $winswXml"
}

$svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Error ("Service $serviceName still missing after WinSW install.")
    exit 1
}

Write-Host "Installed service $serviceName -> $winswExe (wraps $start)"
Write-Host "Start: net start $serviceName"
Write-Host "Logs: $installDir\$serviceName.out.log / .err.log (WinSW roll)"
Write-Host "Jenkins copies exhibition-portal.jar into $installDir then net stop / net start."
