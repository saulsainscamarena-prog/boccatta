[CmdletBinding()]
param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,

    [switch]$IncludeGenerated
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$suspiciousChars = @{
    ([char]0xFFFD) = "replacement character"
    ([char]0x00C3) = "latin capital A with tilde"
    ([char]0x00C2) = "latin capital A with circumflex"
    ([char]0x00E2) = "latin small a with circumflex"
    ([char]0x00F0) = "latin small eth"
}

$includedExtensions = @(
    ".gradle", ".kts", ".kt", ".java", ".xml", ".json", ".rules", ".txt",
    ".md", ".properties", ".toml", ".yml", ".yaml", ".ps1"
)

$includedFiles = @(
    ".editorconfig", ".gitattributes"
)

$excludedDirectories = @(
    ".git", ".gradle", "build", ".idea", "bocatta-windows-port",
    "dead_code_quarantine", "testsprite_tests", "audit-avd"
)

if ($IncludeGenerated) {
    $excludedDirectories = @(
        ".git", ".gradle", "build", ".idea", "bocatta-windows-port"
    )
}

$excludedFiles = @(
    "build_info.txt"
)

function Get-RelativePathCompat {
    param([string]$Path)

    $base = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if ($fullPath.StartsWith($base, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath.Substring($base.Length).TrimStart('\', '/')
    }
    return $fullPath
}

function Test-IsExcludedPath {
    param([string]$Path)

    $relative = Get-RelativePathCompat $Path
    $parts = $relative -split '[\\/]'
    foreach ($part in $parts) {
        if ($excludedDirectories -contains $part) {
            return $true
        }
    }
    return $false
}

function Get-CandidateFiles {
    param([string]$StartPath)

    $pending = New-Object System.Collections.Generic.Stack[string]
    $pending.Push([System.IO.Path]::GetFullPath($StartPath))

    while ($pending.Count -gt 0) {
        $directory = $pending.Pop()

        foreach ($childDirectory in [System.IO.Directory]::EnumerateDirectories($directory)) {
            if (-not (Test-IsExcludedPath $childDirectory)) {
                $pending.Push($childDirectory)
            }
        }

        foreach ($file in [System.IO.Directory]::EnumerateFiles($directory)) {
            [System.IO.FileInfo]::new($file)
        }
    }
}

$matches = New-Object System.Collections.Generic.List[object]

Get-CandidateFiles $Root | ForEach-Object {
    $file = $_

    if (Test-IsExcludedPath $file.FullName) {
        return
    }

    if (($includedExtensions -notcontains $file.Extension) -and ($includedFiles -notcontains $file.Name)) {
        return
    }

    if ($excludedFiles -contains $file.Name) {
        return
    }

    $lineNumber = 0
    Get-Content -LiteralPath $file.FullName -Encoding UTF8 | ForEach-Object {
        $lineNumber++
        $line = $_
        foreach ($char in $suspiciousChars.Keys) {
            if ($line.Contains([string]$char)) {
                $relative = Get-RelativePathCompat $file.FullName
                $matches.Add([PSCustomObject]@{
                    Path = $relative
                    Line = $lineNumber
                    Reason = $suspiciousChars[$char]
                })
                break
            }
        }
    }
}

if ($matches.Count -gt 0) {
    Write-Host "Mojibake sospechoso detectado:"
    $matches | ForEach-Object {
        $message = "$($_.Path):$($_.Line): possible mojibake ($($_.Reason))"
        Write-Host $message
        if ($env:GITHUB_ACTIONS -eq "true") {
            Write-Host "::error file=$($_.Path),line=$($_.Line)::$($_.Reason)"
        }
    }
    exit 1
}

Write-Host "No se detecto mojibake sospechoso."
