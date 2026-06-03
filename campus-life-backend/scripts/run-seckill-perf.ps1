param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$Threads = 200,
    [int]$Loops = 50,
    [int]$RampUp = 2,
    [switch]$UseAuthenticatedApi,
    [switch]$SkipSetup,
    [int]$UserCount = 0,
    [int]$Stock = 0,
    [string]$MetadataFile = "D:\code1\code\perf\results\seckill-perf-setup-latest.json",
    [string]$JMeterPath = "D:\Code\Code_App\apache-jmeter-5.6.3\bin\jmeter.bat",
    [string]$CoreJmx = "D:\code1\code\perf\seckill-core-load-test.jmx",
    [string]$ApiJmx = "D:\code1\code\perf\seckill-grab-load-test.jmx",
    [string]$UsersCsv = "D:\code1\code\perf\seckill-users.csv",
    [string]$TokensCsv = "D:\code1\code\perf\seckill-tokens.csv",
    [string]$AdminPhone = "13800000001",
    [string]$AdminPassword = "123456",
    [int]$ConsumerDrainSeconds = 5
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Get-Percentile {
    param(
        [long[]]$Values,
        [double]$Percentile
    )

    if ($Values.Count -eq 0) {
        return 0
    }
    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling($Percentile * $sorted.Count) - 1
    if ($index -lt 0) {
        $index = 0
    }
    if ($index -ge $sorted.Count) {
        $index = $sorted.Count - 1
    }
    [int]$sorted[$index]
}

function Get-MetricDelta {
    param(
        $BeforeMetrics,
        $AfterMetrics,
        [string]$Name
    )

    [long]($AfterMetrics.$Name - $BeforeMetrics.$Name)
}

if ($UserCount -le 0) {
    $UserCount = $Threads * $Loops
}
if ($Stock -le 0) {
    $Stock = $UserCount
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $SkipSetup) {
    & (Join-Path $scriptDir "setup-seckill-perf.ps1") `
        -UserCount $UserCount `
        -Stock $Stock `
        -OutputCsv $UsersCsv `
        -MetadataFile $MetadataFile
}

if (-not (Test-Path $MetadataFile)) {
    throw "Metadata file not found: $MetadataFile"
}

$metadata = Get-Content $MetadataFile -Raw | ConvertFrom-Json
$voucherId = [long]$metadata.voucherId
$baseUri = [Uri]$BaseUrl
$hostName = $baseUri.Host
$port = if ($baseUri.IsDefaultPort) {
    if ($baseUri.Scheme -eq "https") { 443 } else { 80 }
} else {
    $baseUri.Port
}
$protocol = $baseUri.Scheme

if ($UseAuthenticatedApi) {
    & (Join-Path $scriptDir "export-seckill-perf-tokens.ps1") `
        -MetadataFile $MetadataFile `
        -OutputCsv $TokensCsv
}

$adminBody = @{
    phone = $AdminPhone
    password = $AdminPassword
} | ConvertTo-Json
$adminToken = (Invoke-RestMethod `
    -Uri "$BaseUrl/api/auth/tokens" `
    -Method Post `
    -ContentType "application/json" `
    -Body $adminBody).data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

Invoke-RestMethod -Uri "$BaseUrl/api/admin/seckill/metrics/reset" -Method Post -Headers $adminHeaders | Out-Null
Invoke-RestMethod -Uri "$BaseUrl/api/admin/vouchers/$voucherId/preheat" -Method Post -Headers $adminHeaders | Out-Null

$beforeHealth = Invoke-RestMethod -Uri "$BaseUrl/api/admin/seckill/health?voucherId=$voucherId" -Method Get -Headers $adminHeaders

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$mode = if ($UseAuthenticatedApi) { "auth" } else { "core" }
$resultFile = "D:\code1\code\perf\results\seckill-$mode-$timestamp.jtl"
$reportDir = "D:\code1\code\perf\reports\seckill-$mode-$timestamp"
$summaryFile = "D:\code1\code\perf\results\seckill-$mode-$timestamp-summary.json"

$jmx = if ($UseAuthenticatedApi) { $ApiJmx } else { $CoreJmx }
$jmeterArgs = @(
    "-n",
    "-t", $jmx,
    "-l", $resultFile,
    "-JresultsFile=$resultFile",
    "-Jhost=$hostName",
    "-Jport=$port",
    "-Jprotocol=$protocol",
    "-Jthreads=$Threads",
    "-Jloops=$Loops",
    "-Jrampup=$RampUp",
    "-JvoucherId=$voucherId",
    "-JbaseUrl=$BaseUrl",
    "-e",
    "-o", $reportDir
)

if ($UseAuthenticatedApi) {
    $jmeterArgs += "-JtokensFile=$TokensCsv"
} else {
    $jmeterArgs += "-JusersFile=$UsersCsv"
}

& $JMeterPath @jmeterArgs
if ($LASTEXITCODE -ne 0) {
    throw "JMeter exited with code $LASTEXITCODE"
}

