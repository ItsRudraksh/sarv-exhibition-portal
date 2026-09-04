# Staging host checks for exhibition-portal. Does not print password values.
# Run on the Windows Server (elevated not required for most checks):
#   Set-ExecutionPolicy -Scope Process Bypass
#   powershell -NoProfile -ExecutionPolicy Bypass -File C:\exhibition-portal-staging\verify-staging.ps1
# If that file is missing, run from the git repo:
#   .\deploy\windows\verify-staging.ps1
# Paste the full console output into chat. Redact nothing unless you added extra Write-Host.

[CmdletBinding()]
param(
    [string] $InstallDir = 'C:\exhibition-portal-staging',
    [string] $ServiceName = 'exhibition-portal-staging',
    [string] $ExpectPort = '8082'
)

$ErrorActionPreference = 'Continue'
$fails = 0
$warns = 0

function Write-Check {
    param(
        [ValidateSet('OK', 'FAIL', 'WARN', 'INFO')]
        [string] $Status,
        [string] $Message
    )
    $line = "[{0}] {1}" -f $Status.PadRight(4), $Message
    switch ($Status) {
        'OK' { Write-Host $line -ForegroundColor Green }
        'FAIL' { Write-Host $line -ForegroundColor Red; $script:fails++ }
        'WARN' { Write-Host $line -ForegroundColor Yellow; $script:warns++ }
        default { Write-Host $line }
    }
}

function Get-EnvAssignment {
    param([string] $Text, [string] $Name)
    $pattern = '(?m)^\s*\$env:' + [regex]::Escape($Name) + "\s*=\s*'([^']*)'"
    $m = [regex]::Match($Text, $pattern)
    if ($m.Success) { return $m.Groups[1].Value }
    return $null
}

function Find-MysqlExe {
    $candidates = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe',
        'C:\Program Files\MariaDB 11.4\bin\mysql.exe',
        'C:\Program Files\MariaDB 10.11\bin\mysql.exe'
    )
    foreach ($p in $candidates) {
        if (Test-Path -LiteralPath $p) { return $p }
    }
    $cmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $found = Get-ChildItem 'C:\Program Files\MySQL', 'C:\Program Files (x86)\MySQL', 'C:\Program Files\MariaDB*' -Recurse -Filter mysql.exe -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    return $found
}

function Get-ListenPort([int] $Port) {
    try {
        $hits = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
        if ($hits.Count -gt 0) {
            $pids = $hits | Select-Object -ExpandProperty OwningProcess -Unique
            $names = foreach ($procId in $pids) {
                $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($p) { '{0}({1})' -f $p.ProcessName, $procId } else { "pid $procId" }
            }
            return ($names -join ', ')
        }
    } catch { }
    return $null
}

Write-Host '=== Exhibition portal staging verify ==='
Write-Host ("Time: {0:yyyy-MM-dd HH:mm:ss}  Host: {1}  User: {2}" -f (Get-Date), $env:COMPUTERNAME, $env:USERNAME)
Write-Host "InstallDir: $InstallDir"
Write-Host 'Passwords are never printed. Expected staging port is 8082 (8081 is pharma-erp-staging).'
Write-Host ''

if (Test-Path -LiteralPath $InstallDir) {
    Write-Check OK "Install dir exists"
} else {
    Write-Check FAIL "Install dir missing. Jenkins Deploy to Staging has not created it yet."
}

$jar = Join-Path $InstallDir 'exhibition-portal.jar'
if (Test-Path -LiteralPath $jar) {
    $info = Get-Item -LiteralPath $jar
    Write-Check OK ("JAR present  LastWrite={0:yyyy-MM-dd HH:mm:ss}  Size={1} bytes" -f $info.LastWriteTime, $info.Length)
} else {
    Write-Check FAIL "exhibition-portal.jar missing"
}

foreach ($name in @('start-portal.ps1', 'portal.env.ps1', 'init-mysql.sql', 'verify-staging.ps1')) {
    $p = Join-Path $InstallDir $name
    if (Test-Path -LiteralPath $p) {
        Write-Check OK $name
    } else {
        if ($name -eq 'verify-staging.ps1') {
            Write-Check WARN "$name not in install dir yet (run from repo, or wait for next Jenkins copy)"
        } elseif ($name -eq 'init-mysql.sql') {
            Write-Check WARN "$name not in install dir (use repo deploy\\windows\\init-mysql.sql)"
        } else {
            Write-Check FAIL "$name missing"
        }
    }
}

