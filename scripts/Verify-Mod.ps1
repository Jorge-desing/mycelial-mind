param(
    [switch]$IncludeServerSmoke,
    [switch]$IncludeVisualSmoke,
    [switch]$IncludeStructureVisualSmoke,
    [switch]$IncludeWorldgenAudit,
    [int]$WorldgenAuditRadius = 8,
    [switch]$ForceRegenerate,
    [switch]$NoDaemon
)

if ($IncludeWorldgenAudit) {
    $IncludeServerSmoke = $true
}

$ErrorActionPreference = "Stop"
$totalTimer = [System.Diagnostics.Stopwatch]::StartNew()

$preferredJavaHome = "C:\Program Files\Java\jdk-21"
if (-not (Test-Path $preferredJavaHome)) {
    $preferredJavaHome = "C:\Program Files\Java\graalvm-jdk-21.0.7+8.1"
}
if (Test-Path $preferredJavaHome) {
    $env:JAVA_HOME = $preferredJavaHome
}
$env:GRADLE_OPTS = "-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT"

$resolvedPythonExe = (& python -c "import sys; print(sys.executable)" 2>$null)
if (-not $resolvedPythonExe -or -not (Test-Path $resolvedPythonExe)) {
    $resolvedPythonExe = "python"
}

function Invoke-ParallelProcesses {
    param([array]$Specs)

    $running = @()
    foreach ($spec in $Specs) {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = if ($spec.FileName -eq "python") { $resolvedPythonExe } else { $spec.FileName }
        $psi.Arguments = $spec.Arguments
        $psi.WorkingDirectory = (Get-Location).Path
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.UseShellExecute = $false
        $psi.CreateNoWindow = $true

        $proc = [System.Diagnostics.Process]::Start($psi)
        $running += [PSCustomObject]@{
            Name   = $spec.Name
            Proc   = $proc
            Out    = $proc.StandardOutput.ReadToEndAsync()
            Err    = $proc.StandardError.ReadToEndAsync()
        }
    }

    $failed = @()
    foreach ($item in $running) {
        $item.Proc.WaitForExit()
        $stdout = $item.Out.GetAwaiter().GetResult()
        $stderr = $item.Err.GetAwaiter().GetResult()
        if ($stdout) { Write-Host "[$($item.Name)]`n$($stdout.TrimEnd())" }
        if ($stderr) { Write-Host "[$($item.Name) stderr]`n$($stderr.TrimEnd())" -ForegroundColor DarkYellow }
        if ($item.Proc.ExitCode -ne 0) {
            $failed += "$($item.Name) (exit $($item.Proc.ExitCode))"
        }
    }
    if ($failed.Count -gt 0) {
        throw "Parallel step failed: $($failed -join ', ')"
    }
}

Write-Host "=== [0/3] Python Studios (Models/Textures/Particles/Sounds), LabPBR 1.3 & Open Agent Skills Validation ===" -ForegroundColor Cyan
$phase0Timer = [System.Diagnostics.Stopwatch]::StartNew()

$modelsCached = $false
$texturesCached = $false
$soundsCached = $false
$particlesCached = $false
if (-not $ForceRegenerate) {
    python scripts\verify_cache.py check models
    $modelsCached = ($LASTEXITCODE -eq 0)
    if ($modelsCached) {
        python scripts\verify_cache.py check textures
        $texturesCached = ($LASTEXITCODE -eq 0)
    }
    python scripts\verify_cache.py check sounds
    $soundsCached = ($LASTEXITCODE -eq 0)
    python scripts\verify_cache.py check particles
    $particlesCached = ($LASTEXITCODE -eq 0)
}

if (-not $modelsCached) {
    Write-Host "[Model Studio] Inputs changed or -ForceRegenerate set; regenerating EntityModels..." -ForegroundColor DarkCyan
    python scripts\model_studio\build_models.py --all
    if ($LASTEXITCODE -ne 0) { throw "Model Studio generation failed." }
    python scripts\verify_cache.py update models
    if ($LASTEXITCODE -ne 0) { throw "Failed to update models cache stamp." }
}

