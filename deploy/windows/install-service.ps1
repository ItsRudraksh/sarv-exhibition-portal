# Install exhibition-portal as a real Windows service via WinSW.
# Runs java.exe directly (not powershell → java). LocalSystem often has no interactive PATH;
# this script resolves java and embeds portal.env.ps1 into the WinSW XML.
# MUST run elevated (Jenkins LocalSystem is fine).
#   .\deploy\windows\install-service.ps1
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

$jar = Join-Path $installDir 'exhibition-portal.jar'
$envFile = Join-Path $installDir 'portal.env.ps1'
$start = Join-Path $installDir 'start-portal.ps1'

if (-not (Test-Path -LiteralPath $jar)) {
    Write-Error "Missing $jar. Deploy the JAR first."
    exit 1
}
if (-not (Test-Path -LiteralPath $envFile)) {
    Write-Error "Missing $envFile. Copy portal.env.example.ps1 and set passwords."
    exit 1
}

New-Item -ItemType Directory -Force -Path $installDir | Out-Null

function Escape-Xml([string] $Value) {
    if ($null -eq $Value) { return '' }
    return (($Value -replace '&', '&amp;') -replace '<', '&lt;' -replace '>', '&gt;' -replace '"', '&quot;')
}

function Get-PortalEnvMap([string] $Path) {
    $map = @{}
    $raw = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
    $pattern = '(?m)^\s*\$env:([A-Za-z0-9_]+)\s*=\s*''([^'']*)'''
    foreach ($m in [regex]::Matches($raw, $pattern)) {
        $map[$m.Groups[1].Value] = $m.Groups[2].Value
    }
    return $map
}

function Resolve-JavaExe([hashtable] $EnvMap) {
    $javaHome = $null
    if ($EnvMap.ContainsKey('JAVA_HOME') -and -not [string]::IsNullOrWhiteSpace($EnvMap['JAVA_HOME'])) {
        $javaHome = $EnvMap['JAVA_HOME']
    } elseif ($env:JAVA_HOME) {
        $javaHome = $env:JAVA_HOME
    }
    if ($javaHome) {
        $candidate = Join-Path $javaHome 'bin\java.exe'
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source -and (Test-Path -LiteralPath $cmd.Source)) {
        return $cmd.Source
    }
    $roots = @(
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Java',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Amazon Corretto',
        'C:\Program Files\Zulu',
        'C:\Program Files\BellSoft',
        'C:\Program Files\Temurin'
    )
    $found = New-Object System.Collections.Generic.List[string]
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        Get-ChildItem -LiteralPath $root -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.DirectoryName -and $_.DirectoryName.EndsWith('\bin') } |
            ForEach-Object { [void]$found.Add($_.FullName) }
    }
    if ($found.Count -eq 0) { return $null }
    $prefer17 = $found | Where-Object { $_ -match 'jdk-?17|java-?17|temurin-17|adoptium.*17' } | Select-Object -First 1
    if ($prefer17) { return $prefer17 }
    return $found[0]
}

function Stop-OrphanPortalJava([string] $Dir) {
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and $_.CommandLine -like "*$Dir*" -and $_.CommandLine -like '*exhibition-portal.jar*' } |
        ForEach-Object {
            Write-Host ("Stopping leftover portal java PID {0}" -f $_.ProcessId)
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

function Get-ListenPids([int] $Port) {
    $ids = New-Object System.Collections.Generic.List[int]
    try {
        Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
            Where-Object { $_.OwningProcess -gt 4 } |
            ForEach-Object { [void]$ids.Add([int]$_.OwningProcess) }
    } catch {
        netstat -ano | Select-String -Pattern (':' + $Port + '\s+') | ForEach-Object {
            if ($_.Line -match 'LISTENING' -and $_.Line -match '\s(\d+)\s*$') {
                $p = [int]$Matches[1]
                if ($p -gt 4) { [void]$ids.Add($p) }
            }
        }
    }
    return @($ids | Select-Object -Unique)
}

function Test-PortalJavaProcess([int] $ProcessId, [string] $Dir) {
    $proc = Get-CimInstance Win32_Process -Filter ("ProcessId=" + $ProcessId) -ErrorAction SilentlyContinue
    if (-not $proc -or -not $proc.CommandLine) { return $false }
    return ($proc.CommandLine -like "*$Dir*" -and $proc.CommandLine -like '*exhibition-portal.jar*')
}

function Wait-PortalPortFree([int] $Port, [string] $Dir, [int] $TimeoutSec = 40) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $listenPids = @(Get-ListenPids $Port)
        if ($listenPids.Count -eq 0) {
            Write-Host ("Listen port {0} is free" -f $Port)
            return
        }
        foreach ($owningPid in $listenPids) {
            if (Test-PortalJavaProcess $owningPid $Dir) {
                Write-Host ("Stopping leftover portal java PID {0} still listening on {1}" -f $owningPid, $Port)
                Stop-Process -Id $owningPid -Force -ErrorAction SilentlyContinue
            } else {
                $cl = (Get-CimInstance Win32_Process -Filter ("ProcessId=" + $owningPid) -ErrorAction SilentlyContinue).CommandLine
                Write-Host ("Port {0} still LISTEN PID {1} (not this portal): {2}" -f $Port, $owningPid, $cl)
            }
        }
        Start-Sleep -Seconds 2
    }
    $left = @(Get-ListenPids $Port)
    if ($left.Count -gt 0) {
        Write-Error ("Port {0} still in use after {1}s (PIDs {2}). Do not net start until it is free." -f $Port, $TimeoutSec, ($left -join ','))
        exit 1
    }
}