$envFile = Join-Path $InstallDir 'portal.env.ps1'
$portValue = $null
$dbUser = $null
$url = $null
$dbPass = $null
$staffPass = $null
if (Test-Path -LiteralPath $envFile) {
    $raw = Get-Content -LiteralPath $envFile -Raw
    $portValue = Get-EnvAssignment $raw 'SERVER_PORT'
    $dbUser = Get-EnvAssignment $raw 'DATASOURCE_USERNAME'
    $url = Get-EnvAssignment $raw 'DATASOURCE_URL'
    $dbPass = Get-EnvAssignment $raw 'DATASOURCE_PASSWORD'
    $staffPass = Get-EnvAssignment $raw 'EXHIBITION_STAFF_BOOTSTRAP_PASSWORD'

    if ($portValue -eq $ExpectPort) {
        Write-Check OK "SERVER_PORT=$portValue"
    } elseif ($portValue -eq '8081') {
        Write-Check FAIL "SERVER_PORT=8081 collides with pharma-erp-staging. Set SERVER_PORT='$ExpectPort'."
    } elseif ($portValue -eq '80') {
        Write-Check FAIL "SERVER_PORT=80 is production. Staging must be $ExpectPort."
    } elseif ([string]::IsNullOrEmpty($portValue)) {
        Write-Check FAIL 'SERVER_PORT not found in portal.env.ps1'
    } else {
        Write-Check WARN "SERVER_PORT=$portValue (expected $ExpectPort)"
    }

    if ($url -and $url -match 'exhibition_portal' -and $url -match '3306') {
        Write-Check OK 'DATASOURCE_URL points at 127.0.0.1:3306 / exhibition_portal'
    } else {
        Write-Check FAIL "DATASOURCE_URL unexpected (need jdbc mysql 3306 exhibition_portal). Length=$(if ($url) { $url.Length } else { 0 })"
    }

    if ([string]::IsNullOrWhiteSpace($dbUser)) {
        Write-Check FAIL 'DATASOURCE_USERNAME missing'
    } else {
        Write-Check OK "DATASOURCE_USERNAME=$dbUser"
    }

    if ([string]::IsNullOrWhiteSpace($dbPass) -or $dbPass -eq 'change-me-db') {
        Write-Check FAIL 'DATASOURCE_PASSWORD is empty or still change-me-db. Jenkins will refuse net start. Edit portal.env.ps1 (do not paste the password here).'
    } else {
        Write-Check OK ("DATASOURCE_PASSWORD is set (length {0}, not printed)" -f $dbPass.Length)
    }

    if ([string]::IsNullOrWhiteSpace($staffPass) -or $staffPass -eq 'change-me-staff') {
        Write-Check FAIL 'EXHIBITION_STAFF_BOOTSTRAP_PASSWORD is empty or still change-me-staff. Edit portal.env.ps1.'
    } else {
        Write-Check OK ("EXHIBITION_STAFF_BOOTSTRAP_PASSWORD is set (length {0}, not printed)" -f $staffPass.Length)
    }
}

$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($svc) {
    Write-Check OK ("Windows service {0} Status={1} StartType={2}" -f $svc.Name, $svc.Status, $svc.StartType)
    $pathName = (Get-CimInstance Win32_Service -Filter ("Name='" + $ServiceName + "'") -ErrorAction SilentlyContinue).PathName
    if ($pathName -and ($pathName -like ('*' + $ServiceName + '.exe*'))) {
        Write-Check OK 'Service PathName uses WinSW (net start should work)'
    } elseif ($pathName -and ($pathName -like '*powershell*start-portal.ps1*')) {
        Write-Check FAIL 'Service PathName is powershell-only (causes NET 2186). Rebuild after WinSW install-service.ps1, or run: .\\deploy\\windows\\install-service.ps1 -Staging'
    } elseif ($pathName) {
        Write-Check WARN ("Service PathName unexpected: $pathName")
    }
    $winswExe = Join-Path $InstallDir ($ServiceName + '.exe')
    if (Test-Path -LiteralPath $winswExe) {
        Write-Check OK "WinSW exe present: $winswExe"
    } else {
        Write-Check WARN "WinSW exe missing under install dir (next Jenkins Deploy should download it)"
    }
} else {
    Write-Check FAIL "Windows service $ServiceName is not installed. From the git repo: .\\deploy\\windows\\install-service.ps1 -Staging"
}

