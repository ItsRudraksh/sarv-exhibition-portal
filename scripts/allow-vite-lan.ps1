# Allow phones on the same Wi-Fi to reach the Vite HTTPS dev server.
# This MUST run in an elevated PowerShell (right-click Start -> Terminal (Admin)
# or "Windows PowerShell (Admin)").
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\scripts\allow-vite-lan.ps1

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
   cd C:\Users\asus\Documents\GitHub\sarv-exhibition-portal
   Set-ExecutionPolicy -Scope Process Bypass
   .\scripts\allow-vite-lan.ps1
'@
    exit 1
}

$ruleName = 'Sarv Exhibition Portal Vite LAN (TCP 5173)'
$previewName = 'Sarv Exhibition Portal Vite preview LAN (TCP 4173)'

foreach ($pair in @(
    @{ Name = $ruleName; Port = 5173 },
    @{ Name = $previewName; Port = 4173 }
)) {
    $existing = Get-NetFirewallRule -DisplayName $pair.Name -ErrorAction SilentlyContinue
    if ($existing) {
        Remove-NetFirewallRule -DisplayName $pair.Name
    }
    New-NetFirewallRule `
        -DisplayName $pair.Name `
        -Direction Inbound `
        -Protocol TCP `
        -LocalPort $pair.Port `
        -Action Allow `
        -Profile Private,Public `
        -Description 'Dev-only: visitor Vite server for same-Wi-Fi phone testing.' `
        -ErrorAction Stop | Out-Null
    Write-Host "Allowed inbound TCP $($pair.Port) on Private and Public profiles ($($pair.Name))."
}

$lanIps = @(Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object {
        $_.IPAddress -notlike '127.*' -and (
            $_.IPAddress -like '192.168.*' -or
            $_.IPAddress -like '10.*' -or
            $_.IPAddress -match '^172\.(1[6-9]|2[0-9]|3[0-1])\.'
        )
    } |
    Select-Object -ExpandProperty IPAddress -Unique)
if ($lanIps.Count -eq 0) {
    Write-Host 'No private LAN IPv4 found. Connect Wi-Fi and re-run.'
} else {
    foreach ($lanIp in $lanIps) {
        Write-Host "Open https://${lanIp}:5173/?c=POC-STALL-1 on the phone."
    }
}
Write-Host 'Accept the self-signed certificate warning (Android: Advanced -> Proceed). HTTPS is required for camera.'
Write-Host 'The phone uses Vite on 5173; it does not need port 8080. Keep npm run dev and backend\\run.ps1 running.'
