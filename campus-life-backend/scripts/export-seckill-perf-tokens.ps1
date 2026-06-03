param(
    [string]$MetadataFile = "D:\code1\code\perf\results\seckill-perf-setup-latest.json",
    [string]$OutputCsv = "D:\code1\code\perf\seckill-tokens.csv",
    [string]$JwtSecret = "campus-life-secret-key-2025-please-change-in-production",
    [int]$TokenTtlDays = 7
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-AccessToken {
    param(
        [long]$UserId,
        [string]$Secret,
        [int]$TtlDays
    )

    $now = [DateTimeOffset]::UtcNow
    $headerJson = '{"alg":"HS384","typ":"JWT"}'
    $payloadJson = '{"userId":' + $UserId + ',"type":"access","jti":"' + ([guid]::NewGuid().ToString()) + '","iat":' + $now.ToUnixTimeSeconds() + ',"exp":' + $now.AddDays($TtlDays).ToUnixTimeSeconds() + '}'
    $headerSegment = ConvertTo-Base64Url ([System.Text.Encoding]::UTF8.GetBytes($headerJson))
    $payloadSegment = ConvertTo-Base64Url ([System.Text.Encoding]::UTF8.GetBytes($payloadJson))
    $unsignedToken = "$headerSegment.$payloadSegment"

    $hmac = [System.Security.Cryptography.HMACSHA384]::new([System.Text.Encoding]::UTF8.GetBytes($Secret))
    try {
        $signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($unsignedToken))
    } finally {
        $hmac.Dispose()
    }

    $signatureSegment = ConvertTo-Base64Url $signatureBytes
    "$unsignedToken.$signatureSegment"
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

if (-not (Test-Path $MetadataFile)) {
    throw "Metadata file not found: $MetadataFile"
}

$metadata = Get-Content $MetadataFile -Raw | ConvertFrom-Json
$usersCsv = [string]$metadata.usersCsv
if (-not (Test-Path $usersCsv)) {
    throw "Users CSV not found: $usersCsv"
}

$outputDir = Split-Path -Parent $OutputCsv
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$rows = Import-Csv $usersCsv | ForEach-Object {
    [PSCustomObject]@{
        token = New-AccessToken -UserId ([long]$_.userId) -Secret $JwtSecret -TtlDays $TokenTtlDays
        userId = [long]$_.userId
        phone = $_.phone
    }
}

Write-PlainCsv -Rows $rows -Columns @("token", "userId", "phone") -Path $OutputCsv

Write-Host "Generated $($rows.Count) access tokens:"
Write-Host "  tokensCsv: $OutputCsv"
