# Start the packaged portal on this Windows Server.
# Port 80 requires an elevated PowerShell (or a Windows service running as LocalSystem).
#   Set-ExecutionPolicy -Scope Process Bypass
#   C:\exhibition-portal\start-portal.ps1
# Jenkins/WinSW runs this as LocalSystem — interactive PATH often does not apply; resolve java.exe explicitly.

$ErrorActionPreference = 'Stop'

$installDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $installDir) {
    $installDir = 'C:\exhibition-portal'
}

$bootLog = Join-Path $installDir 'start-portal-boot.log'
function Write-Boot {
    param([string] $Message)
    $line = '{0:yyyy-MM-dd HH:mm:ss} {1}' -f (Get-Date), $Message
    Add-Content -LiteralPath $bootLog -Value $line -ErrorAction SilentlyContinue
    Write-Host $line
}

function Resolve-JavaExe {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
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
    $found = [System.Collections.Generic.List[string]]::new()
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        Get-ChildItem -LiteralPath $root -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.DirectoryName -and ($_.DirectoryName.EndsWith('\bin')) } |
            ForEach-Object { [void]$found.Add($_.FullName) }
    }
    if ($found.Count -eq 0) { return $null }
    $prefer17 = $found | Where-Object { $_ -match '[\\/]jdk-?17|[\\/]java-?17|temurin-17|adoptium.*17' } | Select-Object -First 1
    if ($prefer17) { return $prefer17 }
    return $found[0]
}

Write-Boot ("start-portal begin installDir={0} user={1}" -f $installDir, $env:USERNAME)

$envFile = Join-Path $installDir 'portal.env.ps1'
if (-not (Test-Path -LiteralPath $envFile)) {
    Write-Boot "Missing $envFile"
    Write-Error "Missing $envFile. Copy deploy\windows\portal.env.example.ps1 there and set passwords."
}
. $envFile

$jar = Join-Path $installDir 'exhibition-portal.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    Write-Boot "Missing $jar"
    Write-Error "Missing $jar. Run deploy\windows\deploy.ps1 first."
}

if (-not $env:DATASOURCE_PASSWORD -or $env:DATASOURCE_PASSWORD -eq 'change-me-db') {
    Write-Boot 'DATASOURCE_PASSWORD still placeholder'
    Write-Error 'Set DATASOURCE_PASSWORD in portal.env.ps1 to a real database password before a public start.'
}
if (-not $env:EXHIBITION_STAFF_BOOTSTRAP_PASSWORD -or $env:EXHIBITION_STAFF_BOOTSTRAP_PASSWORD -eq 'change-me-staff') {
    Write-Boot 'EXHIBITION_STAFF_BOOTSTRAP_PASSWORD still placeholder'
    Write-Error 'Set EXHIBITION_STAFF_BOOTSTRAP_PASSWORD in portal.env.ps1. Do not ship poc-staff on a public IP.'
}

$javaExe = Resolve-JavaExe
if (-not $javaExe) {
    Write-Boot 'java.exe not found (PATH/JAVA_HOME empty for this account)'
    Write-Error 'java.exe not found for this account. Install Java 17, set JAVA_HOME in portal.env.ps1, or add java to the system PATH (LocalSystem does not see interactive user PATH).'
}
$javaBin = Split-Path -Parent $javaExe
if ($env:Path -notlike ('*' + $javaBin + '*')) {
    $env:Path = $javaBin + ';' + $env:Path
}
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = Split-Path -Parent $javaBin
}
Write-Boot ("Using java: {0}" -f $javaExe)

$iis = Get-Service -Name W3SVC -ErrorAction SilentlyContinue
if ($iis -and $iis.Status -eq 'Running' -and $env:SERVER_PORT -eq '80') {
    Write-Host 'IIS (W3SVC) is running and usually owns port 80. Stop it or change SERVER_PORT.'
    Write-Host '  Stop-Service W3SVC'
}

if ($env:EXHIBITION_STORAGE_ROOT) {
    New-Item -ItemType Directory -Force -Path $env:EXHIBITION_STORAGE_ROOT | Out-Null
}

Write-Boot ("Starting {0} on port {1} (profile {2})" -f $jar, $env:SERVER_PORT, $env:SPRING_PROFILES_ACTIVE)
Write-Host "Starting $jar on port $($env:SERVER_PORT) (profile $($env:SPRING_PROFILES_ACTIVE))"
& $javaExe -jar $jar
$exit = $LASTEXITCODE
Write-Boot ("java exited with code {0}" -f $exit)
exit $exit
