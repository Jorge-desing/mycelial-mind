# Ciclo de Vida en Vivo: Servidor Aislado, RCON Parseado, MC Pilot y JFR

## 1. Sesión Unificada (Servidor Smoke Aislado + Cliente MC Pilot)
```python
sess = await call_tool("start_live_session", {
    "client_name": "mycelial-1211",
    "world_type": "flat",
    "launch_client": False,  # True para lanzar también MC Pilot
})
if not sess["ok"]:
    return sess

rcon_port = sess["server"]["rcon_port"]
rcon_pass = sess["server"]["rcon_password"]

status = await call_tool("get_live_hive_status", {"port": rcon_port, "password": rcon_pass})
verify = await call_tool("run_live_verify_scenario", {"scenario": "hive_ai", "port": rcon_port, "password": rcon_pass})
stopped = await call_tool("stop_live_session", {"session_id": sess["session_id"], "cleanup_dir": True})
return {"status": status, "verify": verify, "stopped": stopped["ok"]}
```

## 2. Comandos `/mycelial` soportados por RCON (`rcon_command` / `run_rcon_batch`)
- `mycelial status` (parseado automáticamente por `get_live_hive_status` en `threat_pct`, `tier`, `milestone_pct`, `protected`, `traits`, `tier2_traits`, `synergies`, `links`, `suppressed_secs`, `nearest_node`, `breached`, `matriarch_defeated`, `remission_secs`)
- `mycelial colony <status|spawn <caste>|upgrade|biomass <n>|raid|reset>` (parseado automáticamente por `get_live_colony_status`: `nexus`, `tier`, `radius_m`, `population`, `population_cap`, `biomass`, `morale_pct`, `domain_xp`, `trait_mask_hex`, `outposts`, `orders`)
- `mycelial cycle <status|start <mutators>|abort|reset>` (parseado por `get_live_colony_status(include_cycle=True)`: `unlocked`, `active`, `cycle_number`, `mutators`, `completed_cycles`, `best_score`, `best_cycle`, `last_score`, `last_grade`)
- `mycelial reset`
- `mycelial stimulus <MELEE|PROJECTILE|FIRE|EXPLOSION|MAGIC> <0.1..500.0>`
- `mycelial symbiosis <status|clear|grant <mutagen>>`
- `mycelial diplomacy <strain> <-100..100>` | `mycelial schism <strain>` | `mycelial habits` | `mycelial routes`
- `mycelial homeostasis <status|stress <0..100>|sedate>`
- `mycelial worldgen audit [radiusChunks] [sanctumGridChunks]`
- `mycelial verify <projectile_adaptation|pylon_containment|boss_encounter|tier2_synergy|synaptic_network|symbiosis|units|expansion|tide|counteroffensive|arsenal|mycorrhizal|hive_ai|chimera|memory|tactical_ai|field_ai|hyphal_siege|loot|homeostasis|polish|dimension_progression|cycles|colony|worldgen|all>`

## 3. Escenarios Parametrizables y Reportes Persistentes (`run_live_scenario`)
Presets integrados: `progression_ramp`, `swarm_load`, `diplomacy_and_memory`, `homeostasis_and_dimension`, `colony_and_dialects`, `cycles_and_endgame`, `full_smoke_matrix`, o `custom` (`custom_commands=[...]`).
- Pasa `enable_diagnostics=True` en `start_smoke_server` / `start_live_session` / `run_live_scenario` para cargar `-Pdiagnostics` (`spark` + `Observable`).
- Con `capture_perf=True`, recolecta TPS/MSPT/CPU vía `collect_spark_tps` (si hay cliente MC Pilot) o graba automáticamente JFR (`Start` → escenario RCON → `Stop` → `summarize_jfr`, configurable con `jfr_duration_secs >= 10`) filtrando `GradleDaemon`/`jcmd` sobre el PID real de Minecraft.
- Con `capture_screenshot=True`, guarda la captura vía `capture_mcpilot_screenshot`.
- Cada corrida se guarda en `artifacts/diagnostics/live-reports/<run_id>.json` y `live-report-latest.json`. Usa `get_latest_live_report()` y `compare_live_reports(before_path, after_path)` para ver deltas de TPS/MSPT, amenaza y SSIM.

## 4. Cliente Real (MC Pilot) y Perfilado JVM (JFR)
- **Preparación:** `prepare_mcpilot_client()` exige cliente detenido y puerto libre.
- **Capturas y spark:** `mcpilot_command(["--client", "mycelial-1211", "screenshot", "--output", "artifacts/diagnostics/shot.png"])` y `chat command "spark tps"` + `chat history --last 16`.
- **JFR:** `list_java_processes()` → identificar PID real de Minecraft (no daemon Gradle) → `run_jfr(process_id=PID, action="Start", duration_seconds=60)` → `summarize_jfr("artifacts/diagnostics/mycelial-....jfr")`.
