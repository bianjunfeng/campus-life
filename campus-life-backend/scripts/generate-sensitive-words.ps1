[CmdletBinding()]
param(
    [int]$Count = 100000,
    [string]$OutputPath = "D:\code1\code\campus-life-backend\uploads\sensitive-words.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$baseWords = @(
    "赌博",
    "诈骗",
    "毒品",
    "枪支",
    "暴恐",
    "反动",
    "色情",
    "辱骂"
)

$categories = @(
    "赌博", "诈骗", "毒品", "枪支", "暴恐", "反动", "色情", "辱骂",
    "涉黄", "涉政", "涉暴", "引流", "黑产", "外挂", "代刷", "洗钱",
    "违禁", "违规", "非法", "低俗", "挑衅", "谣言", "仇恨", "辱华",
    "伪证", "假证", "售假", "钓鱼", "博彩", "黄牛", "卖号", "封号",
    "爆粉", "水军", "刷单", "代考", "枪粉", "爆炸", "迷药", "军火"
)

$tags = @(
    "推广", "引流", "代办", "教程", "链接", "资源", "群聊", "号码", "入口", "渠道",
    "批发", "零售", "售卖", "转让", "交易", "陪聊", "兼职", "返利", "注册码", "脚本",
    "工具", "速成", "名单", "线路", "方案", "服务", "平台", "接单", "包装", "代充"
)

if ($Count -lt $baseWords.Count) {
    throw "Count 不能小于基础词条数量 $($baseWords.Count)"
}

$outputDir = Split-Path -Parent $OutputPath
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$lines = New-Object 'System.Collections.Generic.List[string]'
$lines.Add("# 敏感词词库（系统生成，适用于性能验证）")
foreach ($word in $baseWords) {
    $lines.Add($word)
}

$index = 1
while ($lines.Count -lt ($Count + 1)) {
    $category = $categories[($index - 1) % $categories.Count]
    $tagIndex = [int](($index - 1) / $categories.Count)
    $tag = $tags[$tagIndex % $tags.Count]
    $lines.Add(("{0}{1}{2:D6}" -f $category, $tag, $index))
    $index++
}

$lines | Set-Content -LiteralPath $OutputPath -Encoding utf8

$words = $lines | Where-Object { $_ -and -not $_.StartsWith("#") }
[pscustomobject]@{
    OutputPath = $OutputPath
    WordCount = $words.Count
    FirstWord = $words[0]
    LastWord = $words[$words.Count - 1]
}
