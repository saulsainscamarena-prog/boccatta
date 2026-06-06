[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter(Mandatory = $true)]
    [string] $Id,

    [string] $Title,

    [string] $Capability = "general",

    [switch] $StrictTdd,

    [switch] $NoStrictTdd,

    [switch] $Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Id -notmatch '^[a-z0-9]+(-[a-z0-9]+)*$') {
    throw "Spec Id must be kebab-case using lowercase letters, numbers, and hyphens."
}

if ($StrictTdd -and $NoStrictTdd) {
    throw "Use either -StrictTdd or -NoStrictTdd, not both."
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$openspecRoot = Join-Path $repoRoot "openspec"
$templatesRoot = Join-Path $openspecRoot "templates"
$changeDir = Join-Path (Join-Path $openspecRoot "changes") $Id

if (-not (Test-Path $templatesRoot)) {
    throw "Templates folder not found: $templatesRoot"
}

if ([string]::IsNullOrWhiteSpace($Title)) {
    $Title = ($Id -replace '-', ' ')
}

if ((Test-Path $changeDir) -and -not $Force) {
    throw "Spec already exists: $changeDir. Use -Force only for draft specs without real work."
}

if (-not $PSCmdlet.ShouldProcess($changeDir, "Create SDD spec")) {
    Write-Host "Preview complete. No files were created."
    return
}

New-Item -ItemType Directory -Force -Path $changeDir | Out-Null

$date = Get-Date -Format "yyyy-MM-dd"
$gradleVerify = ".\gradlew.bat compileDebugKotlin"
$strictTddValue = if ($NoStrictTdd) { "false" } else { "true" }

$templateMap = @{
    "research.md" = "research.md"
    "proposal.md" = "proposal.md"
    "spec.md" = "spec.md"
    "design.md" = "design.md"
    "plan.md" = "plan.md"
    "tasks.md" = "tasks.md"
    "review.md" = "review.md"
    "verification.md" = "verification.md"
    "archive.md" = "archive.md"
}

foreach ($entry in $templateMap.GetEnumerator()) {
    $source = Join-Path $templatesRoot $entry.Key
    $target = Join-Path $changeDir $entry.Value

    if (-not (Test-Path $source)) {
        throw "Missing template: $source"
    }

    $content = Get-Content -Raw -Path $source
    $content = $content.Replace("{{ID}}", $Id)
    $content = $content.Replace("{{TITLE}}", $Title)
    $content = $content.Replace("{{CAPABILITY}}", $Capability)
    $content = $content.Replace("{{DATE}}", $date)
    $content = $content.Replace("{{GRADLE_VERIFY}}", $gradleVerify)
    $content = $content.Replace("{{STRICT_TDD}}", $strictTddValue)

    Set-Content -Path $target -Value $content -Encoding UTF8
}

Write-Host "Created SDD spec: $changeDir"
Write-Host "Next: complete spec.md, plan.md, tasks.md, and verification.md before editing code."
