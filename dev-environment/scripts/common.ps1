$ErrorActionPreference = 'Stop'

$script:EnvironmentRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$script:ProjectRoot = [System.IO.Path]::GetFullPath((Join-Path $script:EnvironmentRoot '..'))
$script:RuntimeRoot = Join-Path $script:EnvironmentRoot 'runtime'
$script:DataRoot = Join-Path $script:EnvironmentRoot 'data'
$script:LogRoot = Join-Path $script:EnvironmentRoot 'logs'
$script:DownloadRoot = Join-Path $script:EnvironmentRoot 'downloads'
$script:CacheRoot = Join-Path $script:EnvironmentRoot 'cache'
$script:MavenRepository = Join-Path $script:CacheRoot 'maven-repository'
$script:ManifestPath = Join-Path $script:EnvironmentRoot 'environment.psd1'
$script:ConfigPath = Join-Path $script:EnvironmentRoot '.env'
$script:BackendRoot = Join-Path $script:ProjectRoot '02_项目代码\volunteer-backend'
$script:FrontendRoot = Join-Path $script:ProjectRoot '02_项目代码\volunteer-frontend'
$script:DatabaseRoot = Join-Path $script:ProjectRoot '02_项目代码\database'

function Read-EnvironmentConfig {
    param([string]$Path)

    $config = @{
        MYSQL_PORT = '3307'
        MYSQL_ROOT_PASSWORD = 'root123456'
        REDIS_PORT = '6380'
        BACKEND_PORT = '8080'
        FRONTEND_PORT = '5173'
        JWT_SECRET = 'dev-only-change-this-secret-before-production-2026'
    }

    if (Test-Path -LiteralPath $Path) {
        foreach ($line in Get-Content -LiteralPath $Path) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) {
                continue
            }
            $separator = $trimmed.IndexOf('=')
            if ($separator -lt 1) {
                continue
            }
            $key = $trimmed.Substring(0, $separator).Trim()
            $value = $trimmed.Substring($separator + 1).Trim().Trim('"')
            $config[$key] = $value
        }
    }
    return $config
}

$script:Config = Read-EnvironmentConfig -Path $script:ConfigPath

function Get-ConfigValue {
    param(
        [string]$Name,
        [string]$Default
    )
    if ($script:Config.ContainsKey($Name) -and $script:Config[$Name]) {
        return $script:Config[$Name]
    }
    return $Default
}

function Get-RuntimePath {
    param(
        [string]$Runtime,
        [string]$ChildPath
    )
    return Join-Path (Join-Path $script:RuntimeRoot $Runtime) $ChildPath
}

function Ensure-File {
    param(
        [string]$Path,
        [string]$Message
    )
    if (-not (Test-Path -LiteralPath $Path)) {
        throw $Message
    }
}

function Test-TcpPort {
    param(
        [int]$Port,
        [string]$HostName = '127.0.0.1',
        [int]$TimeoutMilliseconds = 600
    )
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync($HostName, $Port)
        if (-not $task.Wait($TimeoutMilliseconds)) {
            return $false
        }
        return $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Wait-TcpPort {
    param(
        [int]$Port,
        [string]$Name,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -Port $Port) {
            Write-Host "[OK] $Name is listening on port $Port"
            return
        }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not start on port $Port within $TimeoutSeconds seconds."
}

function Assert-TcpPort {
    param(
        [int]$Port,
        [string]$Name,
        [string]$StartHint
    )
    if (-not (Test-TcpPort -Port $Port)) {
        throw "$Name is not running on port $Port. Start it with $StartHint."
    }
    Write-Host "[OK] $Name is listening on port $Port"
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [string]$Name,
        [int]$TimeoutSeconds = 90
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Write-Host "[OK] $Name responded at $Url"
                return
            }
        }
        catch {
        }
        Start-Sleep -Milliseconds 800
    }
    throw "$Name did not respond at $Url within $TimeoutSeconds seconds."
}

function Stop-ProcessFromPidFile {
    param(
        [string]$PidFile,
        [string]$Name,
        [string[]]$AllowedNames
    )
    if (-not (Test-Path -LiteralPath $PidFile)) {
        return
    }
    $processId = [int](Get-Content -LiteralPath $PidFile -Raw).Trim()
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        if ($AllowedNames -and $AllowedNames -notcontains $process.ProcessName) {
            Write-Warning "PID $processId is now $($process.ProcessName), not $Name. The stale PID file was removed."
            Remove-Item -LiteralPath $PidFile -Force
            return
        }
        $oldErrorAction = $ErrorActionPreference
        $ErrorActionPreference = 'SilentlyContinue'
        try {
            & taskkill.exe /PID $processId /T /F 2>$null | Out-Null
            $taskKillExitCode = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $oldErrorAction
        }
        if ($taskKillExitCode -eq 0) {
            Write-Host "[OK] Stopped $Name (PID $processId)"
        }
    }
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}

function Write-Section {
    param([string]$Text)
    Write-Host ''
    Write-Host "== $Text =="
}

function Quote-ProcessArgument {
    param([string]$Value)
    if ($Value -match '\s') {
        return '"' + $Value + '"'
    }
    return $Value
}
