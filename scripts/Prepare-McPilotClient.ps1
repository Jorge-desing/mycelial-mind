param([string]$ClientName = 'mycelial-1211')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$mctHome = if ($env:MCT_HOME) { $env:MCT_HOME } else { Join-Path $env:USERPROFILE '.mct' }
$mods = Join-Path $mctHome "clients\$ClientName\minecraft\mods"
if (!(Test-Path $mods)) { throw "Client does not exist: $ClientName. Create it using Invoke-McPilot.ps1 first." }
$cli = Join-Path $PSScriptRoot '..\.tools\mc-pilot\node_modules\.bin\mct.cmd'
if (!(Test-Path $cli)) { throw 'MC Pilot CLI is not installed locally.' }
$state = ((& $cli client list) -join "`n") | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or -not $state.success) { throw 'Unable to inspect MC Pilot clients.' }
$client = @($state.data.clients | Where-Object { $_.name -eq $ClientName })
if ($client.Count -ne 1 -or $client[0].loader -ne 'neoforge' -or $client[0].mcVersion -ne '1.21.1') { throw 'Expected one existing NeoForge 1.21.1 client.' }
if ($client[0].running) { throw 'Stop the client before replacing its JARs.' }
if (@(Get-NetTCPConnection -LocalPort ([int]$client[0].wsPort) -State Listen -ErrorAction SilentlyContinue).Count -gt 0) { throw 'Client port is still in use; no files changed.' }
. (Join-Path $PSScriptRoot 'Resolve-Jdk21.ps1')
Push-Location $root
try {
    & .\gradlew.bat build
    if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
    $version = (Get-Content gradle.properties | Where-Object { $_ -match '^mod_version\s*=' } | Select-Object -First 1) -replace '^mod_version\s*=\s*',''
    $jar = Join-Path $root "build\libs\mycelialmind-$version.jar"
    if (!(Test-Path $jar)) { throw "Missing build: $jar" }
    Copy-Item -LiteralPath $jar -Destination (Join-Path $mods 'mycelialmind-test.jar') -Force
    Remove-Item -LiteralPath (Join-Path $mods 'smartbrainlib-test.jar') -ErrorAction SilentlyContinue
    Write-Host "Prepared $ClientName with Mycelial Mind $version (native HiveBrain, no SmartBrainLib required)."
} finally { Pop-Location }
