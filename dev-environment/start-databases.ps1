. (Join-Path $PSScriptRoot 'scripts\common.ps1')

$manifest = Import-EnvironmentManifest -Path $ManifestPath
$mysqlHome = Join-Path $RuntimeRoot $manifest.MySql.TargetDirectory
$redisHome = Join-Path $RuntimeRoot $manifest.Redis.TargetDirectory
$mysqlData = Join-Path $DataRoot 'mysql'
$redisData = Join-Path $DataRoot 'redis'
$mysqlPort = [int](Get-ConfigValue 'MYSQL_PORT' '3307')
$redisPort = [int](Get-ConfigValue 'REDIS_PORT' '6380')

Ensure-File -Path (Join-Path $mysqlHome 'bin\mysqld.exe') `
    -Message 'MySQL is not installed. Run .\setup-environment.ps1 first.'
Ensure-File -Path (Join-Path $redisHome 'redis-server.exe') `
    -Message 'Redis is not installed. Run .\setup-environment.ps1 first.'
Ensure-File -Path (Join-Path $mysqlData 'ibdata1') `
    -Message 'MySQL data directory is not initialized. Run .\setup-environment.ps1 first.'

New-Item -ItemType Directory -Path $LogRoot, $redisData -Force | Out-Null

if (-not (Test-TcpPort -Port $mysqlPort)) {
    Write-Section "Starting MySQL on port $mysqlPort"
    $mysqld = Join-Path $mysqlHome 'bin\mysqld.exe'
    $mysqlArguments = @(
        '--no-defaults',
        (Quote-ProcessArgument "--basedir=$mysqlHome"),
        (Quote-ProcessArgument "--datadir=$mysqlData"),
        "--port=$mysqlPort",
        '--bind-address=127.0.0.1',
        '--character-set-server=utf8mb4',
        '--collation-server=utf8mb4_unicode_ci',
        (Quote-ProcessArgument "--log-error=$(Join-Path $LogRoot 'mysql-error.log')"),
        (Quote-ProcessArgument "--pid-file=$(Join-Path $mysqlData 'mysqld.pid')")
    )
    $mysqlProcess = Start-Process -FilePath $mysqld -ArgumentList $mysqlArguments `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $LogRoot 'mysql.out.log') `
        -RedirectStandardError (Join-Path $LogRoot 'mysql.err.log')
    Set-Content -LiteralPath (Join-Path $mysqlData 'launcher.pid') -Value $mysqlProcess.Id -Encoding Ascii
    try {
        Wait-TcpPort -Port $mysqlPort -Name 'MySQL'
    }
    catch {
        $tail = Get-Content -LiteralPath (Join-Path $LogRoot 'mysql-error.log') -Tail 30 -ErrorAction SilentlyContinue
        if ($tail) {
            Write-Host $tail
        }
        throw
    }
}
else {
    Write-Host "[OK] MySQL is already listening on port $mysqlPort"
}

$redisConfig = Join-Path $redisData 'redis.conf'
$redisDir = $redisData.Replace('\', '/')
$redisLog = (Join-Path $LogRoot 'redis.log').Replace('\', '/')
$redisConfigContent = @"
bind 127.0.0.1
protected-mode yes
port $redisPort
timeout 0
tcp-keepalive 300
daemonize no
supervised no
dir "$redisDir"
dbfilename dump.rdb
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
logfile "$redisLog"
databases 8
"@
[System.IO.File]::WriteAllText(
    $redisConfig,
    $redisConfigContent,
    (New-Object System.Text.UTF8Encoding($false))
)

if (-not (Test-TcpPort -Port $redisPort)) {
    Write-Section "Starting Redis on port $redisPort"
    $redisProcess = Start-Process -FilePath (Join-Path $redisHome 'redis-server.exe') `
        -ArgumentList @((Quote-ProcessArgument $redisConfig)) -WorkingDirectory $redisHome `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $LogRoot 'redis.out.log') `
        -RedirectStandardError (Join-Path $LogRoot 'redis.err.log')
    Set-Content -LiteralPath (Join-Path $redisData 'launcher.pid') -Value $redisProcess.Id -Encoding Ascii
    Wait-TcpPort -Port $redisPort -Name 'Redis'
}
else {
    Write-Host "[OK] Redis is already listening on port $redisPort"
}

$redisPing = & (Join-Path $redisHome 'redis-cli.exe') -h 127.0.0.1 -p $redisPort ping
if ($redisPing -ne 'PONG') {
    throw "Redis health check failed: $redisPing"
}
Write-Host '[OK] Redis health check passed'
