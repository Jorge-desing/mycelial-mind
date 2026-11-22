param([string]$ClientName = 'mycelial-1211')
$ErrorActionPreference = 'Stop'
$cli = Join-Path $PSScriptRoot '..\.tools\mc-pilot\node_modules\.bin\mct.cmd'
$built = Join-Path $PSScriptRoot '..\artifacts\diagnostics\mc-pilot-source\client-mod\version-1.21.1-neoforge\build\libs\mct-client-mod-neoforge-1.21.1.jar'
if (!(Test-Path $cli) -or !(Test-Path $built)) { throw 'MC Pilot CLI or patched JAR missing; run Build-McPilotCompat.ps1 first.' }
$state = ((& $cli client list) -join "`n") | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or -not $state.success) { throw 'Unable to inspect MC Pilot clients.' }
$client = @($state.data.clients | Where-Object { $_.name -eq $ClientName })
if ($client.Count -ne 1 -or $client[0].loader -ne 'neoforge' -or $client[0].mcVersion -ne '1.21.1') { throw 'Expected one existing NeoForge 1.21.1 client.' }
if ($client[0].running) { throw 'Stop the client before replacing its JAR.' }
if (@(Get-NetTCPConnection -LocalPort ([int]$client[0].wsPort) -State Listen -ErrorAction SilentlyContinue).Count -gt 0) { throw 'Client port is still in use; no files changed.' }
$mctHome = if ($env:MCT_HOME) { $env:MCT_HOME } else { Join-Path $env:USERPROFILE '.mct' }
$target = Join-Path $mctHome "clients\$ClientName\minecraft\mods\mct-client-mod-neoforge-1.21.1.jar"
if (!(Test-Path $target)) { throw "Client mod is missing: $target" }
$oldHash = (Get-FileHash -Algorithm SHA256 $target).Hash.ToLowerInvariant()
$newHash = (Get-FileHash -Algorithm SHA256 $built).Hash.ToLowerInvariant()
if ($oldHash -eq $newHash) { Write-Host "Compatibility JAR already installed ($newHash)."; exit 0 }
$backupDir = Join-Path $PSScriptRoot '..\artifacts\diagnostics\mct-backups'
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
Copy-Item -LiteralPath $target -Destination (Join-Path $backupDir "mct-$oldHash.jar") -Force
Copy-Item -LiteralPath $built -Destination $target -Force
if ((Get-FileHash -Algorithm SHA256 $target).Hash.ToLowerInvariant() -ne $newHash) { throw 'Installed file hash did not match; backup was retained.' }
Write-Host "Installed compatibility JAR $newHash; backup $oldHash retained."