$pySpecs = @(
    @{ Name = "Skills"; FileName = "python"; Arguments = "scripts\validate_agent_skills.py" },
    @{ Name = "ModelTests"; FileName = "python"; Arguments = "-m unittest discover -s scripts\model_studio\tests -v" },
    @{ Name = "TextureTests"; FileName = "python"; Arguments = "-m unittest discover -s scripts\texture_studio\tests -v" },
    @{ Name = "ParticleTests"; FileName = "python"; Arguments = "-m unittest discover -s scripts\particle_studio\tests -v" },
    @{ Name = "SoundTests"; FileName = "python"; Arguments = "-m unittest discover -s scripts\sound_studio\tests -v" }
)
$buildSpecs = @()
if ($modelsCached) {
    $pySpecs += @{ Name = "ModelCheck"; FileName = "python"; Arguments = "scripts\model_studio\build_models.py --check" }
}
if ($texturesCached) {
    $pySpecs += @{ Name = "TextureCheck"; FileName = "python"; Arguments = "scripts\texture_studio\build_textures.py --pbr --check" }
} else {
    Write-Host "[Texture Studio] Inputs changed or -ForceRegenerate set; regenerating textures + LabPBR..." -ForegroundColor DarkCyan
    $buildSpecs += @{ Name = "TextureBuild"; FileName = "python"; Arguments = "scripts\texture_studio\build_textures.py --all --export-extras --pbr --check" }
}
if ($soundsCached) {
    $pySpecs += @{ Name = "SoundCheck"; FileName = "python"; Arguments = "scripts\sound_studio\build_sounds.py --check" }
} else {
    Write-Host "[Sound Studio] Inputs changed or -ForceRegenerate set; regenerating mono Ogg Vorbis sounds..." -ForegroundColor DarkCyan
    $buildSpecs += @{ Name = "SoundBuild"; FileName = "python"; Arguments = "scripts\sound_studio\build_sounds.py --all --check" }
}
if ($particlesCached) {
    $pySpecs += @{ Name = "ParticleCheck"; FileName = "python"; Arguments = "scripts\particle_studio\build_particles.py --check" }
} else {
    Write-Host "[Particle Studio] Inputs changed or -ForceRegenerate set; regenerating particle sprites..." -ForegroundColor DarkCyan
    $buildSpecs += @{ Name = "ParticleBuild"; FileName = "python"; Arguments = "scripts\particle_studio\build_particles.py --all --export-extras --check" }
}

# Studio tests read the generated assets from disk.
Invoke-ParallelProcesses -Specs $buildSpecs
Invoke-ParallelProcesses -Specs $pySpecs
if (-not $texturesCached) {
    python scripts\verify_cache.py update textures
    if ($LASTEXITCODE -ne 0) { throw "Failed to update textures cache stamp." }
}
if (-not $soundsCached) {
    python scripts\verify_cache.py update sounds
    if ($LASTEXITCODE -ne 0) { throw "Failed to update sounds cache stamp." }
}
if (-not $particlesCached) {
    python scripts\verify_cache.py update particles
    if ($LASTEXITCODE -ne 0) { throw "Failed to update particles cache stamp." }
}
$phase0Timer.Stop()
Write-Host ("Phase 0 completed in {0:N1}s (modelsCached=$modelsCached, texturesCached=$texturesCached, soundsCached=$soundsCached)" -f $phase0Timer.Elapsed.TotalSeconds) -ForegroundColor DarkCyan

$datagenCached = $false
if (-not $ForceRegenerate) {
    python scripts\verify_cache.py check datagen
    $datagenCached = ($LASTEXITCODE -eq 0)
}

$gradleArgs = if ($datagenCached) {
    Write-Host "[DataGen] Inputs unchanged (stamp hit); skipping runData." -ForegroundColor DarkCyan
    @("build", "runGameTestServer", "--max-workers=1")
} else {
    @("runData", "build", "runGameTestServer", "--max-workers=1")
}
if ($NoDaemon) { $gradleArgs += "--no-daemon" }

Write-Host "=== [1/3] Gradle DataGen + Build + GameTestServer ===" -ForegroundColor Cyan
$stepTimer = [System.Diagnostics.Stopwatch]::StartNew()
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { throw "Gradle verification ($($gradleArgs -join '/')) failed with exit code $LASTEXITCODE" }
if (-not $datagenCached) {
    python scripts\verify_cache.py update datagen
    if ($LASTEXITCODE -ne 0) { throw "Failed to update datagen cache stamp." }
}
$stepTimer.Stop()
Write-Host ("Phase 1 completed in {0:N1}s" -f $stepTimer.Elapsed.TotalSeconds) -ForegroundColor DarkCyan

$visualProc = $null
$visualOutTask = $null
$visualErrTask = $null
$visualTimer = $null
$screenshot = "run\screenshots\visual_smoke_mobs.png"
$previousWriteTime = if ($IncludeVisualSmoke -and (Test-Path $screenshot)) { (Get-Item $screenshot).LastWriteTimeUtc } else { [datetime]::MinValue }

$structProc = $null
$structOutTask = $null
$structErrTask = $null
$structTimer = $null
$structDir = "artifacts\diagnostics\structure-visual-client\screenshots"
$structStartEpoch = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - 1