$prodSvc = Get-Service -Name 'exhibition-portal' -ErrorAction SilentlyContinue
if ($prodSvc) {
    Write-Check INFO ("Production service exhibition-portal Status={0} (should not be started by the poc job)" -f $prodSvc.Status)
}

$pharma = Get-ListenPort 8081
if ($pharma) {
    Write-Check INFO "Port 8081 LISTEN $pharma (pharma-erp-staging — leave it)"
} else {
    Write-Check INFO 'Port 8081 is free (pharma-erp-staging not listening right now)'
}

$ours = Get-ListenPort 8082
if ($ours) {
    Write-Check OK "Port 8082 LISTEN $ours"
} else {
    if ($svc -and $svc.Status -eq 'Running') {
        Write-Check FAIL 'Service is Running but nothing is listening on 8082. Check Windows Event Log / start-portal.ps1.'
    } else {
        Write-Check INFO 'Port 8082 is free (app not started yet — expected until passwords + rebuild)'
    }
}

$port80 = Get-ListenPort 80
if ($port80) {
    Write-Check INFO "Port 80 LISTEN $port80 (IIS or production — staging must not use this)"
}

$mysqlExe = Find-MysqlExe
if ($mysqlExe) {
    Write-Check OK "mysql.exe $mysqlExe"
} else {
    Write-Check FAIL "mysql.exe not found. Typical path: C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe (not on PATH)."
}

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $jv = & java -version 2>&1 | Out-String
    $jvOne = ($jv -split "`r?`n" | Where-Object { $_ } | Select-Object -First 1)
    if ($jvOne -match '17\.') {
        Write-Check OK "Java $jvOne"
    } else {
        Write-Check WARN "Java $jvOne (target is 17)"
    }
} else {
    Write-Check FAIL 'java not on PATH for this user (service LocalSystem may still have it)'
}

if ($mysqlExe -and $dbPass -and $dbPass -ne 'change-me-db' -and $dbUser) {
    $prevPwd = $env:MYSQL_PWD
    $env:MYSQL_PWD = $dbPass
    try {
        $probe = & $mysqlExe -h 127.0.0.1 -P 3306 -u $dbUser -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='exhibition_portal';" 2>&1
        $code = $LASTEXITCODE
        $probeText = ($probe | Out-String).Trim()
        if ($code -eq 0 -and $probeText -match 'exhibition_portal') {
            Write-Check OK 'MySQL login as exhibition succeeded; database exhibition_portal exists'
        } elseif ($code -eq 0) {
            Write-Check FAIL 'MySQL login worked but database exhibition_portal is missing. Run init-mysql.sql as root.'
        } else {
            $safe = $probeText
            if ($dbPass) { $safe = $safe.Replace($dbPass, '***') }
            Write-Check FAIL ("MySQL login as exhibition failed (exit $code). Create user/db with init-mysql.sql; password in portal.env.ps1 must match. Client said: $safe")
        }
    } finally {
        if ($null -eq $prevPwd) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue } else { $env:MYSQL_PWD = $prevPwd }
    }
} elseif ($mysqlExe) {
    Write-Check INFO 'Skipped MySQL login (password still placeholder or unset)'
}

$healthUrl = "http://127.0.0.1:$ExpectPort/actuator/health"
try {
    $r = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
    Write-Check OK ("Health $healthUrl HTTP $($r.StatusCode) $($r.Content)")
} catch {
    Write-Check INFO ("Health $healthUrl not up: $($_.Exception.Message)")
}

Write-Host ''
if ($fails -gt 0) {
    Write-Host "RESULT: FAIL  ($fails failing check(s), $warns warning(s))" -ForegroundColor Red
    Write-Host 'Next: set DATASOURCE_PASSWORD and EXHIBITION_STAFF_BOOTSTRAP_PASSWORD in portal.env.ps1, create MySQL DB, then rebuild Jenkins. Do not paste secrets into chat.'
    exit 1
}
Write-Host "RESULT: PASS  ($warns warning(s))" -ForegroundColor Green
exit 0
