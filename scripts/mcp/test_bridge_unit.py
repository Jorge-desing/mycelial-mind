"""Unit regression coverage for mycelial-mind-dev bridge tools and helpers."""

from __future__ import annotations

import asyncio
import json
import os
import sys
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, patch

MCP_DIR = Path(__file__).resolve().parent
if str(MCP_DIR) not in sys.path:
    sys.path.insert(0, str(MCP_DIR))

import server


class BridgeUpgradeTests(unittest.TestCase):
    def setUp(self) -> None:
        server._CACHE.clear()
        server._AUDIT_SNAPSHOTS.clear()
        server._GRADLE_LOCK_PATH.unlink(missing_ok=True)

    def tearDown(self) -> None:
        server._GRADLE_LOCK_PATH.unlink(missing_ok=True)

    def test_bridge_health_reports_70_tools_and_reload_flags(self) -> None:
        health = server.get_bridge_health()
        self.assertTrue(health["ok"])
        self.assertEqual(health["tool_count"], 70)
        self.assertIn("server_file_modified", health)
        self.assertIn("restart_recommended", health)
        self.assertEqual(health["server_file_modified"], health["restart_recommended"])

    def test_get_mixin_overview_matches_repo_config(self) -> None:
        mx = server.get_mixin_overview()
        self.assertTrue(mx["ok"], mx)
        self.assertGreaterEqual(mx["common_count"], 6)
        self.assertGreaterEqual(mx["client_count"], 10)
        self.assertEqual(mx["total_count"], mx["common_count"] + mx["client_count"])
        self.assertEqual(mx["missing_files"], [])
        self.assertEqual(mx["unlisted_files"], [])
        self.assertTrue(mx["mods_toml_enabled"])
        self.assertGreaterEqual(mx["annotation_counts"].get("@Mixin", 0), 10)

    def test_animation_pair_validation(self) -> None:
        cmd = server._build_gradle_task_command(
            ["runAnimationVisualSmoke"],
            2,
            False,
            True,
            animation_pair="brute_warden",
        )
        self.assertIn("-PanimationPair=brute_warden", cmd)

        for bad_kwargs in (
            {"tasks": ["test"], "animation_pair": "brute_warden"},
            {"tasks": ["publishMods"], "animation_pair": "brute_warden"},
            {"tasks": ["runAnimationVisualSmoke"], "animation_pair": "bad;pair"},
        ):
            with self.assertRaises(ValueError):
                server._build_gradle_task_command(
                    max_workers=2,
                    no_daemon=False,
                    use_offline=True,
                    **bad_kwargs,
                )

    def test_diagnose_symptom_resolves_mixins_client(self) -> None:
        diag = server.diagnose_symptom("mixin", include_live_state=False)
        self.assertTrue(diag["ok"], diag)
        self.assertEqual(diag["symptom"], "mixins_client")
        self.assertIn("get_mixin_overview", diag["tools"])

    def test_audit_snapshot_and_diff_include_mixins(self) -> None:
        before = server.capture_audit_snapshot("before")["snapshot"]
        self.assertIn("mixins_ok", before)
        self.assertIn("mixins_common_count", before)
        self.assertIn("mixins_client_count", before)
        server._AUDIT_SNAPSHOTS["after"] = {
            **before,
            "mixins_client_count": before["mixins_client_count"] + 1,
        }
        diff = server.diff_audit_snapshots("before", "after")
        self.assertTrue(diff["ok"])
        self.assertEqual(diff["deltas"]["mixins_client_count"]["delta"], 1)

    def test_resource_mixins_overview_returns_valid_json(self) -> None:
        payload = json.loads(server.resource_mixins_overview())
        self.assertTrue(payload["ok"])
        self.assertGreaterEqual(payload["client_count"], 10)


class BridgeAsyncAndLockTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self) -> None:
        server._CACHE.clear()
        server._GRADLE_LOCK_PATH.unlink(missing_ok=True)

    def tearDown(self) -> None:
        server._GRADLE_LOCK_PATH.unlink(missing_ok=True)

    async def test_validate_skills_reloads_module(self) -> None:
        server._CACHE.clear()
        with patch.object(server.importlib, "reload", wraps=server.importlib.reload) as reload_spy:
            res = await server.validate_skills()
        self.assertTrue(res["ok"], res)
        self.assertGreaterEqual(reload_spy.call_count, 1)

    async def test_get_repo_status_reads_git_worktree_async(self) -> None:
        repo = await server.get_repo_status(include_worktrees=True, max_dirty_files=10)
        self.assertTrue(repo["ok"], repo)
        self.assertTrue(repo["branch"])
        self.assertTrue(repo["head_sha"])
        self.assertGreaterEqual(repo["worktrees_count"], 1)
        self.assertLessEqual(len(repo["dirty_files"]), 10)

    async def test_resource_repo_status_returns_valid_json(self) -> None:
        payload = json.loads(await server.resource_repo_status())
        self.assertTrue(payload["ok"])
        self.assertTrue(payload["head_sha"])

    async def test_run_bridge_self_tests_parses_unittest_output_async(self) -> None:
        with patch.object(
            server,
            "_run_subprocess_async",
            new=AsyncMock(
                side_effect=[
                    {"ok": True, "exit_code": 0, "timed_out": False, "duration_secs": 0.1, "stdout_tail": "", "stderr_tail": ""},
                    {
                        "ok": True,
                        "exit_code": 0,
                        "timed_out": False,
                        "duration_secs": 0.4,
                        "stdout_tail": "",
                        "stderr_tail": "......\nRan 24 tests in 0.123s\n\nOK",
                    },
                ]
            ),
        ) as mock_run:
            res = await server.run_bridge_self_tests(include_py_compile=True)
        self.assertTrue(res["ok"], res)
        self.assertTrue(res["py_compile_ok"])
        self.assertTrue(res["unittest_ok"])
        self.assertEqual(res["tests_run"], 24)
        self.assertEqual(mock_run.call_count, 2)

    async def test_gradle_lock_serializes_in_process_calls(self) -> None:
        active = 0
        max_active = 0

        async def fake_subproc(*args, **kwargs):
            nonlocal active, max_active
            active += 1
            max_active = max(max_active, active)
            await asyncio.sleep(0.03)
            active -= 1
            return {"ok": True, "exit_code": 0, "timed_out": False, "duration_secs": 0.03, "stdout_tail": "", "stderr_tail": ""}

        with patch.object(server, "_run_subprocess_async", side_effect=fake_subproc):
            r1, r2 = await asyncio.gather(
                server.run_gradle_task(["test"], test_filter="HyphalPhysicsTest"),
                server.run_gradle_task(["test"], test_filter="ModCompatTest"),
            )
        self.assertTrue(r1["ok"] and r2["ok"])
        self.assertEqual(max_active, 1)
        self.assertFalse(server._GRADLE_LOCK_PATH.exists())

    async def test_gradle_lock_cleans_stale_dead_pid_lock(self) -> None:
        server._GRADLE_LOCK_PATH.parent.mkdir(parents=True, exist_ok=True)
        server._GRADLE_LOCK_PATH.write_text(
            json.dumps({"pid": 9999999, "instance_id": "dead", "operation": "old"}),
            encoding="utf-8",
        )
        with (
            patch.object(server, "_is_pid_alive", return_value=False),
            patch.object(
                server,
                "_run_subprocess_async",
                new=AsyncMock(
                    return_value={"ok": True, "exit_code": 0, "timed_out": False, "duration_secs": 0.01, "stdout_tail": "", "stderr_tail": ""}
                ),
            ),
        ):
            res = await server.run_gradle_task(["compileJava"])
        self.assertTrue(res["ok"], res)
        self.assertFalse(server._GRADLE_LOCK_PATH.exists())

    async def test_gradle_lock_returns_busy_error_when_live_pid_holds_lock(self) -> None:
        server._GRADLE_LOCK_PATH.parent.mkdir(parents=True, exist_ok=True)
        server._GRADLE_LOCK_PATH.write_text(
            json.dumps({"pid": 424242, "instance_id": "other", "operation": "runVerifyMod"}),
            encoding="utf-8",
        )
        with patch.object(server, "_is_pid_alive", return_value=True):
            res = await server.run_gradle_task(["compileJava"], lock_wait_secs=0.05)
        self.assertFalse(res["ok"])
        self.assertEqual(res["stage"], "gradle_lock")
        self.assertEqual(res["lock_owner"]["pid"], 424242)
        self.assertEqual(res["lock_owner"]["operation"], "runVerifyMod")


if __name__ == "__main__":
    unittest.main()
