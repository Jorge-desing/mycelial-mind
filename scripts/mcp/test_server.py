"""Verification tests for the Mycelial Mind FastMCP CodeMode server."""

from __future__ import annotations

import asyncio
import json
import re
import socket
import struct
import sys
import threading
from pathlib import Path

from fastmcp import Client

MCP_DIR = Path(__file__).resolve().parent
if str(MCP_DIR) not in sys.path:
    sys.path.insert(0, str(MCP_DIR))

from server import (
    _annotate_gradle_result,
    _build_gradle_task_command,
    _parse_colony_status_output,
    _parse_cycle_status_output,
    _parse_hive_status_output,
    _parse_spark_tps_output,
    _pick_minecraft_jvm_pid,
    _resolve_repo_path,
    _resolve_temp_dir,
    _run_subprocess,
    _SKIP_DIAGNOSTIC_DIRS,
    mcp,
)

REPO_ROOT = MCP_DIR.parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.rcon_verify import recv_packet, send_packet


def _start_mock_rcon_server() -> tuple[socket.socket, int, threading.Thread]:
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.bind(("127.0.0.1", 0))
    srv.listen(8)
    port = srv.getsockname()[1]

    def _serve() -> None:
        srv.settimeout(60.0)
        for _ in range(16):
            try:
                conn, _ = srv.accept()
            except OSError:
                break
            with conn:
                while True:
                    hdr = conn.recv(4)
                    if not hdr:
                        break
                    length = struct.unpack("<i", hdr)[0]
                    body = conn.recv(length)
                    req_id, ptype = struct.unpack("<ii", body[:8])
                    payload = body[8:-2].decode("utf-8", errors="replace")
                    if ptype == 3:  # auth
                        resp_id = req_id if payload == "secret" else -1
                        resp = b""
                        pkt = struct.pack("<iii", len(resp) + 10, resp_id, 2) + resp + b"\x00\x00"
                        conn.sendall(pkt)
                    else:
                        if payload == "mycelial status":
                            resp_str = (
                                "[Mycelial] Threat: 42.5% [AWAKENED] | Milestone: 25% | Protected: false | "
                                "Traits: melee_thorns, fire_resistant | Tier2: flanking_swarm | "
                                "Synergies: pyrolytic_shell | Links: 3 | Suppressed: 0s | NearestNode: (12, 64, -8) | "
                                "Breached: true | MatriarchDefeated: false | Remission: 0s"
                            )
                        elif payload == "mycelial colony status":
                            resp_str = (
                                "[Colony] Nexus=10, 65, -20 | Tier=2 | Radius=36m | Pop=5/8 | Biomass=140 | "
                                "Morale=88% | DomainXP=420 | Traits=0x5 | Outposts=2 | Orders=3"
                            )
                        elif payload == "mycelial cycle status":
                            resp_str = (
                                "[Cycle] Unlocked=true Active=true Cycle=2 Mutators=[lethal_spores, fast_learning] "
                                "Completed=1 BestScore=1450 (Cycle 1, Last=1450 S)"
                            )
                        elif payload.startswith("mycelial verify "):
                            resp_str = "[Verify:HiveAI] PASSED"
                        else:
                            resp_str = f"ok:{payload}"
                        resp = resp_str.encode("utf-8")
                        pkt = struct.pack("<iii", len(resp) + 10, req_id, 0) + resp + b"\x00\x00"
                        conn.sendall(pkt)

    t = threading.Thread(target=_serve, daemon=True)
    t.start()
    return srv, port, t


