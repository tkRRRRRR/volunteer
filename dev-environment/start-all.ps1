param(
    [switch]$NoBrowser,
    [switch]$SkipVerification
)

. (Join-Path $PSScriptRoot 'scripts\common.ps1')

$backendPort = [int](Get-ConfigValue 'BACKEND_PORT' '8080')
$frontendPort = [int](Get-ConfigValue 'FRONTEND_PORT' '5173')

Write-Section 'Preparing portable development environment'
& (Join-Path $PSScriptRoot 'setup-environment.ps1') -SkipTests

Write-Section 'Starting backend'
& (Join-Path $PSScriptRoot 'start-backend.ps1') -Background

Write-Section 'Starting frontend'
& (Join-Path $PSScriptRoot 'start-frontend.ps1') -Background

if (-not $SkipVerification) {
    & (Join-Path $PSScriptRoot 'verify.ps1')
}

$url = "http://127.0.0.1:$frontendPort"
Write-Host ''
Write-Host "Frontend: $url"
Write-Host "Backend:  http://127.0.0.1:$backendPort/swagger-ui.html"
Write-Host 'Demo student: student / student123'
Write-Host 'Demo admin:   admin / admin123'

if (-not $NoBrowser) {
    Start-Process $url
}
