"""Regression coverage for bridge-owned live session lifecycle."""

import asyncio
import io
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, patch

import server


class Process:
    pid = 123

    def __init__(self):
        self.running = True

    def poll(self):
        return None if self.running else 0

    def wait(self, timeout):
        if self.running:
            raise subprocess.TimeoutExpired("server", timeout)
        return 0


class LifecycleTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.proc = Process()
        self.sess = {"server_proc": self.proc, "log_fh": io.BytesIO(), "rcon_port": 1234,
                     "rcon_password": "secret", "run_dir": Path("unused"), "client_name": None}
        server._LIVE_SESSIONS["test"] = self.sess

    def tearDown(self):
        server._LIVE_SESSIONS.clear()

    async def test_equal_and_invalid_ports_are_rejected(self):
        for rcon, game in ((10, 10), (-1, 0), (65536, 0)):
            result = await server.start_smoke_server(rcon_port=rcon, server_port=game)
            self.assertEqual(result["stage"], "validation")

    async def test_concurrent_stop_shares_operation(self):
        async def stop(*args, **kwargs):
            await asyncio.sleep(0.01)
            self.proc.running = False
            return {"ok": True}
        with patch.object(server, "rcon_command", side_effect=stop) as command:
            first, second = await asyncio.gather(server.stop_smoke_server("test"), server.stop_smoke_server("test"))
        self.assertTrue(first["ok"] and second["ok"])
        self.assertEqual(command.call_count, 1)
        self.assertNotIn("test", server._LIVE_SESSIONS)

    async def test_failed_kill_keeps_session_for_retry(self):
        with patch.object(server, "rcon_command", new=AsyncMock(return_value={"ok": False})), patch.object(server, "_kill_process_tree"):
            result = await server.stop_smoke_server("test")
        self.assertFalse(result["ok"])
        self.assertIn("test", server._LIVE_SESSIONS)
        self.assertFalse(self.sess["log_fh"].closed)

    async def test_client_stop_error_does_not_skip_server(self):
        self.sess["client_name"] = "client"
        with patch.object(server, "stop_mcpilot_client", new=AsyncMock(side_effect=RuntimeError("failed"))), patch.object(server, "stop_smoke_server", new=AsyncMock(return_value={"ok": True})) as stop:
            result = await server.stop_live_session("test")
        self.assertFalse(result["ok"])
        stop.assert_awaited_once()

    async def test_concurrent_live_stops_share_client_and_server_shutdown(self):
        self.sess["client_name"] = "client"
        async def stop_client(*args, **kwargs):
            await asyncio.sleep(0.01)
            return {"ok": True}
        with patch.object(server, "stop_mcpilot_client", side_effect=stop_client) as client, patch.object(server, "stop_smoke_server", new=AsyncMock(return_value={"ok": True})) as stop:
            first, second = await asyncio.gather(server.stop_live_session("test"), server.stop_live_session("test"))
        self.assertTrue(first["ok"] and second["ok"])
        client.assert_awaited_once()
        stop.assert_awaited_once()

    async def test_cancelled_waiter_does_not_cancel_stop(self):
        async def stop(*args, **kwargs):
            await asyncio.sleep(0.02)
            self.proc.running = False
            return {"ok": True}
        with patch.object(server, "rcon_command", side_effect=stop):
            waiter = asyncio.create_task(server.stop_smoke_server("test"))
            await asyncio.sleep(0)
            waiter.cancel()
            with self.assertRaises(asyncio.CancelledError):
                await waiter
            await self.sess["stop_task"]
        self.assertNotIn("test", server._LIVE_SESSIONS)

    async def test_readiness_requires_authenticated_rcon_and_preserves_log(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def launch(*args, **kwargs):
                kwargs["stdout"].write(b"RCON running on 127.0.0.1\n")
                kwargs["stdout"].flush()
                return self.proc
            def kill(proc):
                proc.running = False
            with patch.object(server, "REPO_ROOT", root), patch.object(server.subprocess, "Popen", side_effect=launch), patch.object(server, "_rcon_command_sync", return_value={"ok": False}) as probe, patch.object(server, "_kill_process_tree", side_effect=kill):
                result = await server.start_smoke_server(timeout_secs=1)
            self.assertFalse(result["ok"])
            self.assertEqual(result["stage"], "readiness")
            self.assertTrue(probe.called)
            self.assertTrue((root / result["log_path"]).is_file())
            self.assertNotIn(result["session_id"], server._LIVE_SESSIONS)

    async def test_cleanup_failure_is_reported(self):
        self.proc.running = False
        with patch.object(server.shutil, "rmtree", side_effect=OSError("locked")):
            result = await server.stop_smoke_server("test", cleanup_dir=True)
        self.assertFalse(result["ok"])
        self.assertFalse(result["cleaned_dir"])
        self.assertIn("locked", result["error"])

    async def test_cancelled_start_cleans_process(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            started = asyncio.Event()
            def launch(*args, **kwargs):
                started.set()
                return self.proc
            async def stop(*args, **kwargs):
                self.proc.running = False
                return {"ok": True}
            with patch.object(server, "REPO_ROOT", root), patch.object(server.subprocess, "Popen", side_effect=launch), patch.object(server, "rcon_command", side_effect=stop):
                task = asyncio.create_task(server.start_smoke_server(timeout_secs=10))
                await started.wait()
                task.cancel()
                with self.assertRaises(asyncio.CancelledError):
                    await task
            self.assertFalse(self.proc.running)
            self.assertEqual(list(server._LIVE_SESSIONS), ["test"])

    async def test_client_launch_failure_redacts_server_password(self):
        response = {"ok": True, "session_id": "test", "rcon_password": "secret", "log_path": "log"}
        with patch.object(server, "start_smoke_server", new=AsyncMock(return_value=response)), patch.object(server, "mcpilot_command", new=AsyncMock(return_value={"ok": False})), patch.object(server, "stop_smoke_server", new=AsyncMock(return_value={"ok": True})) as stop:
            result = await server.start_live_session()
        self.assertEqual(result["stage"], "client_launch")
        self.assertNotIn("secret", str(result))
        stop.assert_awaited_once_with(session_id="test")

    async def test_cancelled_client_launch_stops_server(self):
        response = {"ok": True, "session_id": "test", "rcon_password": "secret", "log_path": "log"}
        with patch.object(server, "start_smoke_server", new=AsyncMock(return_value=response)), patch.object(server, "mcpilot_command", new=AsyncMock(side_effect=asyncio.CancelledError)), patch.object(server, "stop_mcpilot_client", new=AsyncMock(return_value={"ok": True})), patch.object(server, "stop_smoke_server", new=AsyncMock(return_value={"ok": True})) as stop:
            with self.assertRaises(asyncio.CancelledError):
                await server.start_live_session()
        stop.assert_awaited_once_with(session_id="test")

    async def test_early_exit_preserves_redacted_log(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def launch(*args, **kwargs):
                props = next(root.glob('artifacts/diagnostics/*/server.properties')).read_text()
                password = next(line.split('=', 1)[1] for line in props.splitlines() if line.startswith('rcon.password='))
                kwargs['stdout'].write(('error ' + password).encode())
                kwargs['stdout'].flush()
                self.proc.running = False
                return self.proc
            with patch.object(server, "REPO_ROOT", root), patch.object(server.subprocess, "Popen", side_effect=launch):
                result = await server.start_smoke_server(timeout_secs=3)
            self.assertFalse(result['ok'])
            self.assertIn('[redacted]', result['log_tail'])
            self.assertTrue((root / result['log_path']).exists())


if __name__ == "__main__":
    unittest.main()
