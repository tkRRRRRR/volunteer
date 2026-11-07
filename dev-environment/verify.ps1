. (Join-Path $PSScriptRoot 'scripts\common.ps1')

$manifest = Import-PowerShellDataFile -LiteralPath $ManifestPath
$jdkHome = Join-Path $RuntimeRoot $manifest.Jdk.TargetDirectory
$mavenHome = Join-Path $RuntimeRoot $manifest.Maven.TargetDirectory
$nodeHome = Join-Path $RuntimeRoot $manifest.Node.TargetDirectory
$mysqlHome = Join-Path $RuntimeRoot $manifest.MySql.TargetDirectory
$redisHome = Join-Path $RuntimeRoot $manifest.Redis.TargetDirectory
$mysqlPort = [int](Get-ConfigValue 'MYSQL_PORT' '3307')
$redisPort = [int](Get-ConfigValue 'REDIS_PORT' '6380')
$backendPort = [int](Get-ConfigValue 'BACKEND_PORT' '8080')
$frontendPort = [int](Get-ConfigValue 'FRONTEND_PORT' '5173')
$mysqlPassword = Get-ConfigValue 'MYSQL_ROOT_PASSWORD' 'root123456'

Write-Section 'Runtime versions'
$env:JAVA_HOME = $jdkHome
$env:Path = "$(Join-Path $jdkHome 'bin');$(Join-Path $mavenHome 'bin');$nodeHome;$env:Path"
$oldErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $javaVersion = (& (Join-Path $jdkHome 'bin\java.exe') -version 2>&1 | Select-Object -First 1)
    $mavenVersion = (& (Join-Path $mavenHome 'bin\mvn.cmd') -version 2>&1 | Select-Object -First 1)
    $nodeVersion = & (Join-Path $nodeHome 'node.exe') --version
    $npmVersion = & (Join-Path $nodeHome 'npm.cmd') --version
    $mysqlVersion = (& (Join-Path $mysqlHome 'bin\mysql.exe') --version)
    $redisVersion = (& (Join-Path $redisHome 'redis-server.exe') --version)
}
finally {
    $ErrorActionPreference = $oldErrorActionPreference
}
Write-Host "[OK] $javaVersion"
Write-Host "[OK] $mavenVersion"
Write-Host "[OK] Node.js $nodeVersion / npm $npmVersion"
Write-Host "[OK] $mysqlVersion"
Write-Host "[OK] $redisVersion"

Write-Section 'Ports'
Wait-TcpPort -Port $mysqlPort -Name 'MySQL' -TimeoutSeconds 5
Wait-TcpPort -Port $redisPort -Name 'Redis' -TimeoutSeconds 5
Wait-TcpPort -Port $backendPort -Name 'Backend' -TimeoutSeconds 30
Wait-TcpPort -Port $frontendPort -Name 'Frontend' -TimeoutSeconds 30

Write-Section 'Database'
$oldMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $mysqlPassword
$tableCount = & (Join-Path $mysqlHome 'bin\mysql.exe') --protocol=tcp --host=127.0.0.1 `
    "--port=$mysqlPort" --user=root -N -e `
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='gaokao_volunteer';"
if ($null -eq $oldMysqlPassword) {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
else {
    $env:MYSQL_PWD = $oldMysqlPassword
}
$tableCount = [int]($tableCount | Select-Object -First 1)
if ($tableCount -lt 5) {
    throw "Expected at least 5 application tables, found $tableCount."
}
Write-Host "[OK] Application database contains $tableCount tables"

$redisPing = & (Join-Path $redisHome 'redis-cli.exe') -h 127.0.0.1 -p $redisPort ping
if ($redisPing -ne 'PONG') {
    throw "Redis ping failed: $redisPing"
}
Write-Host '[OK] Redis ping returned PONG'

Write-Section 'HTTP and business flow'
$universityResponse = Invoke-RestMethod -Uri "http://127.0.0.1:$backendPort/api/universities?page=1&size=5"
if ($universityResponse.code -ne 0 -or $universityResponse.data.records.Count -lt 1) {
    throw 'University query did not return demo records.'
}
Write-Host "[OK] University query returned $($universityResponse.data.records.Count) records"

$studentBody = @{ username = 'student'; password = 'student123' } | ConvertTo-Json
$studentLogin = Invoke-RestMethod -Uri "http://127.0.0.1:$backendPort/api/auth/login" `
    -Method Post -ContentType 'application/json' -Body $studentBody
if ($studentLogin.code -ne 0 -or -not $studentLogin.data.token) {
    throw 'Student login failed.'
}
Write-Host '[OK] Student login succeeded'

$adminBody = @{ username = 'admin'; password = 'admin123' } | ConvertTo-Json
$adminLogin = Invoke-RestMethod -Uri "http://127.0.0.1:$backendPort/api/auth/login" `
    -Method Post -ContentType 'application/json' -Body $adminBody
if ($adminLogin.code -ne 0 -or $adminLogin.data.user.role -ne 'ADMIN') {
    throw 'Admin login failed.'
}
Write-Host '[OK] Admin login succeeded'

$headers = @{ Authorization = "Bearer $($studentLogin.data.token)" }
$recommendationBody = @{
    province = '山西'
    examYear = 2026
    subjectTrack = '物理类'
    score = 560
    provinceRank = 10000
    preferredProvince = ''
    universityLevel = ''
    keyword = ''
} | ConvertTo-Json
$recommendationBytes = [System.Text.Encoding]::UTF8.GetBytes($recommendationBody)
$recommendationResponse = Invoke-RestMethod -Uri "http://127.0.0.1:$backendPort/api/recommendations" `
    -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $recommendationBytes
if ($recommendationResponse.code -ne 0 -or $recommendationResponse.data.Count -lt 1) {
    throw 'Recommendation flow did not return results.'
}
Write-Host "[OK] Recommendation flow returned $($recommendationResponse.data.Count) results"

$favoriteResponse = Invoke-RestMethod -Uri "http://127.0.0.1:$backendPort/api/favorites" -Headers $headers
if ($favoriteResponse.code -ne 0) {
    throw 'Favorite query failed.'
}
Write-Host "[OK] Favorite query returned $($favoriteResponse.data.Count) records"

$frontendResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$frontendPort/login" -UseBasicParsing
if ($frontendResponse.StatusCode -ne 200 -or $frontendResponse.Content -notmatch '<div id="app">') {
    throw 'Frontend login page did not load correctly.'
}
Write-Host '[OK] Frontend login page loaded'

Write-Section 'Verification passed'
