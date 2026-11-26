param([string]$Source = 'artifacts/diagnostics/mc-pilot-source')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$requestedSource = if ([System.IO.Path]::IsPathRooted($Source)) { $Source } else { Join-Path $root $Source }
if (-not (Test-Path -LiteralPath $requestedSource)) {
    New-Item -ItemType Directory -Path (Split-Path -Parent $requestedSource) -Force | Out-Null
    & git -c http.sslBackend=schannel clone --depth 1 --branch v0.20.2 https://github.com/kzheart/mc-pilot.git $requestedSource
    if ($LASTEXITCODE -ne 0) { throw 'Could not obtain the pinned MC Pilot source.' }
}
$sourcePath = (Resolve-Path -LiteralPath $requestedSource).Path
$patch = Join-Path $PSScriptRoot 'patches/mc-pilot-0.20.2-world-init.patch'
$commit = (& git -C $sourcePath rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $commit -ne '49f3424bd240da9af8e70402388bc4d721b3597a') { throw 'Expected MC Pilot v0.20.2 source commit 49f3424.' }
& git -C $sourcePath apply --reverse --check $patch 2>$null
if ($LASTEXITCODE -ne 0) {
    & git -C $sourcePath apply --check $patch
    if ($LASTEXITCODE -ne 0) { throw 'Patch does not match the source; preserve local changes and inspect them.' }
    & git -C $sourcePath apply $patch
    if ($LASTEXITCODE -ne 0) { throw 'Patch failed.' }
}
$settings = Join-Path $sourcePath 'client-mod/settings.gradle.kts'
$text = Get-Content -Raw -LiteralPath $settings
$needle = '    .filter { (it["javaVersion"] as Number).toInt() <= javaVersionMajor }'
$filter = '    .filter { it["gradleModule"].toString() == "version-1.21.1-neoforge" }'
if (-not $text.Contains($filter)) {
    if (-not $text.Contains($needle)) { throw 'Cannot find variant filter in settings.gradle.kts.' }
    $text = $text.Replace($needle, "$needle`n$filter")
    [System.IO.File]::WriteAllText($settings, $text, [System.Text.UTF8Encoding]::new($false))
}
. (Join-Path $PSScriptRoot 'Resolve-Jdk21.ps1')
Push-Location (Join-Path $sourcePath 'client-mod')
try {
    & .\gradlew.bat :version-1.21.1-neoforge:build --no-daemon --max-workers=2
    if ($LASTEXITCODE -ne 0) { throw 'MC Pilot compatibility build failed.' }
    Write-Host 'Built version-1.21.1-neoforge/build/libs/mct-client-mod-neoforge-1.21.1.jar; back up the original before installing.'
} finally { Pop-Location }
