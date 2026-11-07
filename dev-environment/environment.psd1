@{
    Jdk = @{
        Name             = 'Eclipse Temurin JDK'
        Version          = '17.0.21+9'
        TargetDirectory  = 'jdk-17'
        ArchiveName      = 'jdk17.zip'
        ArchiveRootGlob  = 'jdk-*'
        Url              = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.21_9.zip'
        Sha256           = '98518ecf554465057ee5509462631deeee374bc368c12a411d225cd29381f84b'
        RequiredFile     = 'bin\java.exe'
        License          = 'GPLv2 with Classpath Exception'
    }
    Maven = @{
        Name             = 'Apache Maven'
        Version          = '3.9.9'
        TargetDirectory  = 'maven-3.9.9'
        ArchiveName      = 'maven.zip'
        ArchiveRootGlob  = 'apache-maven-*'
        Url              = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip'
        Sha256           = '4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c'
        RequiredFile     = 'bin\mvn.cmd'
        License          = 'Apache License 2.0'
    }
    Node = @{
        Name             = 'Node.js'
        Version          = '20.18.1'
        TargetDirectory  = 'node-20.18.1'
        ArchiveName      = 'node.zip'
        ArchiveRootGlob  = 'node-v*-win-x64'
        Url              = 'https://nodejs.org/dist/v20.18.1/node-v20.18.1-win-x64.zip'
        Sha256           = '56e5aacdeee7168871721b75819ccacf2367de8761b78eaceacdecd41e04ca03'
        RequiredFile     = 'npm.cmd'
        License          = 'MIT and third-party licenses'
    }
    MySql = @{
        Name             = 'MySQL Community Server'
        Version          = '8.0.34'
        TargetDirectory  = 'mysql-8.0.34'
        ArchiveName      = 'mysql-8.0.34-winx64.zip'
        ArchiveRootGlob  = 'mysql-*'
        Url              = 'https://cdn.mysql.com/archives/mysql-8.0/mysql-8.0.34-winx64.zip'
        Sha256           = 'd0480039bceef97a558027bfb868b77be1717ec1097410c697db383315243895'
        RequiredFile     = 'bin\mysqld.exe'
        License          = 'GPLv2 or commercial license'
    }
    Redis = @{
        Name             = 'Redis for Windows community build'
        Version          = '5.0.14.1'
        TargetDirectory  = 'redis-5.0.14.1'
        ArchiveName      = 'Redis-x64-5.0.14.1.zip'
        ArchiveRootGlob  = ''
        Url              = 'https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip'
        Sha256           = '018ea18a35876383cbb5f4cd0258adfc87747cf9d619bce1cf73a2e36f720ccf'
        RequiredFile     = 'redis-server.exe'
        License          = 'BSD 3-Clause'
    }
}
