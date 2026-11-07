# 使用便携 Node.js 启动 Vue 开发服务器，并设置开发代理的后端地址。
param(
    [switch]$Background
)

. (Join-Path $PSScriptRoot 'scripts\common.ps1')

$manifest = Import-PowerShellDataFile -LiteralPath $ManifestPath
$nodeHome = Join-Path $RuntimeRoot $manifest.Node.TargetDirectory
$frontendPort = [int](Get-ConfigValue 'FRONTEND_PORT' '5173')
$backendPort = [int](Get-ConfigValue 'BACKEND_PORT' '8080')

Ensure-File -Path (Join-Path $nodeHome 'npm.cmd') `
    -Message 'Node.js is missing. Run .\setup-environment.ps1 first.'
Ensure-File -Path (Join-Path $FrontendRoot 'node_modules') `
    -Message 'Frontend dependencies are missing. Run .\setup-environment.ps1 first.'

if (Test-TcpPort -Port $frontendPort) {
    Write-Host "[OK] Frontend is already listening on port $frontendPort"
    return
}

$env:Path = "$nodeHome;$env:Path"
$env:VITE_BACKEND_URL = "http://127.0.0.1:$backendPort"

$arguments = @(
    'run',
    'dev',
    '--',
    '--host',
    '127.0.0.1',
    '--port',
    "$frontendPort"
)

if ($Background) {
    Write-Section "Starting frontend in background on port $frontendPort"
    $process = Start-Process -FilePath (Join-Path $nodeHome 'npm.cmd') `
        -ArgumentList $arguments -WorkingDirectory $FrontendRoot `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $LogRoot 'frontend.out.log') `
        -RedirectStandardError (Join-Path $LogRoot 'frontend.err.log')
    Set-Content -LiteralPath (Join-Path $LogRoot 'frontend.pid') -Value $process.Id -Encoding Ascii
    Wait-HttpOk -Url "http://127.0.0.1:$frontendPort/login" -Name 'Frontend'
}
else {
    Write-Section "Starting frontend in the foreground on port $frontendPort"
    Push-Location $FrontendRoot
    try {
        & (Join-Path $nodeHome 'npm.cmd') @arguments
    }
    finally {
        Pop-Location
    }
}