$envMap = Get-PortalEnvMap $envFile
if (-not $envMap.ContainsKey('DATASOURCE_PASSWORD') -or $envMap['DATASOURCE_PASSWORD'] -eq 'change-me-db') {
    Write-Error "Set DATASOURCE_PASSWORD in $envFile before installing the service."
    exit 1
}
if (-not $envMap.ContainsKey('EXHIBITION_STAFF_BOOTSTRAP_PASSWORD') -or $envMap['EXHIBITION_STAFF_BOOTSTRAP_PASSWORD'] -eq 'change-me-staff') {
    Write-Error "Set EXHIBITION_STAFF_BOOTSTRAP_PASSWORD in $envFile before installing the service."
    exit 1
}

$javaExe = Resolve-JavaExe $envMap
if (-not $javaExe) {
    Write-Error @"
java.exe not found for service install.
Set JAVA_HOME in $envFile to your JDK 17 folder (the parent of bin\java.exe), e.g.
  `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18-hotspot'
Then re-run install-service.ps1 -Staging
"@
    exit 1
}
Write-Host "WinSW will run: $javaExe -jar $jar"

# Ensure storage root exists (java-direct no longer goes through start-portal.ps1 mkdir)
$storageRoot = $null
if ($envMap.ContainsKey('EXHIBITION_STORAGE_ROOT')) { $storageRoot = $envMap['EXHIBITION_STORAGE_ROOT'] }
if ($storageRoot) {
    New-Item -ItemType Directory -Force -Path $storageRoot | Out-Null
    Write-Host "Storage root: $storageRoot"
}
$envKeys = @(
    'SPRING_PROFILES_ACTIVE',
    'SERVER_PORT',
    'DATASOURCE_URL',
    'DATASOURCE_USERNAME',
    'DATASOURCE_PASSWORD',
    'EXHIBITION_STAFF_BOOTSTRAP_PASSWORD',
    'EXHIBITION_STORAGE_ROOT',
    'JAVA_HOME',
    'EXHIBITION_OUTBOX_MARKETING',
    'EXHIBITION_OUTBOX_VENDOR',
    'EXHIBITION_PHARMA_ERP_ENABLED',
    'EXHIBITION_PHARMA_ERP_JDBC_URL',
    'EXHIBITION_PHARMA_ERP_USERNAME',
    'EXHIBITION_PHARMA_ERP_PASSWORD',
    'EXHIBITION_PHARMA_ERP_SCHEDULE'
)
$envXml = New-Object System.Text.StringBuilder
foreach ($key in $envKeys) {
    if ($envMap.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace($envMap[$key])) {
        [void]$envXml.AppendLine(('  <env name="{0}" value="{1}"/>' -f $key, (Escape-Xml $envMap[$key])))
    }
}
# Ensure JAVA_HOME points at the resolved JDK even if not in portal.env.ps1
$resolvedHome = Split-Path -Parent (Split-Path -Parent $javaExe)
if (-not ($envMap.ContainsKey('JAVA_HOME') -and $envMap['JAVA_HOME'])) {
    [void]$envXml.AppendLine(('  <env name="JAVA_HOME" value="{0}"/>' -f (Escape-Xml $resolvedHome)))
}

$winswUrl = 'https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe'
$winswExe = Join-Path $installDir ($serviceName + '.exe')
$winswXml = Join-Path $installDir ($serviceName + '.xml')

if (-not (Test-Path -LiteralPath $winswExe)) {
    Write-Host "Downloading WinSW to $winswExe ..."
    try {
        Invoke-WebRequest -Uri $winswUrl -OutFile $winswExe -UseBasicParsing
    } catch {
        Write-Error ("Failed to download WinSW from " + $winswUrl + ". Copy WinSW-x64.exe to " + $winswExe + ". " + $_.Exception.Message)
        exit 1
    }
}

$xml = @"
<service>
  <id>$serviceName</id>
  <name>$displayName</name>
  <description>Sarv Exhibition Portal (Spring Boot via WinSW + java.exe).</description>
$($envXml.ToString().TrimEnd())
  <executable>$(Escape-Xml $javaExe)</executable>
  <arguments>-jar "%BASE%\exhibition-portal.jar"</arguments>
  <workingdirectory>%BASE%</workingdirectory>
  <log mode="roll"></log>
  <onfailure action="restart" delay="10 sec"/>
  <stoptimeout>30 sec</stoptimeout>
</service>
"@
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($winswXml, $xml, $utf8NoBom)
Write-Host "Wrote $winswXml (java-direct, no powershell wrapper)"

$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
$pathName = $null
if ($existing) {
    $pathName = (Get-CimInstance Win32_Service -Filter ("Name='" + $serviceName + "'") -ErrorAction SilentlyContinue).PathName
}
$isWinsw = $pathName -and ($pathName -like ('*' + $serviceName + '.exe*'))
$isJavaDirect = (Test-Path -LiteralPath $winswXml) -and ((Get-Content -LiteralPath $winswXml -Raw) -match '<executable>[^<]*java\.exe</executable>')

# Stop WinSW first. Killing java while the service is still Running schedules onfailure
# restart (~10s) which rebinds SERVER_PORT before Jenkins can net start.
if ($existing -and $existing.Status -ne 'Stopped') {
    Write-Host "Stopping $serviceName before XML refresh / leftover java cleanup..."
    Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
    if ($isWinsw -and (Test-Path -LiteralPath $winswExe)) {
        & $winswExe stop 2>$null
    }
    Start-Sleep -Seconds 3
}

if ($existing -and (-not $isWinsw -or -not $isJavaDirect)) {
    Write-Host "Replacing service registration (need WinSW + java.exe direct)..."
    if ($isWinsw -and (Test-Path -LiteralPath $winswExe)) {
        & $winswExe uninstall 2>$null
        Start-Sleep -Seconds 2
    }
    sc.exe delete $serviceName 2>$null | Out-Null
    Start-Sleep -Seconds 3
    while (Get-Service -Name $serviceName -ErrorAction SilentlyContinue) {
        Start-Sleep -Seconds 2
    }
}

Stop-OrphanPortalJava $installDir
$listenPort = if ($Staging) { 8082 } else { 80 }
if ($envMap.ContainsKey('SERVER_PORT') -and $envMap['SERVER_PORT'] -match '^\d+$') {
    $listenPort = [int]$envMap['SERVER_PORT']
}
Wait-PortalPortFree $listenPort $installDir

if (-not (Get-Service -Name $serviceName -ErrorAction SilentlyContinue)) {
    Write-Host "Installing WinSW service $serviceName ..."
    & $winswExe install
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        Write-Error ("WinSW install failed with exit " + $LASTEXITCODE)
        exit $LASTEXITCODE
    }
} else {
    Write-Host "WinSW service $serviceName already registered; refreshed XML for next start"
}

$svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Error ("Service $serviceName still missing after WinSW install.")
    exit 1
}

Write-Host "Installed service $serviceName -> $winswExe"
Write-Host "Executable: $javaExe -jar exhibition-portal.jar"
if (Test-Path -LiteralPath $start) {
    Write-Host "Manual console start still: $start"
}
Write-Host "Start: net start $serviceName"
Write-Host "Logs: $installDir\$serviceName.out.log / .err.log / .wrapper.log"
