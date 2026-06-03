param(
    [string]$MavenRepoLocal
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $MavenRepoLocal) {
    $MavenRepoLocal = (Join-Path $projectRoot "target/.m2repo").Replace("\", "/")
}

function Set-EnvDefault {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )

    if ([Environment]::GetEnvironmentVariable($Name, "Process")) {
        return
    }

    $existingValue = Get-EnvAnyScope $Name
    if ($existingValue) {
        Set-Item -Path "Env:$Name" -Value $existingValue
    } else {
        Set-Item -Path "Env:$Name" -Value $Value
    }
}

function Get-EnvAnyScope {
    param(
        [Parameter(Mandatory = $true)][string]$Name
    )

    foreach ($scope in @("Process", "User", "Machine")) {
        $value = [Environment]::GetEnvironmentVariable($Name, $scope)
        if ($value) {
            return $value
        }
    }
    return $null
}

function Copy-EnvFallback {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string[]]$FallbackNames
    )

    if ([Environment]::GetEnvironmentVariable($Name, "Process")) {
        return
    }

    foreach ($fallbackName in $FallbackNames) {
        $fallbackValue = Get-EnvAnyScope $fallbackName
        if ($fallbackValue) {
            Set-Item -Path "Env:$Name" -Value $fallbackValue
            return
        }
    }
}

Set-EnvDefault "SPRING_PROFILES_ACTIVE" "local"
Set-EnvDefault "SPRING_DATASOURCE_URL" "jdbc:mysql://8.130.80.46:3306/campus_life_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
Set-EnvDefault "SPRING_DATASOURCE_USERNAME" "campus_life_ai_user"
Set-EnvDefault "SPRING_DATASOURCE_PASSWORD" "ChangeMe_2026!"
Set-EnvDefault "JWT_SECRET" "campus-life-secret-key-2025-please-change-in-production"
Set-EnvDefault "PLATFORM_USER_SERVICE_BASE_URL" "http://127.0.0.1:8080"

Set-EnvDefault "AI_DEFAULT_PROVIDER_CODE" "openai-compatible"
Set-EnvDefault "AI_DEFAULT_MODEL_CODE" "qwen-plus"
Set-EnvDefault "AI_OPENAI_COMPATIBLE_ENABLED" "true"
Set-EnvDefault "AI_OPENAI_COMPATIBLE_PROVIDER_CODE" "openai-compatible"
Set-EnvDefault "AI_OPENAI_COMPATIBLE_PROVIDER_NAME" "Qwen"
Set-EnvDefault "AI_OPENAI_COMPATIBLE_BASE_URL" "https://dashscope.aliyuncs.com/compatible-mode/v1"
Set-EnvDefault "AI_OPENAI_COMPATIBLE_MODEL" "qwen-plus"
Copy-EnvFallback "AI_API_KEY_ENCRYPTION_SECRET" @("AI_API_KEY_ENCRYPTION_SECRET")
Copy-EnvFallback "AI_OPENAI_COMPATIBLE_API_KEY" @("AI_OPENAI_COMPATIBLE_API_KEY", "DASHSCOPE_API_KEY")
Copy-EnvFallback "AI_KNOWLEDGE_EMBEDDING_API_KEY" @("AI_OPENAI_COMPATIBLE_API_KEY", "DASHSCOPE_API_KEY")

Set-EnvDefault "AI_KNOWLEDGE_VECTOR_ENABLED" "true"
Set-EnvDefault "AI_KNOWLEDGE_VECTOR_FALLBACK_KEYWORD_SEARCH" "true"
Set-EnvDefault "MILVUS_URI" "http://localhost:19530"

$missing = @()
if (-not [Environment]::GetEnvironmentVariable("AI_API_KEY_ENCRYPTION_SECRET", "Process")) {
    $missing += "AI_API_KEY_ENCRYPTION_SECRET"
}
if (-not [Environment]::GetEnvironmentVariable("AI_OPENAI_COMPATIBLE_API_KEY", "Process")) {
    Write-Warning "AI_OPENAI_COMPATIBLE_API_KEY or DASHSCOPE_API_KEY is not set in this process; the service will use the encrypted provider key from database if configured."
}
if ($missing.Count -gt 0) {
    Write-Warning ("AI service is starting, but these environment variables are not set: " + ($missing -join ", "))
}

Set-Location $projectRoot
& mvn "-Dmaven.repo.local=$MavenRepoLocal" "-Dmaven.test.skip=true" spring-boot:run
