param(
    [string]$MySqlPath = "D:\Code\Code_App\mysql-8.0.31-winx64\bin\mysql.exe",
    [string]$DbHost = "8.130.80.46",
    [int]$DbPort = 3306,
    [string]$DbName = "campus_life",
    [string]$DbUser = "campus_user",
    [string]$DbPassword = "020907BJf",
    [long]$BasePhone = 16610000000,
    [int]$UserCount = 5000,
    [int]$Stock = 5000,
    [long]$MerchantId = 1,
    [string]$PasswordHash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    [string]$OutputCsv = "D:\code1\code\perf\seckill-users.csv",
    [string]$MetadataFile = "D:\code1\code\perf\results\seckill-perf-setup-latest.json"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-MySqlQuery {
    param([string]$Query)

    & $MySqlPath `
        -h $DbHost `
        -P $DbPort `
        -u $DbUser `
        "-p$DbPassword" `
        -D $DbName `
        --batch `
        --raw `
        --skip-column-names `
        -e $Query

    if ($LASTEXITCODE -ne 0) {
        throw "mysql exited with code $LASTEXITCODE"
    }
}

function Write-PlainCsv {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Rows,
        [Parameter(Mandatory = $true)]
        [string[]]$Columns,
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add(($Columns -join ","))
    foreach ($row in $Rows) {
        $values = foreach ($column in $Columns) {
            [string]$row.$column
        }
        $lines.Add(($values -join ","))
    }
    [System.IO.File]::WriteAllLines($Path, $lines, [System.Text.UTF8Encoding]::new($false))
}

if ($UserCount -le 0) {
    throw "UserCount must be positive."
}

if ($Stock -le 0) {
    throw "Stock must be positive."
}

$outputDir = Split-Path -Parent $OutputCsv
$metadataDir = Split-Path -Parent $MetadataFile
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
New-Item -ItemType Directory -Force -Path $metadataDir | Out-Null

$merchantExists = Invoke-MySqlQuery "SELECT COUNT(1) FROM merchant WHERE id = $MerchantId;"
if ([int]$merchantExists -le 0) {
    throw "Merchant $MerchantId does not exist."
}

$title = "perf-seckill-" + (Get-Date -Format "yyyyMMddHHmmss")
$escapedTitle = $title.Replace("'", "''")
$startTime = (Get-Date).AddMinutes(-5).ToString("yyyy-MM-dd HH:mm:ss")
$endTime = (Get-Date).AddDays(2).ToString("yyyy-MM-dd HH:mm:ss")
$maxPhone = $BasePhone + $UserCount - 1
$cteDepth = [Math]::Max($UserCount + 10, 1000)

$seedUsersSql = @"
SET SESSION cte_max_recursion_depth = $cteDepth;
INSERT INTO ``user`` (
    phone,
    email,
    username,
    password_hash,
    role,
    status,
    avatar_url,
    bio,
    gender,
    region,
    create_time,
    update_time
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n + 1 < $UserCount
)
SELECT
    CAST($BasePhone + n AS CHAR),
    CONCAT('perf', LPAD(n + 1, 5, '0'), '@example.test'),
    CONCAT('perf_user_', LPAD(n + 1, 5, '0')),
    '$PasswordHash',
    0,
    1,
    NULL,
    'seckill perf user',
    0,
    'perf',
    NOW(),
    NOW()
FROM seq
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    status = VALUES(status),
    update_time = NOW();
"@
Invoke-MySqlQuery $seedUsersSql | Out-Null

$voucherSql = @"
INSERT INTO voucher (
    merchant_id,
    title,
    sub_title,
    image_url,
    stock,
    sold_count,
    amount,
    pay_value,
    status,
    begin_time,
    end_time,
    create_time,
    update_time
) VALUES (
    $MerchantId,
    '$escapedTitle',
    'perf seckill voucher',
    NULL,
    $Stock,
    0,
    10.00,
    1.00,
    1,
    '$startTime',
    '$endTime',
    NOW(),
    NOW()
);
SELECT LAST_INSERT_ID();
"@
$voucherIdText = Invoke-MySqlQuery $voucherSql | Select-Object -Last 1
$voucherId = [long]$voucherIdText

$seckillSql = @"
INSERT INTO seckill_voucher (
    voucher_id,
    stock,
    start_time,
    end_time,
    create_time
) VALUES (
    $voucherId,
    $Stock,
    '$startTime',
    '$endTime',
    NOW()
);
"@
Invoke-MySqlQuery $seckillSql | Out-Null

$userRows = Invoke-MySqlQuery "SELECT id, phone FROM ``user`` WHERE phone BETWEEN '$BasePhone' AND '$maxPhone' ORDER BY phone;"
$users = foreach ($row in $userRows) {
    $parts = $row -split "`t"
    [PSCustomObject]@{
        userId = [long]$parts[0]
        phone = $parts[1]
    }
}

if ($users.Count -ne $UserCount) {
    throw "Expected $UserCount users, but found $($users.Count)."
}

Write-PlainCsv -Rows $users -Columns @("userId", "phone") -Path $OutputCsv

$metadata = [PSCustomObject]@{
    createdAt = (Get-Date).ToString("s")
    merchantId = $MerchantId
    voucherId = $voucherId
    title = $title
    stock = $Stock
    userCount = $UserCount
    basePhone = $BasePhone
    maxPhone = $maxPhone
    startTime = $startTime
    endTime = $endTime
    usersCsv = $OutputCsv
}
$metadata | ConvertTo-Json -Depth 4 | Set-Content -Path $MetadataFile -Encoding UTF8

Write-Host "Created seckill perf data:"
Write-Host "  voucherId: $voucherId"
Write-Host "  stock:     $Stock"
Write-Host "  users:     $UserCount"
Write-Host "  usersCsv:  $OutputCsv"
Write-Host "  metadata:  $MetadataFile"