Start-Sleep -Seconds $ConsumerDrainSeconds

$afterHealth = Invoke-RestMethod -Uri "$BaseUrl/api/admin/seckill/health?voucherId=$voucherId" -Method Get -Headers $adminHeaders
$afterMonitor = Invoke-RestMethod -Uri "$BaseUrl/api/admin/vouchers/$voucherId/monitor" -Method Get -Headers $adminHeaders

$rows = Import-Csv $resultFile
$total = $rows.Count
$successCount = @($rows | Where-Object { $_.success -eq "true" }).Count
$errorCount = $total - $successCount
$elapsedValues = $rows | ForEach-Object { [long]$_.elapsed }
$minTimestamp = ($rows | Measure-Object timeStamp -Minimum).Minimum
$maxTimestamp = ($rows | Measure-Object timeStamp -Maximum).Maximum
$wallSeconds = [Math]::Round((($maxTimestamp - $minTimestamp) / 1000.0), 2)
$httpThroughput = if ($wallSeconds -gt 0) { [Math]::Round($total / $wallSeconds, 2) } else { 0 }
$httpCodes = $rows |
    Group-Object responseCode |
    Sort-Object Name |
    ForEach-Object {
        [PSCustomObject]@{
            responseCode = $_.Name
            count = $_.Count
            avgMs = [Math]::Round((($_.Group | Measure-Object elapsed -Average).Average), 2)
        }
    }

$beforeMetrics = $beforeHealth.data.metrics
$afterMetrics = $afterHealth.data.metrics
$submitCount = Get-MetricDelta -BeforeMetrics $beforeMetrics -AfterMetrics $afterMetrics -Name "submitRequestCount"
$acceptedCount = Get-MetricDelta -BeforeMetrics $beforeMetrics -AfterMetrics $afterMetrics -Name "acceptedCount"
$luaRejectStock = Get-MetricDelta -BeforeMetrics $beforeMetrics -AfterMetrics $afterMetrics -Name "luaRejectStock"
$luaRejectDuplicate = Get-MetricDelta -BeforeMetrics $beforeMetrics -AfterMetrics $afterMetrics -Name "luaRejectDuplicate"
$mqPublishSuccess = Get-MetricDelta -BeforeMetrics $beforeMetrics -AfterMetrics $afterMetrics -Name "mqPublishSuccessCount"
$processSuccess = Get-MetricDelta -BeforeMetrics $beforeMetrics -AfterMetrics $afterMetrics -Name "processSuccessCount"

$summary = [PSCustomObject]@{
    mode = $mode
    baseUrl = $BaseUrl
    voucherId = $voucherId
    threads = $Threads
    loops = $Loops
    rampUp = $RampUp
    userCount = [int]$metadata.userCount
    stock = [int]$metadata.stock
    totalRequests = $total
    httpSuccessCount = $successCount
    httpErrorCount = $errorCount
    httpSuccessRate = if ($total -gt 0) { [Math]::Round(($successCount * 100.0 / $total), 2) } else { 0 }
    httpThroughputQps = $httpThroughput
    avgMs = [Math]::Round((($rows | Measure-Object elapsed -Average).Average), 2)
    p50Ms = Get-Percentile -Values $elapsedValues -Percentile 0.50
    p95Ms = Get-Percentile -Values $elapsedValues -Percentile 0.95
    p99Ms = Get-Percentile -Values $elapsedValues -Percentile 0.99
    maxMs = ($rows | Measure-Object elapsed -Maximum).Maximum
    wallSeconds = $wallSeconds
    serviceSubmitCount = $submitCount
    serviceSubmitQps = if ($wallSeconds -gt 0) { [Math]::Round($submitCount / $wallSeconds, 2) } else { 0 }
    acceptedCount = $acceptedCount
    acceptedQps = if ($wallSeconds -gt 0) { [Math]::Round($acceptedCount / $wallSeconds, 2) } else { 0 }
    luaRejectStock = $luaRejectStock
    luaRejectDuplicate = $luaRejectDuplicate
    mqPublishSuccess = $mqPublishSuccess
    processSuccess = $processSuccess
    responseCodes = $httpCodes
    resultFile = $resultFile
    reportDir = $reportDir
    metadataFile = $MetadataFile
    finalVoucherMonitor = $afterMonitor.data
}

$summary | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryFile -Encoding UTF8

Write-Host ""
Write-Host "Seckill perf summary"
Write-Host "  mode:              $mode"
Write-Host "  voucherId:         $voucherId"
Write-Host "  totalRequests:     $total"
Write-Host "  httpThroughputQps: $httpThroughput"
Write-Host "  serviceSubmitQps:  $($summary.serviceSubmitQps)"
Write-Host "  acceptedQps:       $($summary.acceptedQps)"
Write-Host "  p95Ms:             $($summary.p95Ms)"
Write-Host "  resultFile:        $resultFile"
Write-Host "  reportDir:         $reportDir"
Write-Host "  summaryFile:       $summaryFile"
