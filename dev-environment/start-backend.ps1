# 使用便携 JDK 和项目专属 Maven 仓库构建后端，并通过相对 JAR 路径启动。
param(
    [switch]$Background
)

. (Join-Path $PSScriptRoot 'scripts\common.ps1')

$manifest = Import-PowerShellDataFile -LiteralPath $ManifestPath
$jdkHome = Join-Path $RuntimeRoot $manifest.Jdk.TargetDirectory
$mavenHome = Join-Path $RuntimeRoot $manifest.Maven.TargetDirectory
$backendPort = [int](Get-ConfigValue 'BACKEND_PORT' '8080')
$mysqlPort = Get-ConfigValue 'MYSQL_PORT' '3307'
$redisPort = Get-ConfigValue 'REDIS_PORT' '6380'
$mysqlPassword = Get-ConfigValue 'DB_PASSWORD' (Get-ConfigValue 'MYSQL_ROOT_PASSWORD' 'root123456')
$redisHost = Get-ConfigValue 'REDIS_HOST' '127.0.0.1'
$jwtSecret = Get-ConfigValue 'JWT_SECRET' 'dev-only-change-this-secret-before-production-2026'

Ensure-File -Path (Join-Path $jdkHome 'bin\java.exe') `
    -Message 'JDK is missing. Run .\setup-environment.ps1 first.'
Ensure-File -Path (Join-Path $mavenHome 'bin\mvn.cmd') `
    -Message 'Maven is missing. Run .\setup-environment.ps1 first.'
Ensure-File -Path (Join-Path $BackendRoot 'pom.xml') `
    -Message "Backend project was not found at $BackendRoot."

if (Test-TcpPort -Port $backendPort) {
    Write-Host "[OK] Backend is already listening on port $backendPort"
    return
}

if (-not (Test-TcpPort -Port ([int]$mysqlPort))) {
    throw 'MySQL is not running. Run .\start-databases.ps1 first.'
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$(Join-Path $jdkHome 'bin');$(Join-Path $mavenHome 'bin');$env:Path"
$env:SERVER_PORT = "$backendPort"
$env:DB_URL = "jdbc:mysql://127.0.0.1:$mysqlPort/gaokao_volunteer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = $mysqlPassword
$env:JWT_SECRET = $jwtSecret
$env:REDIS_HOST = $redisHost
$env:REDIS_PORT = $redisPort

$maven = Join-Path $mavenHome 'bin\mvn.cmd'
$java = Join-Path $jdkHome 'bin\java.exe'
$jarRelativePath = 'target\volunteer-backend-0.1.0.jar'

Write-Section 'Building executable backend JAR'
Push-Location $BackendRoot
try {
    & $maven (Quote-ProcessArgument "-Dmaven.repo.local=$MavenRepository") -o -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw 'Backend packaging failed.'
    }
}
finally {
    Pop-Location
}
Ensure-File -Path (Join-Path $BackendRoot $jarRelativePath) `
    -Message 'Backend executable JAR was not produced.'

if ($Background) {
    Write-Section "Starting backend in background on port $backendPort"
    $process = Start-Process -FilePath $java `
        -ArgumentList @('-Dspring.profiles.active=redis', '-jar', $jarRelativePath) `
        -WorkingDirectory $BackendRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $LogRoot 'backend.out.log') `
        -RedirectStandardError (Join-Path $LogRoot 'backend.err.log')
    Set-Content -LiteralPath (Join-Path $LogRoot 'backend.pid') -Value $process.Id -Encoding Ascii
    Wait-HttpOk -Url "http://127.0.0.1:$backendPort/api/universities?page=1&size=1" -Name 'Backend'
}
else {
    Write-Section "Starting backend in the foreground on port $backendPort"
    Push-Location $BackendRoot
    try {
        & $java '-Dspring.profiles.active=redis' '-jar' $jarRelativePath
    }
    finally {
        Pop-Location
    }
}
