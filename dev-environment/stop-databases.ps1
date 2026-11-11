. (Join-Path $PSScriptRoot 'scripts\common.ps1')

$manifest = Import-EnvironmentManifest -Path $ManifestPath
$mysqlHome = Join-Path $RuntimeRoot $manifest.MySql.TargetDirectory
$redisHome = Join-Path $RuntimeRoot $manifest.Redis.TargetDirectory
$mysqlPort = [int](Get-ConfigValue 'MYSQL_PORT' '3307')
$redisPort = [int](Get-ConfigValue 'REDIS_PORT' '6380')
$mysqlPassword = Get-ConfigValue 'MYSQL_ROOT_PASSWORD' 'root123456'

$mysqlAdmin = Join-Path $mysqlHome 'bin\mysqladmin.exe'
if ((Test-Path -LiteralPath $mysqlAdmin) -and (Test-TcpPort -Port $mysqlPort)) {
    $oldMysqlPassword = $env:MYSQL_PWD
    $env:MYSQL_PWD = $mysqlPassword
    & $mysqlAdmin --protocol=tcp --host=127.0.0.1 "--port=$mysqlPort" --user=root shutdown 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host '[OK] MySQL stopped'
    }
    if ($null -eq $oldMysqlPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    else {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
}
Stop-ProcessFromPidFile -PidFile (Join-Path $DataRoot 'mysql\launcher.pid') -Name 'MySQL' `
    -AllowedNames @('mysqld')

$redisCli = Join-Path $redisHome 'redis-cli.exe'
if ((Test-Path -LiteralPath $redisCli) -and (Test-TcpPort -Port $redisPort)) {
    & $redisCli -h 127.0.0.1 -p $redisPort shutdown 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host '[OK] Redis stopped'
    }
}
Stop-ProcessFromPidFile -PidFile (Join-Path $DataRoot 'redis\launcher.pid') -Name 'Redis' `
    -AllowedNames @('redis-server')
