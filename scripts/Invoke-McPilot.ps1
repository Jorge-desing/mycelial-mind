param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MctArgs)
$ErrorActionPreference = 'Stop'
$cli = Join-Path $PSScriptRoot '..\.tools\mc-pilot\node_modules\.bin\mct.cmd'
if (!(Test-Path $cli)) { throw 'Run npm install --prefix .tools/mc-pilot --no-audit --no-fund --ignore-scripts @kzheart_/mc-pilot@0.20.2 first.' }
$env:NODE_USE_SYSTEM_CA = '1'
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
& $cli @MctArgs
exit $LASTEXITCODE