if ($IncludeVisualSmoke -and ($IncludeServerSmoke -or $IncludeStructureVisualSmoke)) {
    Write-Host "=== [2+3] Launching Visual Smoke in parallel ===" -ForegroundColor Cyan
    $visualTimer = [System.Diagnostics.Stopwatch]::StartNew()
    $visDaemonFlag = if ($NoDaemon) { " --no-daemon" } else { "" }
    $vpsi = New-Object System.Diagnostics.ProcessStartInfo
    $vpsi.FileName = "cmd.exe"
    $vpsi.Arguments = "/c gradlew.bat runVisualSmoke --max-workers=1$visDaemonFlag"
    $vpsi.WorkingDirectory = (Get-Location).Path
    $vpsi.RedirectStandardOutput = $true
    $vpsi.RedirectStandardError = $true
    $vpsi.UseShellExecute = $false
    $vpsi.CreateNoWindow = $true
    $visualProc = [System.Diagnostics.Process]::Start($vpsi)
    $visualOutTask = $visualProc.StandardOutput.ReadToEndAsync()
    $visualErrTask = $visualProc.StandardError.ReadToEndAsync()
}

if ($IncludeStructureVisualSmoke -and ($IncludeServerSmoke -or $IncludeVisualSmoke)) {
    Write-Host "=== [4] Launching Structure Visual Smoke in parallel ===" -ForegroundColor Cyan
    $structTimer = [System.Diagnostics.Stopwatch]::StartNew()
    $structDaemonFlag = if ($NoDaemon) { " --no-daemon" } else { "" }
    $spsi = New-Object System.Diagnostics.ProcessStartInfo
    $spsi.FileName = "cmd.exe"
    $spsi.Arguments = "/c gradlew.bat runStructureVisualSmoke --max-workers=1$structDaemonFlag"
    $spsi.WorkingDirectory = (Get-Location).Path
    $spsi.RedirectStandardOutput = $true
    $spsi.RedirectStandardError = $true
    $spsi.UseShellExecute = $false
    $spsi.CreateNoWindow = $true
    $structProc = [System.Diagnostics.Process]::Start($spsi)
    $structOutTask = $structProc.StandardOutput.ReadToEndAsync()
    $structErrTask = $structProc.StandardError.ReadToEndAsync()
}

