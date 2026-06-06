[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$requiredPaths = @(
    "openspecfig.yaml",
    "AGENTS.md",
    "openspec/constitution.md",
    "openspec/skill-registry.yaml",
    "openspec/source-catalog.yaml",
    "openspec/web-research-policy.md",
    "openspec/templates/research.md",
    "openspec/templates/proposal.md",
    "openspec/templates/spec.md",
    "openspec/templates/design.md",
    "openspec/templates/plan.md",
    "openspec/templates/tasks.md",
    "openspec/templates/review.md",
    "openspec/templates/verification.md",
    "openspec/templates/archive.md",
    "tools/encoding/Find-Mojibake.ps1",
    "gradlew.bat",
    "settings.gradle.kts",
    "gradle/libs.versions.toml"
)

$missing = @()

foreach ($relativePath in $requiredPaths) {
    $fullPath = Join-Path $repoRoot $relativePath
    if (-not (Test-Path $fullPath)) {
        $missing += $relativePath
    }
}

if ($missing.Count -gt 0) {
    Write-Error ("SDD preflight failed. Missing paths: " + ($missing -join ", "))
    exit 1
}

$registryPath = Join-Path $repoRoot "openspec/skill-registry.yaml"
$registryText = Get-Content -Raw -LiteralPath $registryPath
$skillPathMatches = [regex]::Matches($registryText, "path:\s+(.+)")
$missingSkillPaths = @()

foreach ($match in $skillPathMatches) {
    $relativeSkillPath = $match.Groups[1].Value.Trim()
    $fullSkillPath = Join-Path $repoRoot $relativeSkillPath
    if (-not (Test-Path $fullSkillPath)) {
        $missingSkillPaths += $relativeSkillPath
    }
}

if ($missingSkillPaths.Count -gt 0) {
    Write-Error ("SDD preflight failed. Missing registered skill paths: " + ($missingSkillPaths -join ", "))
    exit 1
}

Write-Host "SDD preflight OK"
Write-Host "Workspace: $repoRoot"
Write-Host "Stack: Android, Kotlin, Jetpack Compose, Koin, Firebase Firestore, Room/SQLite, WorkManager"
Write-Host "Docs root: openspec"
Write-Host "Config: openspecfig.yaml"
Write-Host "Skill registry: openspec/skill-registry.yaml"
Write-Host "Registered skill paths: $($skillPathMatches.Count)"
Write-Host "Source catalog: openspec/source-catalog.yaml"
Write-Host "Web research policy: openspec/web-research-policy.md"
Write-Host "Encoding check: .\tools\encoding\Find-Mojibake.ps1"
Write-Host "Default verification: .\gradlew.bat compileDebugKotlin"
