param(
    [switch]$ForceDownload,
    [switch]$SkipFrontendInstall,
    [switch]$SkipTests
)

. (Join-Path $PSScriptRoot 'scripts\common.ps1')

Write-Section 'Preparing portable directories'
$directories = @(
    $RuntimeRoot,
    $DownloadRoot,
    $MavenRepository,
    (Join-Path $DataRoot 'mysql'),
    (Join-Path $DataRoot 'redis'),
    $LogRoot
)
foreach ($directory in $directories) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

if (-not (Test-Path -LiteralPath $ConfigPath)) {
    Copy-Item -LiteralPath (Join-Path $EnvironmentRoot '.env.example') -Destination $ConfigPath
    Write-Host "[OK] Created local configuration: $ConfigPath"
}

$manifest = Import-EnvironmentManifest -Path $ManifestPath

function Get-RuntimeSpec {
    param([string]$Key)
    return $manifest[$Key]
}

function Install-RuntimeFromArchive {
# 首次安装脚本：检查固定版本运行时、初始化数据库并构建项目依赖。
# 所有文件均位于当前项目目录内，不修改系统级环境变量。
param(
        [string]$Key,
        [hashtable]$Spec
    )

    $target = Join-Path $RuntimeRoot $Spec.TargetDirectory
    $required = Join-Path $target $Spec.RequiredFile
    if (Test-Path -LiteralPath $required) {
        Write-Host "[OK] $($Spec.Name) $($Spec.Version) is already installed"
        return
    }

    $archive = Join-Path $DownloadRoot $Spec.ArchiveName
    if ((Test-Path -LiteralPath $archive) -and $ForceDownload) {
        Remove-Item -LiteralPath $archive -Force
    }
    if (-not (Test-Path -LiteralPath $archive)) {
        Write-Host "Downloading $($Spec.Name) $($Spec.Version)..."
        Invoke-WebRequest -Uri $Spec.Url -OutFile $archive -UseBasicParsing
    }

    $actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $Spec.Sha256) {
        throw "Checksum mismatch for $($Spec.ArchiveName). Expected $($Spec.Sha256), got $actualHash."
    }

    $temporary = Join-Path $RuntimeRoot ('.extract-' + $Key + '-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $temporary -Force | Out-Null
    try {
        Expand-Archive -LiteralPath $archive -DestinationPath $temporary -Force
        if ($Spec.ArchiveRootGlob) {
            $extractedRoot = Get-ChildItem -LiteralPath $temporary -Directory |
                Where-Object { $_.Name -like $Spec.ArchiveRootGlob } |
                Select-Object -First 1
            if (-not $extractedRoot) {
                throw "Could not find expected directory $($Spec.ArchiveRootGlob) in $($Spec.ArchiveName)."
            }
            Move-Item -LiteralPath $extractedRoot.FullName -Destination $target
        }
        else {
            New-Item -ItemType Directory -Path $target -Force | Out-Null
            Copy-Item -Path (Join-Path $temporary '*') -Destination $target -Recurse -Force
        }
    }
    finally {
        if (Test-Path -LiteralPath $temporary) {
            Remove-Item -LiteralPath $temporary -Recurse -Force
        }
    }
    Ensure-File -Path (Join-Path $target $Spec.RequiredFile) `
        -Message "$($Spec.Name) installation did not produce $($Spec.RequiredFile)."
    Write-Host "[OK] Installed $($Spec.Name) $($Spec.Version)"
}

Write-Section 'Checking bundled runtimes'
foreach ($key in @('Jdk', 'Maven', 'Node', 'MySql', 'Redis')) {
    Install-RuntimeFromArchive -Key $key -Spec (Get-RuntimeSpec $key)
}

$jdkHome = Join-Path $RuntimeRoot $manifest.Jdk.TargetDirectory
$mavenHome = Join-Path $RuntimeRoot $manifest.Maven.TargetDirectory
$nodeHome = Join-Path $RuntimeRoot $manifest.Node.TargetDirectory
$mysqlHome = Join-Path $RuntimeRoot $manifest.MySql.TargetDirectory
$redisHome = Join-Path $RuntimeRoot $manifest.Redis.TargetDirectory
$mysqlData = Join-Path $DataRoot 'mysql'
$mysqld = Join-Path $mysqlHome 'bin\mysqld.exe'

if (-not (Test-Path -LiteralPath (Join-Path $mysqlData 'ibdata1'))) {
    Write-Section 'Initializing portable MySQL data directory'
    $mysqlInitLog = Join-Path $LogRoot 'mysql-init.log'
    & $mysqld --no-defaults --initialize-insecure "--basedir=$mysqlHome" "--datadir=$mysqlData" 2>&1 |
        Tee-Object -FilePath $mysqlInitLog
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL initialization failed. See $mysqlInitLog."
    }
}

Write-Section 'Starting project databases'
& (Join-Path $PSScriptRoot 'start-databases.ps1')

Write-Section 'Initializing application database'
$mysql = Join-Path $mysqlHome 'bin\mysql.exe'
$mysqlPort = Get-ConfigValue 'MYSQL_PORT' '3307'
$mysqlPassword = Get-ConfigValue 'MYSQL_ROOT_PASSWORD' 'root123456'
try {
    $oldMysqlPassword = $env:MYSQL_PWD
    $env:MYSQL_PWD = ''
    $blankLoginWorks = $false
    $blankCheckErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $mysql --protocol=tcp "--host=127.0.0.1" "--port=$mysqlPort" '--user=root' `
            '--connect-timeout=3' '-N' '-e' 'SELECT 1;' 2>$null | Out-Null
        $blankLoginWorks = ($LASTEXITCODE -eq 0)
    }
    finally {
        $ErrorActionPreference = $blankCheckErrorAction
    }

    if ($blankLoginWorks) {
        & $mysql --protocol=tcp "--host=127.0.0.1" "--port=$mysqlPort" '--user=root' `
            '-e' "ALTER USER 'root'@'localhost' IDENTIFIED BY '$mysqlPassword';"
        if ($LASTEXITCODE -ne 0) {
            throw 'Failed to set the portable MySQL root password.'
        }
    }

    $env:MYSQL_PWD = $mysqlPassword
    $databaseExists = & $mysql --protocol=tcp "--host=127.0.0.1" "--port=$mysqlPort" '--user=root' `
        '--connect-timeout=3' '-N' '-e' "SHOW DATABASES LIKE 'gaokao_volunteer';"
    if ($LASTEXITCODE -ne 0) {
        throw 'Cannot connect to MySQL. Check logs/mysql-error.log and .env.'
    }
    if (-not $databaseExists) {
        Push-Location (Join-Path $DatabaseRoot 'schema')
        try {
            & $mysql --protocol=tcp "--host=127.0.0.1" "--port=$mysqlPort" '--user=root' `
                '--default-character-set=utf8mb4' '-e' 'source 01_schema.sql;'
            if ($LASTEXITCODE -ne 0) {
                throw 'Failed to import the database schema.'
            }
        }
        finally {
            Pop-Location
        }

        Push-Location (Join-Path $DatabaseRoot 'seed')
        try {
            & $mysql --protocol=tcp "--host=127.0.0.1" "--port=$mysqlPort" '--user=root' `
                '--default-character-set=utf8mb4' '-e' 'source 02_seed.sql;'
            if ($LASTEXITCODE -ne 0) {
                throw 'Failed to import the seed data.'
            }
        }
        finally {
            Pop-Location
        }
        Write-Host '[OK] Application database and demo data initialized'
    }
    else {
        Write-Host '[OK] Application database already exists; existing data was preserved'
    }
}
finally {
    if ($null -eq $oldMysqlPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    else {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
}

if (-not $SkipFrontendInstall) {
    $nodeModules = Join-Path $FrontendRoot 'node_modules'
    if (-not (Test-Path -LiteralPath $nodeModules)) {
        Write-Section 'Installing frontend dependencies into the project'
        $env:Path = "$nodeHome;$env:Path"
        Push-Location $FrontendRoot
        try {
            & (Join-Path $nodeHome 'npm.cmd') ci --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) {
                throw 'npm ci failed.'
            }
        }
        finally {
            Pop-Location
        }
    }
}

if (-not $SkipTests) {
    Write-Section 'Compiling and testing backend with portable Maven repository'
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$(Join-Path $jdkHome 'bin');$(Join-Path $mavenHome 'bin');$env:Path"
    Push-Location $BackendRoot
    try {
        & (Join-Path $mavenHome 'bin\mvn.cmd') "-Dmaven.repo.local=$MavenRepository" -q test
        if ($LASTEXITCODE -ne 0) {
            throw 'Backend compilation or tests failed.'
        }
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Section 'Packaging backend with portable Maven repository'
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$(Join-Path $jdkHome 'bin');$(Join-Path $mavenHome 'bin');$env:Path"
    Push-Location $BackendRoot
    try {
        & (Join-Path $mavenHome 'bin\mvn.cmd') "-Dmaven.repo.local=$MavenRepository" -q -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw 'Backend packaging failed.'
        }
    }
    finally {
        Pop-Location
    }
}

Write-Section 'Environment setup completed'
Write-Host "Environment directory: $EnvironmentRoot"
Write-Host 'Run .\start-all.ps1 to launch MySQL, Redis, backend and frontend.'
Write-Host 'Run .\verify.ps1 to check all runtime components.'
