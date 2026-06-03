# 从当前运行的 MySQL 导出 schema / full 两个版本到 deploy/database/
param(
    [string]$MysqlContainer = "campus-life-mysql",
    [string]$RootPassword = "root123456",
    [string]$MainDb = "campus_life",
    [string]$AiDb = "campus_life_ai"
)

$ErrorActionPreference = "Stop"
$DbRoot = Split-Path -Parent $PSScriptRoot

function Export-Database {
    param([string]$DbName, [string]$OutDir)

    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    $tmpSchema = "/tmp/${DbName}_schema.sql"
    $tmpFull = "/tmp/${DbName}_full.sql"

    Write-Host "[export] $DbName -> schema.sql"
    docker exec $MysqlContainer sh -c "mysqldump -uroot -p'$RootPassword' --no-data --routines --triggers --single-transaction --set-gtid-purged=OFF '$DbName' > $tmpSchema"
    docker cp "${MysqlContainer}:${tmpSchema}" (Join-Path $OutDir "schema.sql")

    Write-Host "[export] $DbName -> full.sql"
    docker exec $MysqlContainer sh -c "mysqldump -uroot -p'$RootPassword' --routines --triggers --single-transaction --set-gtid-purged=OFF '$DbName' > $tmpFull"
    docker cp "${MysqlContainer}:${tmpFull}" (Join-Path $OutDir "full.sql")
}

$running = docker ps --format "{{.Names}}" | Select-String -Pattern "^${MysqlContainer}$"
if (-not $running) {
    throw "MySQL container not running: $MysqlContainer"
}

Export-Database -DbName $MainDb -OutDir (Join-Path $DbRoot "campus_life")
Export-Database -DbName $AiDb -OutDir (Join-Path $DbRoot "campus_life_ai")

$mainTables = docker exec $MysqlContainer mysql -uroot -p"$RootPassword" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MainDb'" 2>$null
$aiTables = docker exec $MysqlContainer mysql -uroot -p"$RootPassword" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$AiDb'" 2>$null

$manifest = [ordered]@{
    exportedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    source = [ordered]@{
        container = $MysqlContainer
        mainDatabase = $MainDb
        aiDatabase = $AiDb
    }
    campus_life = [ordered]@{
        tables = [int]$mainTables
        schemaFile = "campus_life/schema.sql"
        fullFile = "campus_life/full.sql"
    }
    campus_life_ai = [ordered]@{
        tables = [int]$aiTables
        schemaFile = "campus_life_ai/schema.sql"
        fullFile = "campus_life_ai/full.sql"
    }
}

$manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8NoBOM (Join-Path $DbRoot "MANIFEST.json")
Write-Host "[export] wrote $(Join-Path $DbRoot 'MANIFEST.json')"
Write-Host "[export] done"