async def verify_code_mode() -> None:
    # Verify path-traversal guard and _run_subprocess timeout handling
    try:
        _resolve_repo_path("../outside.txt")
        raise AssertionError("Expected ValueError for path traversal")
    except ValueError:
        pass

    assert Path(_resolve_temp_dir()).is_dir(), "Expected writable temp directory"
    assert ".tmp" in _SKIP_DIAGNOSTIC_DIRS and "region" in _SKIP_DIAGNOSTIC_DIRS, _SKIP_DIAGNOSTIC_DIRS

    publish_cmd = _build_gradle_task_command(["publishMods"], 2, False, False)
    assert "-PpublishDryRun=true" in publish_cmd, publish_cmd
    assert not any(arg in {"-PpublishDryRun=false", "-Dorg.gradle.project.publishDryRun=false"} for arg in publish_cmd)
    for unsafe_publish_args in (
        ["publishMods", "-PpublishDryRun=false"],
        ["publishMods", "-Dorg.gradle.project.publishDryRun=false"],
        ["publishMods", "--init-script=release.gradle"],
        ["build", "publishMods"],
    ):
        try:
            _build_gradle_task_command(unsafe_publish_args, 2, False, False)
            raise AssertionError(f"Expected publish safety guard for {unsafe_publish_args}")
        except ValueError:
            pass

    timeout_res = _run_subprocess([sys.executable, "-c", "import time; time.sleep(5)"], timeout_secs=0.35)
    assert timeout_res["ok"] is False and timeout_res["timed_out"] is True, timeout_res

    annotated_fail = _annotate_gradle_result(
        {"ok": False, "stdout_tail": "error: missing semicolon\n> Task :compileJava FAILED", "stderr_tail": ""}
    )
    assert len(annotated_fail.get("error_lines", [])) == 2, annotated_fail
    annotated_pass = _annotate_gradle_result(
        {"ok": True, "stdout_tail": "All 107 required tests passed", "stderr_tail": ""}
    )
    assert annotated_pass.get("summary_markers") == ["All 107 required tests passed"], annotated_pass

    spark_sample = (
        "TPS from last 5s, 10s, 1m, 5m, 15m:\n"
        " *20.0, *19.9, 19.8, 19.7, 19.6\n"
        "Tick durations (min/med/95%ile/max ms) from last 10s, 1m:\n"
        " 1.2/3.4/6.8/12.5; 1.1/3.2/6.5/14.0\n"
        "CPU usage from last 10s, 1m, 15m:\n"
        " 14.5% (process), 28.2% (system)\n"
    )
    spark_parsed = _parse_spark_tps_output(spark_sample)
    assert spark_parsed["ok"] is True
    assert spark_parsed["tps_10s"] == 19.9
    assert spark_parsed["mspt_p95"] == 6.8
    assert spark_parsed["cpu_process_pct"] == 14.5

    mcpilot_spark_payload = {
        "success": True,
        "data": {
            "data": {
                "messages": [
                    {"plain": "TPS from last 5s, 10s, 1m, 5m, 15m:"},
                    {"plain": "[⚡]  *20.0, *19.9, 19.8, 19.7, 19.6"},
                    {"plain": "Tick durations (min/med/95%ile/max ms) from last 10s, 1m:"},
                    {"plain": "[⚡]  4.1/9.4/36.7/62.2;  4.0/6.5/16.9/62.2"},
                    {"plain": "CPU usage from last 10s, 1m, 15m:"},
                    {"plain": "[⚡]  35%, 21%, 17%  (system)"},
                    {"plain": "[⚡]  1%, 1%, 1%  (process)"},
                ]
            }
        },
    }
    mcpilot_spark_parsed = _parse_spark_tps_output(json.dumps(mcpilot_spark_payload))
    assert mcpilot_spark_parsed["ok"] is True, mcpilot_spark_parsed
    assert mcpilot_spark_parsed["tps_5s"] == 20.0 and mcpilot_spark_parsed["tps_10s"] == 19.9
    assert mcpilot_spark_parsed["mspt_med"] == 9.4 and mcpilot_spark_parsed["mspt_p95"] == 36.7
    assert mcpilot_spark_parsed["cpu_process_pct"] == 1.0
    assert mcpilot_spark_parsed["cpu_system_pct"] == 35.0

    hive_en = _parse_hive_status_output(
        "[Mycelial] Threat: 42.5% [AWAKENED] | Milestone: 25% | Protected: false | "
        "Traits: melee_thorns, fire_resistant | Tier2: flanking_swarm | Synergies: pyrolytic_shell | "
        "Links: 3 | Suppressed: 0s | NearestNode: (12, 64, -8) | Breached: true | MatriarchDefeated: false | Remission: 0s"
    )
    assert hive_en["threat_pct"] == 42.5 and hive_en["tier"] == "AWAKENED", hive_en
    assert hive_en["milestone_pct"] == 25 and hive_en["protected"] is False, hive_en
    assert hive_en["tier2_traits"] == ["flanking_swarm"] and hive_en["links"] == 3, hive_en
    assert hive_en["suppressed_secs"] == 0 and hive_en["nearest_node"] == "(12, 64, -8)", hive_en
    assert hive_en["breached"] is True and hive_en["matriarch_defeated"] is False and hive_en["remission_secs"] == 0, hive_en

    hive_es = _parse_hive_status_output(
        "[Mycelial] Threat: 75,8% [OVERMIND] | Milestone: 75% | Protected: true | "
        "Traits: None | Tier2: None | Synergies: None | Links: 0 | Suppressed: 45s | NearestNode: None | "
        "Breached: true | MatriarchDefeated: true | Remission: 1200s"
    )
    assert hive_es["threat_pct"] == 75.8 and hive_es["tier"] == "OVERMIND", hive_es
    assert hive_es["protected"] is True and hive_es["traits"] == [] and hive_es["nearest_node"] is None, hive_es
    assert hive_es["breached"] is True and hive_es["matriarch_defeated"] is True and hive_es["remission_secs"] == 1200, hive_es

    colony_active = _parse_colony_status_output(
        "[Colony] Nexus=10, 65, -20 | Tier=2 | Radius=36m | Pop=5/8 | Biomass=140 | "
        "Morale=88% | DomainXP=420 | Traits=0x5 | Outposts=2 | Orders=3"
    )
    assert colony_active["found"] is True and colony_active["tier"] == 2 and colony_active["radius_m"] == 36, colony_active
    assert colony_active["population"] == 5 and colony_active["population_cap"] == 8, colony_active
    assert colony_active["trait_mask_hex"] == "0x5" and colony_active["outposts"] == 2 and colony_active["orders"] == 3, colony_active

    colony_empty = _parse_colony_status_output("[Colony] No Sovereign Colony found within 256 blocks. Total in dim: 1")
    assert colony_empty["found"] is False and colony_empty["total_in_dim"] == 1, colony_empty

    cycle_parsed = _parse_cycle_status_output(
        "[Cycle] Unlocked=true Active=true Cycle=2 Mutators=[lethal_spores, fast_learning] "
        "Completed=1 BestScore=1450 (Cycle 1, Last=1450 S)"
    )
    assert cycle_parsed["unlocked"] is True and cycle_parsed["active"] is True and cycle_parsed["cycle_number"] == 2, cycle_parsed
    assert cycle_parsed["mutators"] == ["lethal_spores", "fast_learning"] and cycle_parsed["last_grade"] == "S", cycle_parsed

    iso_cmd = _build_gradle_task_command(["runGameTestServer"], 2, False, True, isolated_gametests=True)
    assert "-PisolatedGameTests=true" in iso_cmd and "--offline" in iso_cmd, iso_cmd
    pub_cmd = _build_gradle_task_command(["publishMods"], 2, False, True, isolated_gametests=True)
    assert "-PpublishDryRun=true" in pub_cmd and "-PisolatedGameTests=true" not in pub_cmd, pub_cmd
    flt_cmd = _build_gradle_task_command(["test"], 2, False, True, test_filter="OpenAlEfxLoopbackTest")
    assert "--tests" in flt_cmd and "OpenAlEfxLoopbackTest" in flt_cmd, flt_cmd
    vis_cmd = _build_gradle_task_command(["runVisualSmoke"], 2, False, True, visual_smoke_mode="dialect")
    assert "-PdialectVisualSmoke=true" in vis_cmd, vis_cmd
    for bad_kwargs in (
        {"tasks": ["publishMods"], "test_filter": "OpenAlEfxLoopbackTest"},
        {"tasks": ["test"], "test_filter": "bad;cmd"},
        {"tasks": ["runVisualSmoke"], "visual_smoke_mode": "invalid_mode"},
    ):
        try:
            _build_gradle_task_command(max_workers=2, no_daemon=False, use_offline=True, **bad_kwargs)
            raise AssertionError(f"Expected ValueError for {bad_kwargs}")
        except ValueError:
            pass

    picked_pid = _pick_minecraft_jvm_pid([
        {"pid": 100, "main_class": "org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.10"},
        {"pid": 200, "main_class": "jdk.jcmd/sun.tools.jcmd.JCmd -l"},
        {"pid": 300, "main_class": "cpw.mods.bootstraplauncher.BootstrapLauncher --launchTarget neoforge"},
    ])
    assert picked_pid == 300, f"Expected BootstrapLauncher PID 300, got {picked_pid}"
    assert _pick_minecraft_jvm_pid([{"pid": 101, "main_class": "org.gradle.launcher.daemon.bootstrap.GradleDaemon"}]) is None

    # Verify rcon_verify.recv_packet handles 1-byte TCP fragmentation and rejects invalid lengths
    s_srv, s_cli = socket.socketpair() if hasattr(socket, "socketpair") else (None, None)
    if s_srv is not None and s_cli is not None:
        with s_srv, s_cli:
            payload_bytes = b"pong\x00\x00"
            raw_pkt = struct.pack("<iii", len(payload_bytes) + 8, 7, 0) + payload_bytes
            s_cli.sendall(raw_pkt[:3])
            s_cli.sendall(raw_pkt[3:])
            rid, ptype, body = recv_packet(s_srv)
            assert (rid, ptype, body) == (7, 0, "pong"), (rid, ptype, body)

    srv, rcon_port, thread = _start_mock_rcon_server()
    try:
        async with Client(mcp) as client:
            tools = await client.list_tools()
            tool_names = sorted(t.name for t in tools)
            print("Exposed CodeMode meta-tools:", tool_names)

            assert "execute" in tool_names, f"Missing 'execute' meta-tool in {tool_names}"
            assert "search" in tool_names, f"Missing 'search' meta-tool in {tool_names}"
            assert "get_schema" in tool_names, f"Missing 'get_schema' meta-tool in {tool_names}"
            assert "get_registered_content" not in tool_names, (
                "Raw tool should be hidden behind CodeMode"
            )

            # 1. Test on-demand discovery via search & get_schema
            search_res = await client.call_tool(
                "search", {"query": "registered content skills jfr mcpilot diagnostics"}
            )
            assert "get_registered_content" in str(search_res)
            schema_res = await client.call_tool(
                "get_schema", {"tools": ["run_model_studio", "rcon_command"]}
            )
            assert "run_model_studio" in str(schema_res)

            # 2. Test chained + parallel execution inside MontySandboxProvider (full suite)
            code = f"""
import asyncio
(
    health,
    skills,
    model_chk,
    tex_chk,
    particle_chk,
    struct_studio,
    vis,
    jvms,
    rcon_closed,
    vcache,
    reg,
    asset_audit,
) = await asyncio.gather(
    call_tool("get_bridge_health", {{}}),
    call_tool("validate_skills", {{}}),
    call_tool("run_model_studio", {{"check_only": True, "run_tests": True}}),
    call_tool("run_texture_studio", {{"check_only": True, "pbr": True, "run_tests": True}}),
    call_tool("run_particle_studio", {{"check_only": True, "export_extras": False, "run_tests": True}}),
    call_tool("run_structure_studio", {{}}),
    call_tool("check_visual_regression", {{"include_diff_regions": True}}),
    call_tool("list_java_processes", {{}}),
    call_tool("rcon_command", {{"command": "ping", "port": 1, "timeout_secs": 0.5}}),
    call_tool("manage_verify_cache", {{"action": "status", "scope": "all"}}),
    call_tool("get_registered_content", {{}}),
    call_tool("audit_mod_assets", {{}}),
)
(
    all_skills,
    ai_guide,
    cm_guide,
    diag_guide,
    gametests,
    unit_tests,
    junit_rpt,
    pitest_rpt,
    struct_vis,
    uv,
    uv_behemoth,
    hive_ai,
    datapack,
    cfg,
    sources,
    hits,
    head,
    artifacts,
    rcon_ok,
    rcon_batch,
    live_hive,
    live_verify,
    tags_info,
    smoke_stat,
    ref_cat,
    ref_life,
    ref_play,
    bad_regex,
    bad_log_regex,
    missing_file,
    worldgen,
) = await asyncio.gather(
    call_tool("list_skills", {{}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-ai-hive"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-codemode"}}),
    call_tool("get_diagnostics_guide", {{"topic": "overview"}}),
    call_tool("list_gametests", {{}}),
    call_tool("list_unit_tests", {{}}),
    call_tool("get_latest_junit_report", {{}}),
    call_tool("get_latest_pitest_report", {{}}),
    call_tool("get_latest_structure_visual_smoke", {{}}),
    call_tool("get_uv_manifest", {{}}),
    call_tool("get_uv_manifest", {{"model_id": "spore_behemoth"}}),
    call_tool("get_hive_ai_overview", {{}}),
    call_tool("list_datapack_content", {{}}),
    call_tool("get_gradle_config", {{}}),
    call_tool("list_mod_sources", {{"subdir": "hive", "scope": "java"}}),
    call_tool("search_mod_code", {{"pattern": "HiveMindSavedData", "max_results": 5}}),
    call_tool("read_mod_file", {{"relative_path": "gradle.properties", "start_line": 1, "end_line": 5}}),
    call_tool("list_diagnostic_artifacts", {{"max_results": 10}}),
    call_tool("rcon_command", {{"command": "mycelial status", "port": {rcon_port}, "password": "secret"}}),
    call_tool("run_rcon_batch", {{"commands": ["mycelial status", "mycelial habits"], "port": {rcon_port}, "password": "secret"}}),
    call_tool("get_live_hive_status", {{"port": {rcon_port}, "password": "secret"}}),
    call_tool("run_live_verify_scenario", {{"scenario": "hive_ai", "port": {rcon_port}, "password": "secret"}}),
    call_tool("list_tool_tags", {{}}),
    call_tool("smoke_server_status", {{}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-codemode", "doc": "references/catalog-and-sandbox.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-codemode", "doc": "references/live-game-lifecycle.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-codemode", "doc": "references/audit-playbooks.md"}}),
    call_tool("search_mod_code", {{"pattern": "[unclosed"}}),
    call_tool("read_diagnostic_logs", {{"filter_pattern": "[unclosed"}}),
    call_tool("read_mod_file", {{"relative_path": "no_such_file_12345.txt"}}),
    call_tool("get_latest_worldgen_audit", {{}}),
)
reg_cached = await call_tool("get_registered_content", {{}})
metrics = await call_tool("get_bridge_metrics", {{}})
cleared = await call_tool("clear_bridge_cache", {{}})
await call_tool("capture_audit_snapshot", {{"label": "before"}})
await call_tool("capture_audit_snapshot", {{"label": "after"}})
snap_diff = await call_tool("diff_audit_snapshots", {{"before_label": "before", "after_label": "after"}})
return {{
    "health_ok": health.get("ok") and health.get("jcmd_available") and isinstance(health.get("temp_dir"), str) and bool(health.get("temp_dir")),
    "fastmcp_version": health.get("fastmcp_version"),
    "registries": {{k: len(v) for k, v in reg.items()}},
    "data_components": reg.get("ModDataComponents", []),
    "skills_ok": skills["ok"] and skills.get("exit_code") == 0,
    "skills_listed": all_skills["count"],
    "ai_guide_ok": ai_guide["ok"],
    "codemode_guide_ok": cm_guide["ok"],
    "diag_guide_ok": diag_guide["ok"],
    "gametests_count": gametests["count"],
    "gametest_sample": gametests["tests"][0] if gametests["tests"] else None,
    "unit_tests_count": unit_tests["count"],
    "junit_checked": "exists" in junit_rpt,
    "pitest_checked": "exists" in pitest_rpt,
    "struct_vis_checked": "exists" in struct_vis,
    "visual_diff_regions": vis.get("diff_regions_count"),
    "uv_models": uv.get("count", 0),
    "uv_behemoth_ok": uv_behemoth.get("ok"),
    "hive_enums": {{k: len(v) for k, v in hive_ai.get("enums", {{}}).items()}},
    "hive_brain_core_enums": {{k: len(v) for k, v in hive_ai.get("brain_core_enums", {{}}).items()}},
    "hive_brain_files_count": hive_ai.get("brain_files_count", 0),
    "hive_mem_types": len(hive_ai.get("memory_types", [])),
    "asset_audit_ok": asset_audit.get("ok"),
    "asset_lang": asset_audit.get("lang"),
    "model_atlas_count": asset_audit.get("texture_counts", {{}}).get("model_atlas", 0),
    "model_json_counts": asset_audit.get("model_json_counts"),
    "blockstates_count": asset_audit.get("blockstates_count", 0),
    "sounds_json_count": asset_audit.get("sounds_json_count", 0),
    "ogg_files_count": asset_audit.get("ogg_files_count", 0),
    "missing_sound_files": asset_audit.get("missing_sound_files", []),
    "vanilla_event_redirects": asset_audit.get("vanilla_event_redirects", 0),
    "audio_quality_ok": asset_audit.get("audio_quality_ok", False),
    "entities_without_uv": asset_audit.get("entities_without_uv"),
    "particle_count": asset_audit.get("particle_count", 0),
    "particles_json_count": asset_audit.get("particles_json_count", 0),
    "particle_texture_count": asset_audit.get("particle_texture_count", 0),
    "missing_particle_assets": asset_audit.get("missing_particle_assets", []),
    "vanilla_particle_hits": asset_audit.get("vanilla_particle_hits", 0),
    "datapack_total": datapack.get("total"),
    "datapack_external_total": datapack.get("external_total", 0),
    "mod_version": cfg.get("properties", {{}}).get("mod_version"),
    "hive_sources": sources["count"],
    "search_hits": hits["count"],
    "gradle_props_lines": len(head["content"].splitlines()),
    "visual_ok": vis.get("ok"),
    "model_chk_ok": model_chk.get("ok"),
    "tex_chk_ok": tex_chk.get("ok"),
    "particle_chk_ok": particle_chk.get("ok"),
    "verify_cache_ok": vcache.get("ok") is True and "all_cached" in vcache,
    "artifacts_count": artifacts["count"],
    "artifacts_returned": artifacts.get("returned_count"),
    "jvms_ok": jvms["ok"],
    "rcon_ok": rcon_ok,
    "rcon_batch_ok": rcon_batch.get("ok") and rcon_batch.get("count") == 2,
    "live_hive": live_hive,
    "live_verify_ok": live_verify.get("ok") and live_verify.get("passed"),
    "rcon_closed_ok": rcon_closed["ok"],
    "struct_studio_ok": struct_studio.get("ok"),
    "tags_ok": tags_info.get("ok") and tags_info.get("tagged_tools", 0) >= 50,
    "tagged_tools_count": tags_info.get("tagged_tools", 0),
    "metrics_tracked_tools": len(metrics.get("tools", {{}})),
    "cache_hits": metrics.get("cache", {{}}).get("hits", 0),
    "cleared_ok": cleared.get("ok"),
    "snap_diff_ok": snap_diff.get("ok") and snap_diff.get("changed") is False,
    "smoke_stat_ok": smoke_stat.get("ok") and smoke_stat.get("count") == 0,
    "codemode_refs_ok": ref_cat.get("ok") and ref_life.get("ok") and ref_play.get("ok"),
    "bad_regex_ok": bad_regex["ok"],
    "bad_log_regex_ok": bad_log_regex["ok"],
    "missing_file_ok": missing_file["ok"],
    "worldgen_checked": "exists" in worldgen,
}}
"""
            exec_res = await client.call_tool("execute", {"code": code})
            payload = (
                exec_res.data
                if hasattr(exec_res, "data") and exec_res.data is not None
                else json.loads(exec_res.content[0].text)
            )
            # 3. Second execute() block for live scenario orchestration & report comparison (stays under 50 calls/execute)
            tmp_dir = REPO_ROOT / "artifacts" / "diagnostics" / "live-reports" / "_test_tmp"
            tmp_dir.mkdir(parents=True, exist_ok=True)
            before_json = tmp_dir / "before.json"
            after_json = tmp_dir / "after.json"
            before_png = tmp_dir / "before.png"
            after_png = tmp_dir / "after.png"
            try:
                from PIL import Image

                Image.new("RGB", (16, 16), (10, 20, 30)).save(before_png)
                Image.new("RGB", (16, 16), (200, 180, 40)).save(after_png)
                before_json.write_text(
                    json.dumps({
                        "ok": True,
                        "duration_secs": 4.2,
                        "failed_steps": [],
                        "hive_status": {"threat_pct": 25.0},
                        "perf": {"tps_10s": 19.5, "mspt_med": 4.0, "mspt_p95": 8.0},
                        "screenshot": {"path": before_png.relative_to(REPO_ROOT).as_posix()},
                    }),
                    encoding="utf-8",
                )
                after_json.write_text(
                    json.dumps({
                        "ok": True,
                        "duration_secs": 3.0,
                        "failed_steps": [],
                        "hive_status": {"threat_pct": 60.0},
                        "perf": {"tps_10s": 20.0, "mspt_med": 3.0, "mspt_p95": 6.5},
                        "screenshot": {"path": after_png.relative_to(REPO_ROOT).as_posix()},
                    }),
                    encoding="utf-8",
                )
                code_live = f"""
import asyncio
(
    bad_sess,
    custom_scen,
    live_scen,
    live_cmp,
    diag_cm_guide,
    diag_tps,
    diag_fuzzy,
    diag_unknown,
    sound_chk,
    codex_chk,
) = await asyncio.gather(
    call_tool("run_live_scenario", {{"preset": "progression_ramp", "session_id": "nonexistent"}}),
    call_tool("run_live_scenario", {{"preset": "custom", "custom_commands": ["mycelial status"], "rcon_port": {rcon_port}, "rcon_password": "secret", "capture_perf": True}}),
    call_tool("run_live_scenario", {{"preset": "progression_ramp", "rcon_port": {rcon_port}, "rcon_password": "secret"}}),
    call_tool("compare_live_reports", {{"before_path": "{before_json.relative_to(REPO_ROOT).as_posix()}", "after_path": "{after_json.relative_to(REPO_ROOT).as_posix()}"}}),
    call_tool("get_diagnostics_guide", {{"topic": "codemode"}}),
    call_tool("diagnose_symptom", {{"symptom": "tps_lag", "include_live_state": True}}),
    call_tool("diagnose_symptom", {{"symptom": "estructuras", "include_live_state": False}}),
    call_tool("diagnose_symptom", {{"symptom": "unknown_symptom_xyz", "include_live_state": False}}),
    call_tool("run_sound_studio", {{"check_only": True, "run_tests": True}}),
    call_tool("run_codex_studio", {{"check_only": True, "run_tests": True}}),
)
latest_live, diag_bundle, missing_artifacts, sound_inspect, sound_masking, live_colony, diag_colony, unit_efx, diag_audio, ref_matrix, ai_ref_brain, ai_ref_combat, ai_ref_colony, ai_ref_eco, ai_ref_recipes = await asyncio.gather(
    call_tool("get_latest_live_report", {{}}),
    call_tool("collect_diagnostic_bundle", {{"log_tail_lines": 20, "max_artifacts": 5}}),
    call_tool("list_diagnostic_artifacts", {{"subdir": "nonexistent_subdir_12345"}}),
    call_tool("inspect_sound_event", {{"sound_id": "boss_stagger_break"}}),
    call_tool("audit_sound_masking", {{"top_n": 8}}),
    call_tool("get_live_colony_status", {{"port": {rcon_port}, "password": "secret"}}),
    call_tool("diagnose_symptom", {{"symptom": "dialecto", "include_live_state": False}}),
    call_tool("list_unit_tests", {{"suite_filter": "OpenAlEfxLoopbackTest"}}),
    call_tool("diagnose_symptom", {{"symptom": "openal", "include_live_state": False}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-codemode", "doc": "references/test-matrix.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-ai-hive", "doc": "references/hive-brain-engine.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-ai-hive", "doc": "references/hive-mind-and-combat.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-ai-hive", "doc": "references/sovereign-colony-and-endgame.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-ai-hive", "doc": "references/world-ecosystems-and-integration.md"}}),
    call_tool("get_skill_guide", {{"skill_name": "mycelial-ai-hive", "doc": "references/recipes-and-pitfalls.md"}}),
)
return {{
    "bad_sess_rejected": bad_sess.get("ok") is False and "Unknown session_id" in bad_sess.get("error", ""),
    "custom_scen_ok": custom_scen.get("ok") and custom_scen.get("commands_run") == 1 and (custom_scen.get("perf") or {{}}).get("source") == "jfr_fallback",
    "live_scenario_ok": live_scen.get("ok") and live_scen.get("commands_run") == 7,
    "latest_live_ok": latest_live.get("exists") and latest_live.get("ok"),
    "live_cmp_ok": (
        live_cmp.get("ok")
        and live_cmp.get("deltas", {{}}).get("threat_pct_delta") == 35.0
        and live_cmp.get("deltas", {{}}).get("tps_10s_delta") == 0.5
        and "screenshot_ssim" in live_cmp.get("deltas", {{}})
    ),
    "diag_codemode_guide_ok": diag_cm_guide.get("ok") and "Playbook 4" in diag_cm_guide.get("content", ""),
    "diag_tps_ok": diag_tps.get("ok") and diag_tps.get("symptom") == "tps_lag" and "live_state" in diag_tps,
    "diag_fuzzy_ok": diag_fuzzy.get("ok") and diag_fuzzy.get("symptom") == "worldgen_structures",
    "diag_unknown_rejected": diag_unknown.get("ok") is False and "tps_lag" in diag_unknown.get("available_symptoms", []),
    "diag_bundle_ok": diag_bundle.get("ok") and "java_processes" in diag_bundle and "pitest" in diag_bundle.get("reports", {{}}),
    "missing_artifacts_ok": (
        missing_artifacts.get("exists") is False
        and missing_artifacts.get("count") == 0
        and missing_artifacts.get("returned_count") == 0
    ),
    "sound_studio_ok": sound_chk.get("ok"),
    "sound_inspect_ok": (
        sound_inspect.get("ok")
        and sound_inspect.get("sound_id") == "boss_stagger_break"
        and "psychoacoustic_v4" in sound_inspect
    ),
    "sound_masking_ok": (
        sound_masking.get("ok") is True
        and sound_masking.get("beds_checked") == 11
        and sound_masking.get("bark_unmasking_applied") is True
        and sound_masking.get("high_risk_count") == 0
    ),
    "codex_studio_ok": codex_chk.get("ok"),
    "live_colony_ok": (
        live_colony.get("ok") is True
        and (live_colony.get("colony") or {{}}).get("tier") == 2
        and (live_colony.get("cycle") or {{}}).get("last_grade") == "S"
    ),
    "diag_colony_ok": diag_colony.get("ok") is True and diag_colony.get("symptom") == "colony_dialects",
    "unit_efx_ok": unit_efx.get("ok") is True and unit_efx.get("count", 0) >= 6,
    "diag_audio_ok": diag_audio.get("ok") is True and diag_audio.get("symptom") == "audio_efx",
    "ref_matrix_ok": ref_matrix.get("ok") is True and "OpenAlEfxLoopbackTest" in ref_matrix.get("content", ""),
    "ai_refs_ok": (
        ai_ref_brain.get("ok") is True and "HiveBrainOwner" in ai_ref_brain.get("content", "")
        and ai_ref_combat.get("ok") is True and "HiveMindSavedData" in ai_ref_combat.get("content", "")
        and ai_ref_colony.get("ok") is True and "ColonySymbiontEntity" in ai_ref_colony.get("content", "")
        and ai_ref_eco.get("ok") is True and "HyphalVeinEntity" in ai_ref_eco.get("content", "")
        and ai_ref_recipes.get("ok") is True and "/mycelial ai" in ai_ref_recipes.get("content", "")
    ),
}}
"""
                exec_live = await client.call_tool("execute", {"code": code_live})
                payload_live = (
                    exec_live.data
                    if hasattr(exec_live, "data") and exec_live.data is not None
                    else json.loads(exec_live.content[0].text)
                )
            finally:
                import shutil

                shutil.rmtree(tmp_dir, ignore_errors=True)
            payload.update(payload_live)
            print("CodeMode execute result:", json.dumps(payload, indent=2))
            assert payload["health_ok"] is True
            assert payload["skills_ok"] is True
            assert payload["skills_listed"] >= 7
            assert payload["ai_guide_ok"] is True
            assert payload["codemode_guide_ok"] is True
            assert payload["diag_guide_ok"] is True
            assert payload["gametests_count"] >= 195, f"Expected >= 195 GameTests, got {payload['gametests_count']}"
            assert payload["gametest_sample"] and "batch" in payload["gametest_sample"]
            assert payload["unit_tests_count"] >= 140, f"Expected >= 140 JUnit tests, got {payload['unit_tests_count']}"
            assert payload["junit_checked"] is True
            assert payload["pitest_checked"] is True
            assert payload["model_atlas_count"] >= 7
            assert payload["model_json_counts"]["block"] > 0 and payload["model_json_counts"]["item"] > 0
            assert payload["struct_vis_checked"] is True
            assert isinstance(payload["visual_diff_regions"], int)
            assert payload["uv_models"] >= 17
            assert payload["uv_behemoth_ok"] is True
            assert payload["hive_enums"].get("StimulusType") == 5
            assert payload["hive_enums"].get("AdaptationTrait") == 5
            assert payload["hive_enums"].get("HiveDoctrine") == 5
            assert payload["hive_enums"].get("HiveProgressionTier") == 4
            assert payload["hive_enums"].get("HiveStrain") == 3
            assert payload["hive_enums"].get("PlayerHabit", 0) >= 3
            assert payload["hive_enums"].get("SwarmRole", 0) >= 3
            assert payload["hive_brain_core_enums"].get("HiveResourceChannel") == 5
            assert payload["hive_brain_core_enums"].get("SignalType") == 8
            assert payload["hive_brain_core_enums"].get("EventKind") == 7
            assert payload["hive_brain_files_count"] >= 60
            assert payload["hive_mem_types"] >= 10
            assert payload["registries"]["ModMemoryTypes"] == payload["hive_mem_types"]
            assert payload["asset_audit_ok"] is True, f"Asset audit failed: {payload}"
            assert payload["asset_lang"].get("referenced_keys_count", 0) >= 100
            assert payload["asset_lang"].get("missing_referenced_keys") == []
            assert payload["blockstates_count"] >= 20
            assert payload["sounds_json_count"] == 226
            assert payload["ogg_files_count"] == 596
            assert payload["missing_sound_files"] == []
            assert payload["vanilla_event_redirects"] == 0
            assert payload["audio_quality_ok"] is True
            assert payload["sound_studio_ok"] is True
            assert payload["sound_inspect_ok"] is True
            assert payload["sound_masking_ok"] is True
            assert payload["codex_studio_ok"] is True
            assert payload["live_colony_ok"] is True
            assert payload["diag_colony_ok"] is True
            assert payload["unit_efx_ok"] is True
            assert payload["diag_audio_ok"] is True
            assert payload["ai_refs_ok"] is True
            assert payload["entities_without_uv"] == []
            assert payload["datapack_total"] > 0
            assert payload["datapack_external_total"] >= 15
            assert payload["artifacts_returned"] <= 10
            assert isinstance(payload["mod_version"], str) and re.fullmatch(
                r"\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?", payload["mod_version"]
            )
            assert payload["hive_sources"] > 0
            assert payload["search_hits"] > 0
            assert payload["gradle_props_lines"] == 5
            assert payload["visual_ok"] is True
            assert payload["model_chk_ok"] is True
            assert payload["tex_chk_ok"] is True
            assert payload["particle_chk_ok"] is True
            assert payload["vanilla_particle_hits"] == 0, payload["vanilla_particle_hits"]
            assert payload["missing_particle_assets"] == [], payload["missing_particle_assets"]
            assert payload["verify_cache_ok"] is True
            assert payload["jvms_ok"] is True
            assert {"armor_rank", "engram_id", "staff_mode", "trait_id"}.issubset(set(payload["data_components"])), (
                f"Expected ModDataComponents to contain ['armor_rank', 'engram_id', 'staff_mode', 'trait_id'], got {payload['data_components']}"
            )
            assert payload["rcon_ok"]["ok"] is True
            assert "Threat: 42.5%" in payload["rcon_ok"]["response"]
            assert payload["rcon_batch_ok"] is True
            assert payload["live_hive"]["threat_pct"] == 42.5
            assert payload["live_hive"]["tier"] == "AWAKENED"
            assert payload["live_hive"]["milestone_pct"] == 25
            assert payload["live_hive"]["protected"] is False
            assert payload["live_hive"]["traits"] == ["melee_thorns", "fire_resistant"]
            assert payload["live_hive"]["tier2_traits"] == ["flanking_swarm"]
            assert payload["live_hive"]["synergies"] == ["pyrolytic_shell"]
            assert payload["live_hive"]["links"] == 3
            assert payload["live_hive"]["suppressed_secs"] == 0
            assert payload["live_hive"]["nearest_node"] == "(12, 64, -8)"
            assert payload["live_hive"]["breached"] is True
            assert payload["live_hive"]["matriarch_defeated"] is False
            assert payload["live_hive"]["remission_secs"] == 0
            assert payload["live_verify_ok"] is True
            assert payload["bad_sess_rejected"] is True
            assert payload["custom_scen_ok"] is True
            assert payload["live_scenario_ok"] is True
            assert payload["latest_live_ok"] is True
            assert payload["live_cmp_ok"] is True
            assert payload["diag_codemode_guide_ok"] is True
            assert payload["diag_tps_ok"] is True
            assert payload["diag_fuzzy_ok"] is True
            assert payload["diag_unknown_rejected"] is True
            assert payload["diag_bundle_ok"] is True
            assert payload["missing_artifacts_ok"] is True
            assert payload["rcon_closed_ok"] is False
            assert payload["struct_studio_ok"] is True
            assert payload["tags_ok"] is True
            assert payload["tagged_tools_count"] >= 67
            assert payload["metrics_tracked_tools"] >= 20
            assert payload["cache_hits"] >= 1
            assert payload["cleared_ok"] is True
            assert payload["snap_diff_ok"] is True
            assert payload["smoke_stat_ok"] is True
            assert payload["codemode_refs_ok"] is True
            assert payload["ref_matrix_ok"] is True
            assert payload["bad_regex_ok"] is False
            assert payload["bad_log_regex_ok"] is False
            assert payload["missing_file_ok"] is False
            assert payload["worldgen_checked"] is True
            print("ALL CodeMode checks PASSED.")
    finally:
        srv.close()
        thread.join(timeout=2)


if __name__ == "__main__":
    asyncio.run(verify_code_mode())
