# Open inbound TCP 80 so visitors can reach http://43.225.195.200/
# MUST run in an elevated PowerShell (right-click Start -> Terminal (Admin)).
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\deploy\windows\open-http-80.ps1

$ErrorActionPreference = 'Stop'

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error @'
This window is not elevated. Firewall rules were not added.

1. Close this window.
2. Right-click Start -> Terminal (Admin) or Windows PowerShell (Admin).
3. Approve User Account Control.
4. Run:
   cd <repo>
   Set-ExecutionPolicy -Scope Process Bypass
   .\deploy\windows\open-http-80.ps1
'@
    exit 1
}

$ruleName = 'Sarv Exhibition Portal HTTP (TCP 80)'
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
    Remove-NetFirewallRule -DisplayName $ruleName
}
New-NetFirewallRule `
    -DisplayName $ruleName `
    -Direction Inbound `
    -Protocol TCP `
    -LocalPort 80 `
    -Action Allow `
    -Profile Private,Public,Domain `
    -Description 'Visitor portal and staff UI served by the Spring Boot JAR.' `
    -ErrorAction Stop | Out-Null

Write-Host 'Allowed inbound TCP 80 on Private, Public, and Domain profiles.'
Write-Host 'Also open TCP 80 on the cloud/provider firewall for 43.225.195.200.'
Write-Host 'Do not expose Vite 5173 or MySQL 3306 on the public IP.'
