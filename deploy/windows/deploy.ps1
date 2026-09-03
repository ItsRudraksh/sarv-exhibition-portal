# Build the visitor UI into the Spring Boot JAR.
# Run from an elevated PowerShell if you will also start on port 80.
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\deploy\windows\deploy.ps1

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path (Join-Path $repoRoot 'frontend\package.json'))) {
    $repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
}

$frontend = Join-Path $repoRoot 'frontend'
$backend = Join-Path $repoRoot 'backend'
$installDir = 'C:\exhibition-portal'

Write-Host "Repo: $repoRoot"

Push-Location $frontend
try {
    if (Test-Path 'package-lock.json') {
        npm ci
    } else {
        npm install
    }
    npm run build
} finally {
    Pop-Location
}

Push-Location $backend
try {
    mvn -B -DskipTests package
} finally {
    Pop-Location
}

$jar = Get-ChildItem (Join-Path $backend 'target') -File |
    Where-Object { $_.Name -eq 'exhibition-portal.jar' } |
    Select-Object -First 1
if (-not $jar) {
    Write-Error 'JAR was not built. Check Maven output.'
}

New-Item -ItemType Directory -Force -Path $installDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $installDir 'files') | Out-Null
Copy-Item $jar.FullName (Join-Path $installDir 'exhibition-portal.jar') -Force

$startScript = Join-Path $PSScriptRoot 'start-portal.ps1'
Copy-Item $startScript (Join-Path $installDir 'start-portal.ps1') -Force

$envExample = Join-Path $PSScriptRoot 'portal.env.example.ps1'
$envTarget = Join-Path $installDir 'portal.env.ps1'
if (-not (Test-Path $envTarget)) {
    Copy-Item $envExample $envTarget
    Write-Host "Created $envTarget - edit DATASOURCE_PASSWORD and EXHIBITION_STAFF_BOOTSTRAP_PASSWORD before start."
}

Write-Host "Installed $($jar.Name) to $installDir\exhibition-portal.jar"
Write-Host 'Next:'
Write-Host '  1. Edit C:\exhibition-portal\portal.env.ps1 (MySQL 3306 + staff password)'
Write-Host '  2. Native MySQL: mysql -u root -p < deploy\windows\init-mysql.sql'
Write-Host '  3. Elevated: .\deploy\windows\open-http-80.ps1'
Write-Host '  4. Elevated: .\deploy\windows\install-service.ps1'
Write-Host '  5. net start exhibition-portal'
Write-Host '  6. Open http://43.225.195.200/ and http://43.225.195.200/staff'
Write-Host 'Jenkins on this host uses the same service name (see Jenkinsfile).'