if ($IncludeServerSmoke) {
    Write-Host "=== [2/3] Headless Dedicated Server RCON Smoke Test ===" -ForegroundColor Cyan
    $smokeTimer = [System.Diagnostics.Stopwatch]::StartNew()
    $smokeDir = Join-Path (Get-Location).Path ("artifacts\diagnostics\server-smoke-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $smokeDir -Force | Out-Null
    "eula=true" | Set-Content (Join-Path $smokeDir 'eula.txt')
    $smokeProps = @(
        'server-ip=127.0.0.1',
        'server-port=25577',
        'enable-rcon=true',
        'rcon.port=25578',
        'rcon.password=mycelialtest',
        'online-mode=false',
        'allow-nether=false',
        'spawn-protection=0',
        'max-tick-time=-1',
        'view-distance=2',
        'simulation-distance=2'
    )
    if (-not $IncludeWorldgenAudit) {
        $smokeProps += 'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}'
        $smokeProps += 'level-type=minecraft:flat'
    }
    $smokeProps | Set-Content (Join-Path $smokeDir 'server.properties')

    $serverDaemonFlag = if ($NoDaemon) { " --no-daemon" } else { "" }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c gradlew.bat runSmokeServer `"-PsmokeRunDirectory=$smokeDir`"$serverDaemonFlag 2>&1"
    $psi.WorkingDirectory = (Get-Location).Path
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $false
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $proc = [System.Diagnostics.Process]::Start($psi)
    $rconRan = $false
    $deadline = (Get-Date).AddMinutes(2)
    $readTask = $null

    while ((Get-Date) -lt $deadline) {
        if ($null -eq $readTask) {
            $readTask = $proc.StandardOutput.ReadLineAsync()
        }
        if ($readTask.Wait(200)) {
            $line = $readTask.Result
            $readTask = $null
            if ($null -eq $line) { break }
            Write-Host $line
            if (-not $rconRan -and $line -match "RCON running on") {
                $rconRan = $true
                $rconArgs = @("scripts\rcon_verify.py", "127.0.0.1", "25578", "mycelialtest")
                if ($IncludeWorldgenAudit) {
                    $rconArgs += @("--worldgen-audit", "--worldgen-radius", "$WorldgenAuditRadius")
                }
                python @rconArgs
                if ($LASTEXITCODE -ne 0) {
                    $proc.Kill()
                    throw "RCON verification script failed."
                }
            }
        } elseif ($proc.HasExited) {
            break
        }
    }

    if (-not $proc.HasExited) {
        $proc.Kill()
        throw "Dedicated server smoke test timed out (preserved $smokeDir)."
    }
    if (-not $rconRan -or $proc.ExitCode -ne 0) {
        throw "Dedicated server smoke test failed (rconRan=$rconRan, exitCode=$($proc.ExitCode), preserved $smokeDir)."
    }

    Remove-Item -Recurse -Force $smokeDir -ErrorAction SilentlyContinue
    Get-ChildItem -Path "artifacts\diagnostics" -Directory -Filter "server-smoke-*" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -Skip 2 |
        ForEach-Object { Remove-Item -Recurse -Force $_.FullName -ErrorAction SilentlyContinue }

    $smokeTimer.Stop()
    Write-Host ("Dedicated server RCON smoke test PASSED in {0:N1}s!" -f $smokeTimer.Elapsed.TotalSeconds) -ForegroundColor Green
}

if ($IncludeVisualSmoke) {
    Write-Host "=== [3/3] Fresh visual smoke and regression (SSIM + pHash) ===" -ForegroundColor Cyan
    if ($null -ne $visualProc) {
        $visualProc.WaitForExit()
        $vOut = $visualOutTask.GetAwaiter().GetResult()
        $vErr = $visualErrTask.GetAwaiter().GetResult()
        if ($vOut) { Write-Host $vOut }
        if ($vErr) { Write-Host $vErr -ForegroundColor DarkYellow }
        if ($visualProc.ExitCode -ne 0) { throw "Visual smoke client failed with exit code $($visualProc.ExitCode)." }
        $visualTimer.Stop()
        Write-Host ("Parallel Visual Smoke client finished in {0:N1}s" -f $visualTimer.Elapsed.TotalSeconds) -ForegroundColor DarkCyan
    } else {
        $visualArgs = @("runVisualSmoke", "--max-workers=1")
        if ($NoDaemon) { $visualArgs += "--no-daemon" }
        & .\gradlew.bat @visualArgs
        if ($LASTEXITCODE -ne 0) { throw "Visual smoke client failed." }
    }
    if (-not (Test-Path $screenshot) -or (Get-Item $screenshot).LastWriteTimeUtc -le $previousWriteTime) {
        throw "Visual smoke did not produce a fresh screenshot."
    }
    python scripts\visual_regression.py
    if ($LASTEXITCODE -ne 0) { throw "Visual regression check failed." }
    $bossScreenshot = "run\screenshots\visual_smoke_bosses.png"
    $bossBaseline = "scripts\model_studio\exports\baseline_visual_smoke_bosses.png"
    if (Test-Path $bossScreenshot) {
        python scripts\visual_regression.py --baseline $bossBaseline --current $bossScreenshot --heatmap "artifacts\diagnostics\visual_diff_heatmap_bosses.png"
        if ($LASTEXITCODE -ne 0) { throw "Boss visual regression check failed." }
    }
} else {
    Write-Host "Visual smoke skipped; pass -IncludeVisualSmoke to capture and compare a fresh image." -ForegroundColor DarkCyan
}

if ($IncludeStructureVisualSmoke) {
    Write-Host "=== [4/4] Optional Structure Visual Smoke (Outbreak, SurfaceHive, Sanctum, Citadel) ===" -ForegroundColor Cyan
    if ($null -ne $structProc) {
        $structProc.WaitForExit()
        $sOut = $structOutTask.GetAwaiter().GetResult()
        $sErr = $structErrTask.GetAwaiter().GetResult()
        if ($sOut) { Write-Host $sOut }
        if ($sErr) { Write-Host $sErr -ForegroundColor DarkYellow }
        if ($structProc.ExitCode -ne 0) { throw "Structure visual smoke client failed with exit code $($structProc.ExitCode)." }
        $structTimer.Stop()
        Write-Host ("Parallel Structure Visual Smoke client finished in {0:N1}s" -f $structTimer.Elapsed.TotalSeconds) -ForegroundColor DarkCyan
    } else {
        $structStartEpoch = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - 1
        $structArgs = @("runStructureVisualSmoke", "--max-workers=1")
        if ($NoDaemon) { $structArgs += "--no-daemon" }
        & .\gradlew.bat @structArgs
        if ($LASTEXITCODE -ne 0) { throw "Structure visual smoke client failed." }
    }
    python scripts\structure_visual_check.py --screenshot-dir $structDir --min-mtime $structStartEpoch
    if ($LASTEXITCODE -ne 0) { throw "Structure visual check failed." }
    Get-ChildItem -Path "artifacts\diagnostics\structure-visual-client\saves" -Directory -Filter "structure_visual_smoke_*" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -Skip 1 |
        ForEach-Object { Remove-Item -Recurse -Force $_.FullName -ErrorAction SilentlyContinue }
} else {
    Write-Host "Structure visual smoke skipped; pass -IncludeStructureVisualSmoke to capture structure screenshots." -ForegroundColor DarkCyan
}

$totalTimer.Stop()
Write-Host ("ALL VERIFICATION STEPS COMPLETED SUCCESSFULLY in {0:N1}s." -f $totalTimer.Elapsed.TotalSeconds) -ForegroundColor Green
