# Run Spring Boot from backend/ (local dev). Same idea as pharma-erp/run.ps1.
# Usage:
#   .\run.ps1
#   .\run.ps1 -Profile prod
#
# Needs native MySQL 8 on localhost:3306 (database exhibition_portal, user exhibition).
# mvn test uses embedded MariaDB (mariaDB4j). Docker is not required.
#
# For a deployable JAR (Jenkins / Windows service), run from frontend then backend:
#   npm ci; npm run build
#   mvn -DskipTests package
# See specs/DEPLOY-WINDOWS.md.

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateNotNullOrEmpty()]
    [string] $Profile = "default"
)

$ErrorActionPreference = "Stop"

if (-not $PSScriptRoot) {
    $PSScriptRoot = Split-Path -Parent -LiteralPath $MyInvocation.MyCommand.Path
}

$previousSpringProfiles = $env:SPRING_PROFILES_ACTIVE
Push-Location $PSScriptRoot
try {
    if ($Profile -ne "default") {
        $env:SPRING_PROFILES_ACTIVE = $Profile
    }

    Write-Host ""
    Write-Host "Exhibition portal - starting. API: http://localhost:8080  (prod profile binds port 80)"
    Write-Host "Keep this window open. Ctrl+C stops the server."
    Write-Host ""

    if ($Profile -eq "default") {
        & mvn spring-boot:run "-Dmaven.test.skip=true"
    } else {
        & mvn spring-boot:run `
            "-Dmaven.test.skip=true" `
            "-Dspring-boot.run.profiles=$Profile"
    }
}
finally {
    Pop-Location
    if ($null -eq $previousSpringProfiles -or $previousSpringProfiles -eq "") {
        Remove-Item Env:\SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
    } else {
        $env:SPRING_PROFILES_ACTIVE = $previousSpringProfiles
    }
}
