param(
    [string]$EsHome = "",
    [int]$HeapMb = 512,
    [int]$Port = 9200
)

$ErrorActionPreference = "Stop"

function Resolve-EsHome {
    param([string]$InputPath)

    if ($InputPath -and (Test-Path -LiteralPath $InputPath)) {
        return (Resolve-Path -LiteralPath $InputPath).Path
    }

    $toolsDir = "D:\code1\code\tools"
    if (!(Test-Path -LiteralPath $toolsDir)) {
        throw "未找到 tools 目录：$toolsDir。请先把 Elasticsearch Windows 压缩包解压到该目录。"
    }

    $candidate = Get-ChildItem -Path $toolsDir -Directory |
        Where-Object { $_.Name -like "elasticsearch-*" } |
        Sort-Object Name -Descending |
        Select-Object -First 1

    if ($null -eq $candidate) {
        throw "未找到 Elasticsearch 目录。请先把 Elasticsearch Windows 压缩包解压到 $toolsDir。"
    }

    return $candidate.FullName
}

function Test-PortListening {
    param([int]$LocalPort)

    $matches = netstat -ano | Select-String ":$LocalPort\s+.*LISTENING"
    return $null -ne $matches
}

$resolvedEsHome = Resolve-EsHome -InputPath $EsHome
$esBat = Join-Path $resolvedEsHome "bin\elasticsearch.bat"
if (!(Test-Path -LiteralPath $esBat)) {
    throw "未找到启动文件：$esBat"
}

if (Test-PortListening -LocalPort $Port) {
    Write-Host "端口 $Port 已被监听，跳过启动。"
    exit 0
}

$workspaceRoot = "D:\code1\code"
$dataDir = Join-Path $workspaceRoot "var\elasticsearch\data"
$logsDir = Join-Path $workspaceRoot "var\elasticsearch\logs"
$tmpDir = Join-Path $workspaceRoot "var\elasticsearch\tmp"
$runLogsDir = Join-Path $workspaceRoot "logs\local-es"
$stdoutLog = Join-Path $runLogsDir "stdout.log"
$stderrLog = Join-Path $runLogsDir "stderr.log"
if (Test-Path Env:CLASSPATH) {
    Remove-Item Env:CLASSPATH
}
if (Test-Path Env:JAVA_HOME) {
    Remove-Item Env:JAVA_HOME
}
if (Test-Path Env:ES_CLASSPATH) {
    Remove-Item Env:ES_CLASSPATH
}

New-Item -ItemType Directory -Force -Path $dataDir, $logsDir, $tmpDir, $runLogsDir | Out-Null

$env:TEMP = $tmpDir
$env:TMP = $tmpDir
$env:ES_TMPDIR = $tmpDir
$env:ES_JAVA_OPTS = "-Xms${HeapMb}m -Xmx${HeapMb}m -Djava.io.tmpdir=$tmpDir"

$arguments = @(
    "-E", "http.port=$Port",
    "-E", "discovery.type=single-node",
    "-E", "xpack.security.enabled=false",
    "-E", "path.data=$dataDir",
    "-E", "path.logs=$logsDir"
)

$process = Start-Process -FilePath $esBat `
    -ArgumentList $arguments `
    -WorkingDirectory $resolvedEsHome `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Write-Host "Elasticsearch 启动中，PID=$($process.Id)"
Write-Host "ES_HOME: $resolvedEsHome"
Write-Host "stdout: $stdoutLog"
Write-Host "stderr: $stderrLog"

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 2
    try {
        $response = Invoke-WebRequest "http://127.0.0.1:$Port" -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200) {
            Write-Host "Elasticsearch 已就绪：http://127.0.0.1:$Port"
            exit 0
        }
    } catch {
        if ($process.HasExited) {
            throw "Elasticsearch 进程已退出。请查看日志：$stderrLog"
        }
    }
}

throw "Elasticsearch 在预期时间内未就绪。请查看日志：$stdoutLog 和 $stderrLog"
