param([string]$ClientName = 'mycelial-1211')
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$invoke = Join-Path $PSScriptRoot '..\.tools\mc-pilot\node_modules\.bin\mct.cmd'
if (-not (Test-Path $invoke)) { throw 'MC Pilot is not installed locally.' }
$env:NODE_USE_SYSTEM_CA = '1'
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
$listingText = & $invoke client list
if ($LASTEXITCODE -ne 0) { throw 'Cannot query MC Pilot client state.' }
$listing = ($listingText -join "`n") | ConvertFrom-Json
$client = @($listing.data.clients | Where-Object { $_.name -eq $ClientName })
if ($client.Count -ne 1) { throw "Client not found or ambiguous: $ClientName" }
$port = [int]$client[0].wsPort
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $resultText = @(& $invoke client stop $ClientName 2>&1 | ForEach-Object {
        if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.Exception.Message } else { $_.ToString() }
    })
    $stopCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $prevEap
}
$result = ($resultText -join "`n") | ConvertFrom-Json
if ($stopCode -eq 0 -and $result.success) { $resultText; exit 0 }
if ($result.error.code -ne 'PORT_CONFLICT') {
    $resultText
    if ($stopCode -eq 0) { exit 1 }
    exit $stopCode
}
# MC Pilot can report a transient child-process conflict while stopping. Do not
# signal any listener: success requires both no listener and a stopped client.
$timer = [System.Diagnostics.Stopwatch]::StartNew()
while ($timer.Elapsed.TotalSeconds -lt 5) {
    Start-Sleep -Milliseconds 200
    $listen = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    $stateText = & $invoke client list
    if ($LASTEXITCODE -ne 0) { break }
    $state = ($stateText -join "`n") | ConvertFrom-Json
    $current = @($state.data.clients | Where-Object { $_.name -eq $ClientName })
    if ($listen.Count -eq 0 -and $current.Count -eq 1 -and -not $current[0].running) {
        Write-Output "MC Pilot reported a transient PORT_CONFLICT; verified $ClientName stopped and port $port is free."
        exit 0
    }
}
$resultText
if ($stopCode -eq 0) { exit 1 }
exit $stopCode
