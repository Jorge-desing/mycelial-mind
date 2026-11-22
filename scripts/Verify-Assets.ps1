$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    foreach ($studio in "model", "texture", "particle") {
        $studioDir = "scripts/${studio}_studio"
        uv run --locked python -m unittest discover -s "$studioDir/tests" -v
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        $checkArgs = @("--check")
        if ($studio -eq "texture") { $checkArgs += "--pbr" }
        uv run --locked python "$studioDir/build_${studio}s.py" @checkArgs
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
} finally {
    Pop-Location
}
