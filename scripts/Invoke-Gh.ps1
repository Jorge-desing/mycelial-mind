$ErrorActionPreference = "Stop"

$ghCmd = Get-Command gh -ErrorAction SilentlyContinue
if ($ghCmd) {
    $ghExe = $ghCmd.Source
} elseif (Test-Path "C:\Program Files\GitHub CLI\gh.exe") {
    $ghExe = "C:\Program Files\GitHub CLI\gh.exe"
} else {
    throw "GitHub CLI (gh) no encontrado en PATH ni en 'C:\Program Files\GitHub CLI\gh.exe'."
}

& $ghExe @args
exit $LASTEXITCODE
