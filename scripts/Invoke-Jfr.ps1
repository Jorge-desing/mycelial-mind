param(
    [Parameter(Mandatory=$true)][int]$ProcessId,
    [ValidateSet('Start','Check','Stop','Threads')][string]$Action = 'Start',
    [ValidateRange(10,3600)][int]$DurationSeconds = 120
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$jcmd = Join-Path 'C:\Program Files\Java\jdk-21' 'bin\jcmd.exe'
if (!(Test-Path $jcmd)) { throw 'JDK 21 jcmd not found.' }

function Get-DiagnosticsDirectory {
    $dir = Join-Path $PSScriptRoot '..\artifacts\diagnostics'
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    return (Resolve-Path $dir).Path
}

function Get-JunctionRoot {
    $root = [System.IO.Path]::GetTempPath().TrimEnd('\')
    if ($root -match '\s') { $root = 'C:\Temp' }
    if ($root -match '\s') { throw 'A temporary path without spaces is required for jcmd JFR filenames.' }
    if (!(Test-Path -LiteralPath $root)) { New-Item -ItemType Directory -Force -Path $root | Out-Null }
    return (Resolve-Path -LiteralPath $root).Path
}

function Remove-JunctionSafe([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) { return }
    if (Test-Path -LiteralPath $Path) {
        try {
            [System.IO.Directory]::Delete($Path, $false)
        } catch {
            & cmd.exe /c rmdir "$Path" | Out-Null
        }
    }
}

function Remove-FileSafe([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path) -or !(Test-Path -LiteralPath $Path -PathType Leaf)) { return }
    $lastError = $null
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        try {
            [System.IO.File]::Delete($Path)
            return
        } catch {
            $lastError = $_
            Start-Sleep -Milliseconds 100
        }
    }
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        throw "Failed to delete pending JFR metadata '$Path': $($lastError.Exception.Message)"
    }
}

function Complete-PendingJfrFiles([string]$DiagnosticsDirectory, [string]$PendingPattern) {
    $completedCount = 0
    foreach ($metadataFile in @(Get-ChildItem -LiteralPath $DiagnosticsDirectory -Filter $PendingPattern -File -ErrorAction SilentlyContinue)) {
        if ($null -eq $metadataFile) { continue }
        $metadata = $null
        try {
            $metadata = Get-Content -Raw -LiteralPath $metadataFile.FullName | ConvertFrom-Json
        } catch {
            continue
        }
        if ($null -eq $metadata -or [string]::IsNullOrWhiteSpace($metadata.outputPath)) { continue }
        if (!(Test-Path -LiteralPath $metadata.outputPath -PathType Leaf)) { continue }
        $profile = Get-Item -LiteralPath $metadata.outputPath
        if ($profile.Length -le 0) { continue }
        Remove-JunctionSafe $metadata.junctionPath
        Remove-FileSafe $metadataFile.FullName
        Write-Host "JFR output path: $($metadata.outputPath)"
        $completedCount++
    }
    return $completedCount
}

if ($Action -eq 'Start') {
    $dir = Get-DiagnosticsDirectory
    $junctionRoot = Get-JunctionRoot
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $baseName = "mycelial-$ProcessId-$stamp"
    $outputPath = Join-Path $dir "$baseName.jfr"
    $junctionPath = Join-Path $junctionRoot "mycelial-jfr-$ProcessId-$stamp"
    $jcmdPath = Join-Path $junctionPath "$baseName.jfr"
    $metadataPath = Join-Path $dir "$baseName.jfr.link.json"

    New-Item -ItemType Junction -Path $junctionPath -Target $dir | Out-Null
    & $jcmd $ProcessId JFR.start name=Mycelial settings=profile "duration=${DurationSeconds}s" "filename=$jcmdPath"
    $jcmdExitCode = $LASTEXITCODE
    if ($jcmdExitCode -eq 0) {
        @{ outputPath = $outputPath; junctionPath = $junctionPath; processId = $ProcessId; durationSeconds = $DurationSeconds } |
            ConvertTo-Json | Set-Content -LiteralPath $metadataPath -Encoding utf8
        Write-Host "JFR output path: $outputPath"
    } else {
        Remove-JunctionSafe $junctionPath
        throw "jcmd failed with exit code $jcmdExitCode"
    }
} elseif ($Action -eq 'Check') {
    $dir = Get-DiagnosticsDirectory
    & $jcmd $ProcessId JFR.check
    $jcmdExitCode = $LASTEXITCODE
    if ($jcmdExitCode -eq 0) {
        $null = Complete-PendingJfrFiles $dir "mycelial-$ProcessId-*.jfr.link.json"
    }
} elseif ($Action -eq 'Stop') {
    $dir = Get-DiagnosticsDirectory
    & $jcmd $ProcessId JFR.stop name=Mycelial
    $jcmdExitCode = $LASTEXITCODE
    [int]$completed = Complete-PendingJfrFiles $dir "mycelial-$ProcessId-*.jfr.link.json"
    if ($jcmdExitCode -ne 0 -and $completed -gt 0) {
        $jcmdExitCode = 0
    }
} else {
    & $jcmd $ProcessId Thread.print
    $jcmdExitCode = $LASTEXITCODE
}
if ($jcmdExitCode -ne 0) { throw "jcmd failed with exit code $jcmdExitCode" }
