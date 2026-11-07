. (Join-Path $PSScriptRoot 'scripts\common.ps1')

Write-Section 'Stopping frontend'
Stop-ProcessFromPidFile -PidFile (Join-Path $LogRoot 'frontend.pid') -Name 'Frontend' `
    -AllowedNames @('cmd', 'node')

Write-Section 'Stopping backend'
Stop-ProcessFromPidFile -PidFile (Join-Path $LogRoot 'backend.pid') -Name 'Backend' `
    -AllowedNames @('java')

Write-Section 'Stopping databases'
& (Join-Path $PSScriptRoot 'stop-databases.ps1')
