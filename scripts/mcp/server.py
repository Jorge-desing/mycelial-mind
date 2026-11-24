"""FastMCP CodeMode server for Mycelial Mind (Minecraft 1.21.1 / NeoForge 21.1.176).

Exposes repository discovery, registry inspection, Python asset studios,
Gradle build/GameTest runners, visual regression checks, and RCON controls
behind FastMCP's CodeMode transform so AI agents can discover tools on demand
and chain multi-step workflows in a single sandboxed Python execution.
"""

from __future__ import annotations

import asyncio
import atexit
import contextlib
import csv
import fnmatch
import functools
import heapq
import inspect
import io
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import unicodedata
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Callable

from fastmcp import FastMCP
from fastmcp.experimental.transforms.code_mode import CodeMode, MontySandboxProvider

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

JAVA_SRC_ROOT = REPO_ROOT / "src" / "main" / "java" / "com" / "alex" / "mycelialmind"
RESOURCES_ROOT = REPO_ROOT / "src" / "main" / "resources"
REGISTRY_DIR = JAVA_SRC_ROOT / "registry"

_PREFERRED_JAVA_HOMES = (
    Path(r"C:\Program Files\Java\jdk-21"),
    Path(r"C:\Program Files\Java\graalvm-jdk-21.0.7+8.1"),
)

_SKIP_SEARCH_DIRS = frozenset({
    ".git",
    ".gradle",
    ".idea",
    ".mct",
    ".tools",
    ".venv",
    "__pycache__",
    "build",
    "dist",
    "run",
    "runs",
    "run-data",
})

_TRANS_RE = re.compile(r'Component\.translatable\(\s*"([a-z0-9_.-]+)"')
_GAMETEST_RE = re.compile(r"@GameTest\b(?:\(([^)]*)\))?")
_BATCH_RE = re.compile(r'batch\s*=\s*"([^"]+)"')
_TMPL_RE = re.compile(r'template\s*=\s*"([^"]+)"')
_GAMETEST_METHOD_RE = re.compile(r"public\s+static\s+void\s+([A-Za-z0-9_]+)\s*\(")
_UNIT_ANN_RE = re.compile(r"@Test\b")
_UNIT_METHOD_RE = re.compile(r"(?:public\s+|protected\s+|private\s+)?(?:static\s+)?void\s+([A-Za-z0-9_]+)\s*\(")
_ERR_LINE_RE = re.compile(r"(?:\berror:|FAILURE:|Execution failed|FAILED\b)")
_GT_MARKER_RE = re.compile(r"(?:All \d+ required tests passed|Failed required tests:|\d+/\d+ GameTests|ALL VERIFICATION STEPS COMPLETED)")
_REG_ID_RE = re.compile(
    r'(?:registerBlock|registerItem|registerComponentType|registerColorable|register|\.\s*register)\(\s*"([a-z0-9_]+)"'
)
_ENUM_CONST_RE = re.compile(r"^\s*([A-Z][A-Z0-9_]+)\s*(?:\(|,|;)")


def _iter_scandir_files(
    root: Path,
    *,
    skip_dirs: frozenset[str] = frozenset(),
    suffix: str = "",
) -> list[Path]:
    """Walk root via os.scandir, pruning skip_dirs before descending, returning sorted file Paths."""
    if not root.is_dir():
        return []
    found: list[Path] = []
    stack = [str(root)]
    while stack:
        current = stack.pop()
        try:
            with os.scandir(current) as it:
                for entry in it:
                    name = entry.name
                    try:
                        if entry.is_dir(follow_symlinks=False):
                            if name not in skip_dirs:
                                stack.append(entry.path)
                        elif entry.is_file(follow_symlinks=False):
                            if not suffix or name.endswith(suffix):
                                found.append(Path(entry.path))
                    except OSError:
                        continue
        except OSError:
            continue
    found.sort()
    return found

# --- CodeMode meta-tool schemas -------------------------------------------
# FastMCP's stock `get_schema` only resolves tools that live in the backend
# catalog, so asking for `search`, `get_schema` or `execute` used to answer
# "Tools not found". The factory below merges the synthetic CodeMode tools into
# the reply, and MycelialCodeMode wires it in, so the three meta-tools are
# introspectable like any other tool.

from fastmcp.experimental.transforms.code_mode import Search as _CodeModeSearch
from fastmcp.server.context import Context as _MCPContext
from fastmcp.server.transforms.search.base import (
    serialize_tools_for_output_json as _serialize_tools_json,
    serialize_tools_for_output_markdown as _serialize_tools_markdown,
)
from fastmcp.tools.base import Tool as _FastMCPTool


def _render_tool_selection(tools: list[Any], detail: str) -> str:
    """Render a tool selection like FastMCP's discovery tools do."""
    if detail == "full":
        return json.dumps(_serialize_tools_json(tools), indent=2)
    if detail == "detailed":
        return _serialize_tools_markdown(tools)
    if not tools:
        return "No tools matched the query."
    return "\n".join(
        f"- {tool.name}: {tool.description}" if getattr(tool, "description", "") else f"- {tool.name}"
        for tool in tools
    )


class _MetaAwareSchemas:
    """Discovery-tool factory: `get_schema` that also resolves CodeMode's own tools."""

    def __init__(
        self,
        meta_provider: Callable[[], list[Any]],
        name: str = "get_schema",
        default_detail: str = "detailed",
    ) -> None:
        self._meta_provider = meta_provider
        self._name = name
        self._default_detail = default_detail

    def __call__(self, get_catalog: Callable[[Any], Any]) -> Any:
        meta_provider = self._meta_provider
        default_detail = self._default_detail

        async def get_schema(
            tools: list[str],
            detail: str = default_detail,
            ctx: _MCPContext = None,  # type: ignore[assignment]
        ) -> str:
            """Get parameter schemas for specific tools, CodeMode meta-tools included.

            Use after searching to get the detail needed to call a tool.
            """
            catalog = list(await get_catalog(ctx))
            backend_by_name = {tool.name: tool for tool in catalog}
            meta_by_name = {tool.name: tool for tool in meta_provider()}
            matched: list[Any] = []
            not_found: list[str] = []
            for name in tools:
                if name in backend_by_name:
                    matched.append(backend_by_name[name])
                elif name in meta_by_name:
                    matched.append(meta_by_name[name])
                else:
                    not_found.append(name)
            if not matched and not_found:
                return f"Tools not found: {', '.join(not_found)}"
            rendered = _render_tool_selection(matched, detail)
            if not_found:
                rendered += f"\n\nTools not found: {', '.join(not_found)}"
            return rendered

        return _FastMCPTool.from_function(fn=get_schema, name=self._name)


class MycelialCodeMode(CodeMode):
    """CodeMode whose `get_schema` also knows `search`, `get_schema` and `execute`."""

    def __init__(self, **kwargs: Any) -> None:
        self._meta_tools_cache: list[Any] | None = None
        kwargs.setdefault(
            "discovery_tools",
            [_CodeModeSearch(), _MetaAwareSchemas(lambda: self._meta_tools())],
        )
        super().__init__(**kwargs)

    def _meta_tools(self) -> list[Any]:
        """The exact Tool objects CodeMode publishes, built once on first use."""
        if self._meta_tools_cache is None:
            meta_tools = list(self._build_discovery_tools())
            meta_tools.append(self._get_execute_tool())
            self._meta_tools_cache = meta_tools
        return self._meta_tools_cache


sandbox = MontySandboxProvider(
    limits={
        "max_feed_duration_secs": 600.0,
        "max_memory": 100_000_000,
    }
)

mcp = FastMCP(
    name="MycelialMindDev",
    instructions=(
        "FastMCP CodeMode server for Mycelial Mind (Minecraft 1.21.1 / NeoForge 21.1.267). "
        "Always use `search`/`get_schema` to discover tools on demand and `execute` to run "
        "sandboxed Python scripts calling `await call_tool(name, args)` (all tools return dicts; "
        "use `asyncio.gather(...)` for parallel checks and return a compact summary dict). "
        "Tool domains (see `list_tool_tags()`): "
        "(1) Meta/Observability: get_bridge_health, get_bridge_metrics, list_tool_tags, "
        "clear_bridge_cache, capture_audit_snapshot, diff_audit_snapshots; "
        "(2) Code/Registry/Hive AI/Skills: list_mod_sources, search_mod_code, read_mod_file, "
        "get_registered_content, get_hive_ai_overview, list_gametests, list_unit_tests, "
        "list_datapack_content, get_gradle_config, list_skills, get_skill_guide('mycelial-codemode'), "
        "validate_skills; "
        "(3) Studios & Visual QA: run_model_studio, run_texture_studio, run_particle_studio, run_sound_studio, "
        "inspect_sound_event, run_codex_studio, run_structure_studio, get_uv_manifest, audit_mod_assets, check_visual_regression, "
        "check_structure_visual_regression, get_latest_structure_visual_smoke; "
        "(4) Build/Suites/Cache/Reports: manage_verify_cache, run_gradle_task, run_verify_mod, "
        "get_latest_junit_report, get_latest_pitest_report, get_latest_worldgen_audit; "
        "(5) Unified Live Game Control (Server + RCON + MC Pilot + JFR): start_smoke_server, "
        "smoke_server_status, stop_smoke_server, start_live_session, stop_live_session, "
        "run_live_scenario, collect_spark_tps, capture_mcpilot_screenshot, get_latest_live_report, "
        "compare_live_reports, get_live_hive_status, get_live_colony_status, run_live_verify_scenario, rcon_command, "
        "run_rcon_batch, run_rcon_verify_suite, prepare_mcpilot_client, mcpilot_command, "
        "stop_mcpilot_client, list_java_processes, run_jfr, summarize_jfr, get_diagnostics_guide, "
        "diagnose_symptom, collect_diagnostic_bundle, read_diagnostic_logs, list_diagnostic_artifacts. "
        "Read-only MCP resources: mycelial://bridge/health, mycelial://bridge/tools, mycelial://bridge/tags, "
        "mycelial://memory/tail, mycelial://skills, mycelial://skills/{skill_name}, "
        "mycelial://skills/{skill_name}/{document*} (document may contain '/'). get_schema resolves the "
        "CodeMode meta-tools (search, get_schema, execute) as well as every bridge tool."
    ),
    transforms=[MycelialCodeMode(sandbox_provider=sandbox)],
)

_CACHE_TTL_SECS = 30.0
_CACHE: dict[str, tuple[float, Any]] = {}
_CACHE_STATS: dict[str, int] = {"hits": 0, "misses": 0, "invalidations": 0}
_HOST_ENV_CACHE: dict[str, Any] | None = None
_SKIP_DIAGNOSTIC_DIRS = frozenset({
    ".git",
    ".gradle",
    ".mixin.out",
    ".tmp",
    "advancements",
    "assets",
    "build",
    "datapacks",
    "downloads",
    "entities",
    "gradle-alt",
    "libraries",
    "mc-pilot-source",
    "observable-5.4.4-vbo-fix-src",
    "playerdata",
    "poi",
    "region",
    "resourcepacks",
    "saves",
    "sbl-pilot",
    "stats",
    "versions",
    "world",
})
_METRICS: dict[str, dict[str, Any]] = {}
_AUDIT_SNAPSHOTS: dict[str, dict[str, Any]] = {}
_TOOL_TAGS: dict[str, list[str]] = {}
_LIVE_SESSIONS: dict[str, dict[str, Any]] = {}
# Identity of this particular bridge process: several clients (DSH, Codex,
# Antigravity, ...) each spawn their own stdio instance of this server.
_INSTANCE_ID = uuid.uuid4().hex[:8]
_PROCESS_STARTED_EPOCH = time.time()


def _cache_get(key: str) -> Any | None:
    entry = _CACHE.get(key)
    if entry is None:
        _CACHE_STATS["misses"] += 1
        return None
    expires_at, val = entry
    if time.monotonic() > expires_at:
        _CACHE.pop(key, None)
        _CACHE_STATS["misses"] += 1
        return None
    _CACHE_STATS["hits"] += 1
    return val


def _cache_set(key: str, value: Any, ttl: float = _CACHE_TTL_SECS) -> Any:
    _CACHE[key] = (time.monotonic() + ttl, value)
    return value


def _invalidate_cache() -> int:
    global _HOST_ENV_CACHE
    count = len(_CACHE)
    _CACHE.clear()
    _HOST_ENV_CACHE = None
    _CACHE_STATS["invalidations"] += 1
    return count


def _record_metric(name: str, ok: bool, duration_ms: float) -> None:
    m = _METRICS.setdefault(
        name,
        {"calls": 0, "errors": 0, "total_ms": 0.0, "max_ms": 0.0},
    )
    m["calls"] += 1
    if not ok:
        m["errors"] += 1
    m["total_ms"] += duration_ms
    if duration_ms > m["max_ms"]:
        m["max_ms"] = duration_ms


def _tool(tags: set[str] | None = None) -> Callable[[Callable[..., Any]], Callable[..., Any]]:
    """Register an MCP tool with semantic tags and automatic latency/error telemetry."""
    tag_set = set(tags or ())

    def decorator(fn: Callable[..., Any]) -> Callable[..., Any]:
        _TOOL_TAGS[fn.__name__] = sorted(tag_set)
        if inspect.iscoroutinefunction(fn):
            @functools.wraps(fn)
            async def async_wrapper(*args: Any, **kwargs: Any) -> Any:
                t0 = time.monotonic()
                ok = True
                try:
                    res = await fn(*args, **kwargs)
                    if isinstance(res, dict) and res.get("ok") is False:
                        ok = False
                    return res
                except Exception:
                    ok = False
                    raise
                finally:
                    _record_metric(fn.__name__, ok, round((time.monotonic() - t0) * 1000.0, 2))

            return mcp.tool(tags=tag_set)(async_wrapper)

        @functools.wraps(fn)
        def sync_wrapper(*args: Any, **kwargs: Any) -> Any:
            t0 = time.monotonic()
            ok = True
            try:
                res = fn(*args, **kwargs)
                if isinstance(res, dict) and res.get("ok") is False:
                    ok = False
                return res
            except Exception:
                ok = False
                raise
            finally:
                _record_metric(fn.__name__, ok, round((time.monotonic() - t0) * 1000.0, 2))

        return mcp.tool(tags=tag_set)(sync_wrapper)

    return decorator


def _find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return int(s.getsockname()[1])


def _cleanup_all_live_sessions() -> None:
    """Best-effort cleanup of bridge-owned smoke server processes and log handles on interpreter exit."""
    for sess in list(_LIVE_SESSIONS.values()):
        proc = sess.get("server_proc")
        if proc is not None and proc.poll() is None:
            _kill_process_tree(proc)
        log_fh = sess.get("log_fh")
        if log_fh is not None:
            try:
                log_fh.close()
            except OSError:
                pass
    _LIVE_SESSIONS.clear()


atexit.register(_cleanup_all_live_sessions)


def _resolve_repo_path(relative_path: str) -> Path:
    """Resolve a path relative to REPO_ROOT and block directory traversal."""
    candidate = (REPO_ROOT / relative_path).resolve()
    if candidate != REPO_ROOT and REPO_ROOT not in candidate.parents:
        raise ValueError(f"Path escapes repository root: {relative_path}")
    return candidate


def _build_gradle_env() -> dict[str, str]:
    """Build subprocess environment matching Verify-Mod.ps1 (JDK 21 + Windows truststore)."""
    env = os.environ.copy()
    for java_home in _PREFERRED_JAVA_HOMES:
        if java_home.is_dir():
            env["JAVA_HOME"] = str(java_home)
            break
    env.setdefault("GRADLE_OPTS", "-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT")
    return env


def _kill_process_tree(proc: subprocess.Popen[bytes]) -> None:
    """Terminate a process and all its Windows child processes cleanly."""
    if os.name == "nt":
        subprocess.run(
            ["taskkill", "/F", "/T", "/PID", str(proc.pid)],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
    try:
        proc.kill()
    except OSError:
        pass
    try:
        proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        pass


def _resolve_temp_dir() -> str:
    """Return a writable temporary directory, falling back inside artifacts/diagnostics/.tmp."""
    try:
        candidate = tempfile.gettempdir()
        with tempfile.TemporaryFile(mode="w+b", dir=candidate):
            pass
        return candidate
    except OSError:
        fallback = REPO_ROOT / "artifacts" / "diagnostics" / ".tmp"
        fallback.mkdir(parents=True, exist_ok=True)
        return str(fallback)


def _run_subprocess(
    cmd: list[str],
    *,
    env: dict[str, str] | None = None,
    timeout_secs: float = 300,
    tail_lines: int = 80,
) -> dict[str, Any]:
    """Execute a command in REPO_ROOT and return a compact structured summary.

    Uses temporary files instead of OS pipes so background grandchildren such as
    the Gradle Daemon cannot hold stdout/stderr handles open after exit, and
    detaches stdin from the MCP stdio transport.
    """
    start = time.monotonic()
    tmp_dir = _resolve_temp_dir()
    base_env = env if env is not None else os.environ
    fallback_tmp = str(REPO_ROOT / "artifacts" / "diagnostics" / ".tmp")
    needs_tmp_override = (
        tmp_dir == fallback_tmp
        or not os.path.isdir(base_env.get("TEMP", ""))
        or not os.path.isdir(base_env.get("TMP", ""))
    )
    if needs_tmp_override:
        child_env: dict[str, str] | None = dict(base_env)
        child_env["TEMP"] = tmp_dir
        child_env["TMP"] = tmp_dir
    else:
        child_env = env
    with (
        tempfile.TemporaryFile(mode="w+b", dir=tmp_dir) as out_f,
        tempfile.TemporaryFile(mode="w+b", dir=tmp_dir) as err_f,
    ):
        proc = subprocess.Popen(
            cmd,
            cwd=str(REPO_ROOT),
            env=child_env,
            stdin=subprocess.DEVNULL,
            stdout=out_f,
            stderr=err_f,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
        try:
            returncode = proc.wait(timeout=timeout_secs)
            timed_out = False
        except subprocess.TimeoutExpired:
            _kill_process_tree(proc)
            returncode = -1
            timed_out = True

        elapsed = round(time.monotonic() - start, 2)
        out_f.seek(0)
        err_f.seek(0)
        out_text = out_f.read().decode("utf-8", errors="replace")
        err_text = err_f.read().decode("utf-8", errors="replace")

    stdout_lines = out_text.splitlines()
    stderr_lines = err_text.splitlines()
    if timed_out:
        return {
            "ok": False,
            "exit_code": -1,
            "timed_out": True,
            "duration_secs": elapsed,
            "error": f"Command timed out after {timeout_secs}s",
            "stdout_tail": "\n".join(stdout_lines[-tail_lines:]),
            "stderr_tail": "\n".join(stderr_lines[-tail_lines:]),
        }

    return {
        "ok": returncode == 0,
        "exit_code": returncode,
        "timed_out": False,
        "duration_secs": elapsed,
        "stdout_tail": "\n".join(stdout_lines[-tail_lines:]),
        "stderr_tail": "\n".join(stderr_lines[-tail_lines:]),
    }


async def _run_subprocess_async(
    cmd: list[str],
    *,
    env: dict[str, str] | None = None,
    timeout_secs: float = 300,
    tail_lines: int = 80,
) -> dict[str, Any]:
    """Run _run_subprocess on a worker thread so the MCP event loop stays responsive."""
    return await asyncio.to_thread(
        _run_subprocess,
        cmd,
        env=env,
        timeout_secs=timeout_secs,
        tail_lines=tail_lines,
    )


@_tool(tags={"code"})
def list_mod_sources(subdir: str = "", scope: str = "java") -> dict[str, Any]:
    """List relative file paths in the mod's Java sources, resources, or scripts.

    Args:
        subdir: Optional subdirectory filter (e.g. 'hive', 'entity', 'client/model').
        scope: One of 'java', 'resources', or 'scripts'.
    """
    roots = {
        "java": JAVA_SRC_ROOT,
        "resources": RESOURCES_ROOT,
        "scripts": REPO_ROOT / "scripts",
    }
    if scope not in roots:
        raise ValueError(f"Invalid scope '{scope}'. Expected one of: {sorted(roots)}")
    base = roots[scope]
    target = (base / subdir).resolve() if subdir else base
    if target != base and base not in target.parents:
        raise ValueError(f"Subdir escapes scope root: {subdir}")
    if not target.exists():
        return {"scope": scope, "subdir": subdir, "count": 0, "files": []}
    files = [
        p.relative_to(REPO_ROOT).as_posix()
        for p in _iter_scandir_files(target, skip_dirs=_SKIP_SEARCH_DIRS)
    ]
    return {"scope": scope, "subdir": subdir, "count": len(files), "files": files}


@_tool(tags={"code"})
def search_mod_code(
    pattern: str,
    scope: str = "src",
    file_ext: str = ".java",
    max_results: int = 50,
    ignore_case: bool = False,
) -> dict[str, Any]:
    """Search repository source files with a regex pattern.

    Args:
        pattern: Regular expression to search for.
        scope: Relative directory under repo root (default 'src').
        file_ext: File extension filter (e.g. '.java', '.json', '.py', or '' for all).
        max_results: Maximum matching lines to return.
        ignore_case: Whether to match case-insensitively.
    """
    root = _resolve_repo_path(scope)
    if not root.exists():
        return {"ok": True, "pattern": pattern, "count": 0, "truncated": False, "matches": []}
    flags = re.IGNORECASE if ignore_case else 0
    try:
        compiled = re.compile(pattern, flags)
    except re.error as exc:
        return {"ok": False, "pattern": pattern, "count": 0, "truncated": False, "matches": [], "error": f"Invalid regex: {exc}"}
    matches: list[dict[str, Any]] = []

    for path in _iter_scandir_files(root, skip_dirs=_SKIP_SEARCH_DIRS, suffix=file_ext):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        rel = path.relative_to(REPO_ROOT).as_posix()
        for lineno, line in enumerate(text.splitlines(), start=1):
            if compiled.search(line):
                matches.append({"file": rel, "line": lineno, "text": line.strip()})
                if len(matches) >= max_results:
                    return {
                        "ok": True,
                        "pattern": pattern,
                        "count": len(matches),
                        "truncated": True,
                        "matches": matches,
                    }
    return {
        "ok": True,
        "pattern": pattern,
        "count": len(matches),
        "truncated": False,
        "matches": matches,
    }


@_tool(tags={"code"})
def read_mod_file(relative_path: str, start_line: int = 1, end_line: int = 200) -> dict[str, Any]:
    """Read a slice of a repository file with line numbers.

    Args:
        relative_path: Path relative to the repository root.
        start_line: 1-based inclusive start line.
        end_line: 1-based inclusive end line.
    """
    target = _resolve_repo_path(relative_path)
    if not target.is_file():
        return {"ok": False, "path": relative_path, "error": f"File not found: {relative_path}"}
    lines = target.read_text(encoding="utf-8", errors="replace").splitlines()
    start_idx = max(0, start_line - 1)
    requested_end = min(len(lines), max(start_idx, end_line))
    max_window = 400
    end_idx = min(requested_end, start_idx + max_window)
    numbered = [f"{i + 1}: {lines[i]}" for i in range(start_idx, end_idx)]
    return {
        "ok": True,
        "path": relative_path,
        "start_line": start_idx + 1,
        "end_line": end_idx,
        "total_lines": len(lines),
        "truncated": end_idx < requested_end,
        "content": "\n".join(numbered),
    }


@_tool(tags={"code", "registry"})
def get_registered_content() -> dict[str, list[str]]:
    """Extract registered block, item, entity, block-entity, data-component, and feature IDs from registry/*.java."""
    cached = _cache_get("registered_content")
    if cached is not None:
        return cached
    results: dict[str, list[str]] = {}
    if not REGISTRY_DIR.is_dir():
        return results
    for java_file in sorted(REGISTRY_DIR.glob("Mod*.java")):
        text = java_file.read_text(encoding="utf-8", errors="replace")
        ids = sorted(set(_REG_ID_RE.findall(text)))
        results[java_file.stem] = ids
    return _cache_set("registered_content", results)


@_tool(tags={"code", "meta"})
def get_memory_tail(lines: int = 80) -> dict[str, Any]:
    """Read the last N lines of MEMORY.md to inspect recent session history."""
    memory_file = REPO_ROOT / "MEMORY.md"
    if not memory_file.is_file():
        return {"exists": False, "total_lines": 0, "content": "MEMORY.md not found."}
    all_lines = memory_file.read_text(encoding="utf-8", errors="replace").splitlines()
    return {
        "exists": True,
        "total_lines": len(all_lines),
        "content": "\n".join(all_lines[-max(1, lines) :]),
    }


@_tool(tags={"skills"})
async def validate_skills() -> dict[str, Any]:
    """Run scripts/validate_agent_skills.py in-process (cached) to verify Open Agent Skills frontmatter."""
    cached = _cache_get("validate_skills")
    if cached is not None:
        return cached
    from scripts.validate_agent_skills import validate_all

    t0 = time.monotonic()
    buf = io.StringIO()
    ok = True
    exit_code = 0
    stderr_tail = ""
    try:
        with contextlib.redirect_stdout(buf):
            rc = validate_all()
        if rc != 0:
            ok = False
            exit_code = int(rc)
    except (AssertionError, ValueError, OSError) as exc:
        ok = False
        exit_code = 1
        stderr_tail = str(exc)

    elapsed = round(time.monotonic() - t0, 4)
    stdout_tail = "\n".join(buf.getvalue().splitlines()[-60:])
    res: dict[str, Any] = {
        "ok": ok,
        "exit_code": exit_code,
        "timed_out": False,
        "duration_secs": elapsed,
        "stdout_tail": stdout_tail,
        "stderr_tail": stderr_tail,
    }
    return _cache_set("validate_skills", res)


async def _run_studio_with_tests(
    result_key: str,
    build_args: list[str],
    tests_dir: str,
    *,
    check_only: bool,
    run_tests: bool,
    build_timeout_secs: int = 180,
) -> dict[str, Any]:
    """Run a Python asset studio script and optionally its unittest suite, invalidating cache on mutations."""
    if not check_only:
        _invalidate_cache()
    build_res = await _run_subprocess_async(build_args, timeout_secs=build_timeout_secs)
    out: dict[str, Any] = {result_key: build_res}
    if run_tests and build_res["ok"]:
        test_res = await _run_subprocess_async(
            [sys.executable, "-m", "unittest", "discover", "-s", tests_dir, "-v"],
            timeout_secs=120,
        )
        out["tests"] = test_res
        out["ok"] = build_res["ok"] and test_res["ok"]
    else:
        out["ok"] = build_res["ok"]
    return out


@_tool(tags={"studios"})
async def run_model_studio(check_only: bool = True, run_tests: bool = True) -> dict[str, Any]:
    """Run Mycelial Mind 3D Model Studio (emit or check EntityModels) and its unit tests.

    Args:
        check_only: If True, validates UV packing without overwriting Java files.
        run_tests: If True, also runs scripts/model_studio/tests.
    """
    args = [sys.executable, "scripts/model_studio/build_models.py", "--check" if check_only else "--all"]
    return await _run_studio_with_tests(
        "model_studio",
        args,
        "scripts/model_studio/tests",
        check_only=check_only,
        run_tests=run_tests,
        build_timeout_secs=120,
    )


@_tool(tags={"studios"})
async def run_texture_studio(
    check_only: bool = True,
    pbr: bool = True,
    export_extras: bool = False,
    run_tests: bool = True,
) -> dict[str, Any]:
    """Run Mycelial Mind HD Texture Studio and its unit tests.

    Args:
        check_only: If True, validates textures/LabPBR without regenerating files.
        pbr: Enable LabPBR 1.3 normal/specular generation & validation.
        export_extras: Export palettes and UV guides (used when check_only=False).
        run_tests: Also run scripts/texture_studio/tests.
    """
    args = [sys.executable, "scripts/texture_studio/build_textures.py", "--check"]
    if not check_only:
        args.append("--all")
        if export_extras:
            args.append("--export-extras")
    if pbr:
        args.append("--pbr")
    return await _run_studio_with_tests(
        "texture_studio",
        args,
        "scripts/texture_studio/tests",
        check_only=check_only,
        run_tests=run_tests,
    )


@_tool(tags={"studios"})
async def run_particle_studio(
    check_only: bool = True,
    export_extras: bool = False,
    run_tests: bool = True,
) -> dict[str, Any]:
    """Run Mycelial Mind procedural Particle Studio and its unit tests.

    Args:
        check_only: If True, validates sprites/descriptors/registry parity without regenerating.
        export_extras: Export the contact sheet and HTML gallery (used when check_only=False).
        run_tests: Also run scripts/particle_studio/tests.
    """
    args = [sys.executable, "scripts/particle_studio/build_particles.py", "--check"]
    if not check_only:
        args.append("--all")
        if export_extras:
            args.append("--export-extras")
    return await _run_studio_with_tests(
        "particle_studio",
        args,
        "scripts/particle_studio/tests",
        check_only=check_only,
        run_tests=run_tests,
    )


@_tool(tags={"studios"})
async def run_sound_studio(
    check_only: bool = True,
    run_tests: bool = True,
    only: str | None = None,
    changed_only: bool = False,
    check_baseline: bool = False,
    update_baseline: bool = False,
) -> dict[str, Any]:
    """Run Mycelial Mind Procedural Sound Studio and its unit tests.

    Args:
        check_only: If True, validates .ogg headers, sounds.json, bilingual subtitles, and ModSounds.java without regenerating files.
        run_tests: Also run scripts/sound_studio/tests.
        only: Optional comma-separated sound_ids to synthesize incrementally (when check_only=False).
        changed_only: If True (and check_only=False), only rebuild sound events whose code, spec, IR, or AI stem changed.
        check_baseline: If True, verify acoustic metrics against exports/sound_baseline.json.
        update_baseline: If True (and check_only=False), update exports/sound_baseline.json.
    """
    args = [sys.executable, "scripts/sound_studio/build_sounds.py", "--check"]
    if not check_only:
        if only:
            cleaned = ",".join(s.strip() for s in only.split(",") if s.strip())
            if not re.fullmatch(r"[a-z0-9_,]+", cleaned):
                return {"ok": False, "studio": "sound_studio", "error": f"Invalid sound_id filter: {only}"}
            args.extend(["--only", cleaned])
        elif changed_only:
            args.append("--changed-only")
        else:
            args.append("--all")
        if update_baseline:
            args.append("--update-baseline")
    if check_baseline:
        args.append("--check-baseline")
    return await _run_studio_with_tests(
        "sound_studio",
        args,
        "scripts/sound_studio/tests",
        check_only=check_only,
        run_tests=run_tests,
    )


@_tool(tags={"studios", "assets"})
def inspect_sound_event(sound_id: str) -> dict[str, Any]:
    """Inspect acoustic telemetry, IR room, AI stem, Ogg files, and baseline delta for a sound event."""
    sid = sound_id.strip()
    if not sid:
        return {"ok": False, "error": "sound_id must be non-empty"}
    exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
    manifest_path = exports_dir / "sound_manifest.json"
    qr_path = exports_dir / "sound_quality_report.json"
    baseline_path = exports_dir / "sound_baseline.json"
    if not manifest_path.is_file():
        return {"ok": False, "error": f"Missing manifest at {manifest_path}"}
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    entry = next((e for e in manifest.get("sounds", []) if e.get("sound_id") == sid), None)
    if entry is None:
        return {"ok": False, "error": f"Unknown sound_id '{sid}'", "total_events": len(manifest.get("sounds", []))}

    corr = entry.get("max_variant_corr", 0.0)
    if qr_path.is_file():
        qr = json.loads(qr_path.read_text(encoding="utf-8"))
        corr = qr.get("event_correlations", {}).get(sid, corr)

    base_entry = None
    delta: dict[str, float] | None = None
    if baseline_path.is_file():
        base = json.loads(baseline_path.read_text(encoding="utf-8"))
        base_entry = (base.get("events", {}) or {}).get(sid)
        if isinstance(base_entry, dict):
            m = entry.get("metrics", {})
            delta = {
                "active_rms_delta_db": round(float(m.get("active_rms_dbfs", 0.0)) - float(base_entry.get("active_rms_dbfs", 0.0)), 2),
                "lufs_delta": round(float(m.get("integrated_lufs", 0.0)) - float(base_entry.get("integrated_lufs", 0.0)), 2),
                "true_peak_delta_db": round(
                    float(m.get("true_peak_dbfs", m.get("peak_dbfs", 0.0))) - float(base_entry.get("true_peak_dbfs", 0.0)),
                    2,
                ),
                "centroid_delta_hz": round(float(m.get("spectral_centroid_hz", 0.0)) - float(base_entry.get("spectral_centroid_hz", 0.0)), 1),
            }

    return {
        "ok": True,
        "sound_id": sid,
        "category": entry.get("category"),
        "species": entry.get("species"),
        "loudness_group": entry.get("loudness_group"),
        "ir_space": entry.get("ir_space"),
        "ai_hybrid": bool(entry.get("ai_hybrid", False)),
        "stem_id": entry.get("stem_id", ""),
        "attenuation_distance": entry.get("attenuation_distance"),
        "files": entry.get("files", []),
        "max_variant_corr": corr,
        "metrics": entry.get("metrics", {}),
        "baseline": base_entry,
        "baseline_delta": delta,
    }


@_tool(tags={"studios"})
async def run_codex_studio(
    check_only: bool = True,
    report: bool = False,
    run_tests: bool = True,
) -> dict[str, Any]:
    """Run Mycelial Mind Codex Studio (12 chapters / 50 entries) and its unit tests.

    Args:
        check_only: If True, audits coverage, refs, recipe pairs, and page budgets without exporting files.
        report: If True, emits full JSON coverage report to stdout.
        run_tests: Also run scripts/codex_studio/tests.
    """
    args = [sys.executable, "scripts/codex_studio/build_codex.py", "--check"]
    if not check_only:
        args.append("--all")
    if report:
        args.append("--report")
    return await _run_studio_with_tests(
        "codex_studio",
        args,
        "scripts/codex_studio/tests",
        check_only=check_only,
        run_tests=run_tests,
    )


@_tool(tags={"visual", "diagnostics"})
def check_visual_regression(
    baseline_path: str = "scripts/model_studio/exports/baseline_visual_smoke.png",
    current_path: str = "run/screenshots/visual_smoke_mobs.png",
    threshold_ssim: float = 0.90,
    include_diff_regions: bool = False,
    include_lpips: bool = False,
) -> dict[str, Any]:
    """Compare a current screenshot against the baseline using SSIM, 64-bit DCT pHash, and optional diff_regions/LPIPS."""
    from scripts.visual_regression import _load_gray, compute_diff_regions, compute_lpips, compute_phash, compute_ssim

    base_file = _resolve_repo_path(baseline_path)
    curr_file = _resolve_repo_path(current_path)
    if not base_file.is_file():
        return {"ok": False, "error": f"Baseline not found: {baseline_path}"}
    if not curr_file.is_file():
        return {"ok": False, "error": f"Current screenshot not found: {current_path}"}

    try:
        base_arr = _load_gray(str(base_file))
        curr_arr = _load_gray(str(curr_file), size=(base_arr.shape[1], base_arr.shape[0]))
        ssim = round(compute_ssim(base_arr, curr_arr), 4)
        phash_dist = (compute_phash(str(base_file)) ^ compute_phash(str(curr_file))).bit_count()
        out: dict[str, Any] = {
            "ok": ssim >= threshold_ssim,
            "ssim": ssim,
            "threshold_ssim": threshold_ssim,
            "phash_hamming_distance": phash_dist,
        }
        if include_diff_regions:
            out["diff_regions_count"] = len(compute_diff_regions(base_arr, curr_arr))
        if include_lpips:
            lpips_val = compute_lpips(str(base_file), str(curr_file))
            out["lpips"] = round(lpips_val, 4) if lpips_val is not None else None
        return out
    except (OSError, ValueError) as exc:
        return {"ok": False, "error": str(exc)}


@_tool(tags={"visual", "structures"})
async def check_structure_visual_regression(
    screenshot_dir: str = "",
    update_baseline: bool = False,
    require_baseline: bool = False,
    min_ssim: float = 0.90,
    max_hamming: int = 6,
    max_lpips: float = 0.35,
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Run scripts/structure_visual_check.py to validate structure smoke captures, update contact sheet, and run SSIM/pHash/LPIPS regression."""
    cmd = [
        sys.executable,
        "scripts/structure_visual_check.py",
        "--min-ssim",
        str(min_ssim),
        "--max-hamming",
        str(max_hamming),
        "--max-lpips",
        str(max_lpips),
    ]
    if screenshot_dir:
        resolved = _resolve_repo_path(screenshot_dir)
        cmd.extend(["--screenshot-dir", str(resolved)])
    if update_baseline:
        cmd.append("--update-baseline")
    if require_baseline:
        cmd.append("--require-baseline")
    res = await _run_subprocess_async(cmd, timeout_secs=timeout_secs)
    report_file = REPO_ROOT / "artifacts" / "diagnostics" / "structures" / "structure-visual-smoke-latest.json"
    if report_file.is_file():
        try:
            res["report"] = json.loads(report_file.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            pass
    return res


@_tool(tags={"studios", "structures"})
async def run_structure_studio(timeout_secs: int = 120) -> dict[str, Any]:
    """Run scripts/build_structures.py to build and verify 6-connected hive_sanctum/*.nbt Jigsaw pieces."""
    _invalidate_cache()
    return await _run_subprocess_async([sys.executable, "scripts/build_structures.py"], timeout_secs=timeout_secs)


@_tool(tags={"visual", "structures"})
def get_latest_structure_visual_smoke() -> dict[str, Any]:
    """Read artifacts/diagnostics/structures/structure-visual-smoke-latest.json for AI agent inspection."""
    report_file = REPO_ROOT / "artifacts" / "diagnostics" / "structures" / "structure-visual-smoke-latest.json"
    if not report_file.is_file():
        return {"exists": False, "path": "artifacts/diagnostics/structures/structure-visual-smoke-latest.json"}
    try:
        data = json.loads(report_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {"exists": True, "ok": False, "path": "artifacts/diagnostics/structures/structure-visual-smoke-latest.json", "error": str(exc)}
    return {"exists": True, "ok": bool(data.get("ok", True)), "path": "artifacts/diagnostics/structures/structure-visual-smoke-latest.json", "report": data}


def _annotate_gradle_result(res: dict[str, Any]) -> dict[str, Any]:
    """Attach extracted error_lines and GameTest summary markers to Gradle/Verify-Mod results."""
    combined_lines = (res.get("stdout_tail", "") + "\n" + res.get("stderr_tail", "")).splitlines()
    if not res["ok"]:
        res["error_lines"] = [line.strip() for line in combined_lines if _ERR_LINE_RE.search(line)][:20]
    gt_matches = [line.strip() for line in combined_lines if _GT_MARKER_RE.search(line)]
    if gt_matches:
        res["summary_markers"] = gt_matches[-5:]
    return res


_ALLOWED_GRADLE_TASKS = {
    "build",
    "clean",
    "classes",
    "compileJava",
    "runData",
    "runGameTestServer",
    "runHiveLifecycleSmoke",
    "runVisualSmoke",
    "runAnimationVisualSmoke",
    "runStructureVisualSmoke",
    "stageMcPilotDependency",
    "publishMods",
    "test",
    "testClasses",
    "pitest",
}


def _build_gradle_task_command(
    tasks: list[str],
    max_workers: int,
    no_daemon: bool,
    use_offline: bool,
    isolated_gametests: bool = False,
) -> list[str]:
    """Build a safe Gradle command; CodeMode can only run publishMods in dry-run mode."""
    publish_dry_run = "publishMods" in tasks
    if publish_dry_run:
        safe_publish_args = {"publishMods", "--offline", "--refresh-dependencies"}
        unsafe_publish_args = [t for t in tasks if t not in safe_publish_args]
        if unsafe_publish_args:
            raise ValueError(
                "publishMods is available through CodeMode only as a forced dry-run; "
                f"remove project/system properties and unsupported flags: {unsafe_publish_args}"
            )

    for t in tasks:
        if t not in _ALLOWED_GRADLE_TASKS and not t.startswith(("--", "-P", "-D")):
            raise ValueError(f"Task '{t}' is not in allowed Gradle tasks: {sorted(_ALLOWED_GRADLE_TASKS)}")

    cmd = ["cmd.exe", "/c", "gradlew.bat", *tasks]
    if isolated_gametests and "runGameTestServer" in tasks and not publish_dry_run and "-PisolatedGameTests=true" not in tasks:
        cmd.append("-PisolatedGameTests=true")
    if publish_dry_run:
        # Never allow the MCP route to publish remotely, even if user Gradle properties differ.
        cmd.append("-PpublishDryRun=true")
    cmd.append(f"--max-workers={max_workers}")
    if no_daemon:
        cmd.append("--no-daemon")
    if use_offline and "--offline" not in tasks:
        cmd.append("--offline")
    return cmd


@_tool(tags={"gradle"})
async def run_gradle_task(
    tasks: list[str],
    no_daemon: bool = False,
    max_workers: int = 2,
    timeout_secs: int = 300,
    offline: bool | None = None,
    isolated_gametests: bool = False,
) -> dict[str, Any]:
    """Run allowlisted Gradle tasks; publishMods is restricted to a forced local dry-run.

    Uses JDK 21 and Windows root truststore automatically. Defaults to max_workers=2 for
    parallel task execution and auto-enables --offline when GRADLE_USER_HOME uses .gradle-user-home.
    Pass isolated_gametests=True with runGameTestServer to inject -PisolatedGameTests=true, or include
    -PdialectVisualSmoke=true alongside runVisualSmoke for the Nexus/Dialects UI smoke scene.
    """
    use_offline = (
        offline
        if offline is not None
        else (
            os.environ.get("GRADLE_USER_HOME", "").rstrip("\\/").endswith(".gradle-user-home")
            and "--refresh-dependencies" not in tasks
        )
    )
    cmd = _build_gradle_task_command(tasks, max_workers, no_daemon, use_offline, isolated_gametests=isolated_gametests)
    res = await _run_subprocess_async(cmd, env=_build_gradle_env(), timeout_secs=timeout_secs, tail_lines=60)
    _invalidate_cache()
    return _annotate_gradle_result(res)


@_tool(tags={"gradle", "diagnostics"})
async def run_verify_mod(
    include_server_smoke: bool = False,
    include_visual_smoke: bool = False,
    include_structure_visual_smoke: bool = False,
    include_worldgen_audit: bool = False,
    worldgen_audit_radius: int = 8,
    force_regenerate: bool = False,
    no_daemon: bool = False,
    timeout_secs: int = 540,
) -> dict[str, Any]:
    """Run scripts/Verify-Mod.ps1 full validation pipeline."""
    cmd = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "scripts/Verify-Mod.ps1",
    ]
    if include_server_smoke:
        cmd.append("-IncludeServerSmoke")
    if include_visual_smoke:
        cmd.append("-IncludeVisualSmoke")
    if include_structure_visual_smoke:
        cmd.append("-IncludeStructureVisualSmoke")
    if include_worldgen_audit:
        cmd.extend(["-IncludeWorldgenAudit", "-WorldgenAuditRadius", str(worldgen_audit_radius)])
    if force_regenerate:
        cmd.append("-ForceRegenerate")
    if no_daemon:
        cmd.append("-NoDaemon")
    res = await _run_subprocess_async(cmd, env=_build_gradle_env(), timeout_secs=timeout_secs, tail_lines=80)
    _invalidate_cache()
    return _annotate_gradle_result(res)


@_tool(tags={"gradle", "meta"})
def manage_verify_cache(action: str = "status", scope: str = "all") -> dict[str, Any]:
    """Check or clear SHA-256 verification stamps in build/.verify-stamps/.

    Args:
        action: 'status' to check which scopes are cached, or 'clear' to invalidate stamps.
        scope: 'all', 'models', 'textures', or 'datagen'.
    """
    from scripts.verify_cache import SCOPES, STAMP_DIR, _compute_digest, _outputs_present

    if action not in {"status", "clear"}:
        raise ValueError("action must be 'status' or 'clear'")
    targets = sorted(SCOPES) if scope == "all" else [scope]
    for s in targets:
        if s not in SCOPES:
            raise ValueError(f"Invalid scope '{s}'. Expected one of: {['all', *sorted(SCOPES)]}")

    if action == "clear":
        removed: list[str] = []
        for s in targets:
            stamp = STAMP_DIR / f"{s}.sha256"
            if stamp.is_file():
                stamp.unlink()
                removed.append(s)
        return {"ok": True, "action": "clear", "removed": removed}

    status: dict[str, dict[str, Any]] = {}
    for s in targets:
        stamp = STAMP_DIR / f"{s}.sha256"
        outputs_ok = _outputs_present(s)
        digest = _compute_digest(SCOPES[s])
        stored = stamp.read_text(encoding="utf-8").strip() if stamp.is_file() else None
        status[s] = {
            "cached": bool(outputs_ok and stored == digest),
            "outputs_present": outputs_ok,
            "stamp_exists": stamp.is_file(),
        }
    return {
        "ok": True,
        "action": "status",
        "all_cached": all(v["cached"] for v in status.values()),
        "scopes": status,
    }


def _rcon_command_sync(
    command: str,
    host: str,
    port: int,
    password: str,
    timeout_secs: float = 10.0,
) -> dict[str, Any]:
    from scripts.rcon_verify import recv_packet, send_packet

    try:
        with socket.create_connection((host, port), timeout=timeout_secs) as sock:
            send_packet(sock, 1, 3, password)
            rid, _, _ = recv_packet(sock)
            if rid == -1:
                return {"ok": False, "error": "RCON authentication failed"}
            send_packet(sock, 2, 2, command)
            _, _, body = recv_packet(sock)
            return {"ok": True, "command": command, "response": body}
    except (OSError, EOFError, ValueError) as exc:
        return {"ok": False, "command": command, "error": str(exc)}


def _rcon_batch_sync(
    commands: list[str],
    host: str,
    port: int,
    password: str,
    timeout_secs: float = 15.0,
) -> dict[str, Any]:
    from scripts.rcon_verify import recv_packet, send_packet

    results: list[dict[str, str]] = []
    try:
        with socket.create_connection((host, port), timeout=timeout_secs) as sock:
            send_packet(sock, 1, 3, password)
            rid, _, _ = recv_packet(sock)
            if rid == -1:
                return {"ok": False, "error": "RCON authentication failed", "results": results}
            for idx, cmd in enumerate(commands, start=2):
                send_packet(sock, idx, 2, cmd)
                _, _, body = recv_packet(sock)
                results.append({"command": cmd, "response": body})
        return {"ok": True, "count": len(results), "results": results}
    except (OSError, EOFError, ValueError) as exc:
        return {"ok": False, "error": str(exc), "results": results}


@_tool(tags={"rcon"})
async def rcon_command(
    command: str,
    host: str = "127.0.0.1",
    port: int = 25575,
    password: str = "mycelialtest",
    timeout_secs: float = 10.0,
) -> dict[str, Any]:
    """Send a single RCON command to a running local Minecraft dev/smoke server."""
    return await asyncio.to_thread(_rcon_command_sync, command, host, port, password, timeout_secs)


@_tool(tags={"rcon"})
async def run_rcon_batch(
    commands: list[str],
    host: str = "127.0.0.1",
    port: int = 25575,
    password: str = "mycelialtest",
    timeout_secs: float = 15.0,
) -> dict[str, Any]:
    """Send multiple RCON commands over a single authenticated TCP session."""
    if not commands:
        return {"ok": True, "count": 0, "results": []}
    return await asyncio.to_thread(_rcon_batch_sync, commands, host, port, password, timeout_secs)


def _get_host_env_snapshot() -> dict[str, Any]:
    """Resolve static host environment details (JAVA_HOME, jcmd, temp_dir) once until cache invalidation."""
    global _HOST_ENV_CACHE
    if _HOST_ENV_CACHE is not None:
        return _HOST_ENV_CACHE
    env = _build_gradle_env()
    try:
        jcmd_path = str(_find_jdk21_tool("jcmd.exe"))
    except FileNotFoundError:
        jcmd_path = ""
    _HOST_ENV_CACHE = {
        "java_home_detected": env.get("JAVA_HOME", ""),
        "jcmd_available": bool(jcmd_path),
        "temp_dir": _resolve_temp_dir(),
    }
    return _HOST_ENV_CACHE


@_tool(tags={"meta", "observability"})
def get_bridge_health() -> dict[str, Any]:
    """Return instant MCP bridge runtime diagnostics (FastMCP version, Python, JDK 21 detection)."""
    import fastmcp

    host_env = _get_host_env_snapshot()
    return {
        "ok": True,
        "server": mcp.name,
        "fastmcp_version": getattr(fastmcp, "__version__", "unknown"),
        "tool_count": len(_TOOL_TAGS),
        "active_live_sessions": len(_LIVE_SESSIONS),
        "cache_entries": len(_CACHE),
        "repo_root": str(REPO_ROOT),
        "python_executable": sys.executable,
        "java_home_detected": host_env["java_home_detected"],
        "jcmd_available": host_env["jcmd_available"],
        "temp_dir": host_env["temp_dir"],
        "instance_id": _INSTANCE_ID,
        "instance_pid": os.getpid(),
        "instance_started_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(_PROCESS_STARTED_EPOCH)),
        "instance_uptime_secs": round(time.time() - _PROCESS_STARTED_EPOCH, 1),
    }


async def _run_ps1_async(
    script_rel_path: str,
    ps_args: list[str] | None = None,
    *,
    mct_home: str = "",
    timeout_secs: int = 120,
    tail_lines: int = 100,
) -> dict[str, Any]:
    """Run a repository PowerShell script asynchronously with JDK 21 env and optional MCT_HOME."""
    _resolve_repo_path(script_rel_path)
    env = _build_gradle_env()
    if mct_home:
        env["MCT_HOME"] = (
            str(_resolve_repo_path(mct_home)) if not Path(mct_home).is_absolute() else mct_home
        )
    cmd = [
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        script_rel_path,
        *(ps_args or []),
    ]
    return await _run_subprocess_async(cmd, env=env, timeout_secs=timeout_secs, tail_lines=tail_lines)


_JDK21_TOOL_CACHE: dict[str, Path] = {}


def _find_jdk21_tool(exe_name: str) -> Path:
    """Locate a JDK 21 binary such as jcmd.exe or jfr.exe, caching resolved paths."""
    cached = _JDK21_TOOL_CACHE.get(exe_name)
    if cached is not None and cached.is_file():
        return cached
    candidates = list(_PREFERRED_JAVA_HOMES)
    env_java_home = os.environ.get("JAVA_HOME")
    if env_java_home:
        candidates.append(Path(env_java_home))
    for java_home in candidates:
        candidate = java_home / "bin" / exe_name
        if candidate.is_file():
            _JDK21_TOOL_CACHE[exe_name] = candidate
            return candidate
    on_path = shutil.which(exe_name)
    if on_path:
        resolved = Path(on_path)
        _JDK21_TOOL_CACHE[exe_name] = resolved
        return resolved
    raise FileNotFoundError(f"JDK 21 tool '{exe_name}' not found under {candidates}")


@_tool(tags={"diagnostics", "skills"})
def get_diagnostics_guide(topic: str = "overview") -> dict[str, Any]:
    """Read the mycelial-diagnostics workflow ('overview'), tool matrix ('tools'), or combined CodeMode playbook ('codemode')."""
    diag_root = REPO_ROOT / ".agents" / "skills" / "mycelial-diagnostics"
    codemode_root = REPO_ROOT / ".agents" / "skills" / "mycelial-codemode"
    if topic == "codemode":
        files = [
            diag_root / "references" / "herramientas-y-escenarios.md",
            codemode_root / "references" / "audit-playbooks.md",
        ]
        missing = [f.relative_to(REPO_ROOT).as_posix() for f in files if not f.is_file()]
        if missing:
            return {"ok": False, "topic": topic, "content": f"Missing files: {missing}"}
        combined = "\n\n---\n\n".join(f.read_text(encoding="utf-8", errors="replace") for f in files)
        return {
            "ok": True,
            "topic": topic,
            "paths": [f.relative_to(REPO_ROOT).as_posix() for f in files],
            "content": combined,
        }
    targets = {
        "overview": diag_root / "SKILL.md",
        "tools": diag_root / "references" / "herramientas-y-escenarios.md",
    }
    if topic not in targets:
        raise ValueError(f"Invalid topic '{topic}'. Expected one of: {sorted([*targets.keys(), 'codemode'])}")
    path = targets[topic]
    if not path.is_file():
        return {"ok": False, "topic": topic, "content": f"Missing file: {path.as_posix()}"}
    return {
        "ok": True,
        "topic": topic,
        "path": path.relative_to(REPO_ROOT).as_posix(),
        "content": path.read_text(encoding="utf-8", errors="replace"),
    }


def _normalize_diag_text(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    ascii_only = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    return re.sub(r"\s+", " ", ascii_only.lower()).strip()


_DIAGNOSTIC_SYMPTOM_MATRIX: dict[str, dict[str, Any]] = {
    "hive_gameplay": {
        "label": "Rasgos, daño, infección, nodos o oleadas",
        "keywords": ("rasgos", "dano", "infeccion", "nodos", "oleadas", "hive", "traits", "damage", "waves"),
        "tools": ["get_hive_ai_overview", "list_gametests", "run_gradle_task", "run_live_verify_scenario"],
        "first_step": "Run runGameTestServer / inspect hive AI and reproduce thresholds in an isolated world.",
    },
    "server_rcon": {
        "label": "Comandos o integración de servidor",
        "keywords": ("comandos", "rcon", "servidor", "verify", "status", "server"),
        "tools": ["start_smoke_server", "get_live_hive_status", "run_live_verify_scenario", "run_rcon_batch", "stop_smoke_server"],
        "first_step": "Start an isolated smoke server and verify /mycelial status and /mycelial verify scenarios via RCON.",
    },
    "client_ui_net": {
        "label": "HUD, GUI, animación, shaders o red",
        "keywords": ("hud", "gui", "animacion", "shaders", "red", "network", "mcpilot", "codex"),
        "tools": ["prepare_mcpilot_client", "start_live_session", "capture_mcpilot_screenshot", "read_diagnostic_logs", "stop_live_session"],
        "first_step": "Launch an isolated MC Pilot client session and cross-check server payloads, client logs, and screenshots.",
    },
    "models_textures": {
        "label": "Modelos, UV, texturas o recursos",
        "keywords": ("modelos", "uv", "texturas", "pbr", "recursos", "models", "textures", "assets", "visual"),
        "tools": ["run_model_studio", "run_texture_studio", "audit_mod_assets", "run_gradle_task", "check_visual_regression"],
        "first_step": "Run model/texture studio validators, audit_mod_assets, and refresh runVisualSmoke + SSIM/LPIPS diff.",
    },
    "tps_lag": {
        "label": "Bajadas de TPS o tirones",
        "keywords": ("tps", "mspt", "tirones", "lag", "spark", "observable", "performance"),
        "tools": ["run_live_scenario", "collect_spark_tps", "run_jfr", "summarize_jfr", "compare_live_reports"],
        "first_step": "Compare baseline vs load using run_live_scenario(capture_perf=True, enable_diagnostics=True) and JFR/spark.",
    },
    "fps_render": {
        "label": "Baja tasa de FPS",
        "keywords": ("fps", "render", "gpu", "frames", "client_perf"),
        "tools": ["start_live_session", "capture_mcpilot_screenshot", "check_visual_regression", "read_diagnostic_logs", "stop_live_session"],
        "first_step": "Separate GPU/client rendering from server TPS and capture controlled client visuals.",
    },
    "hang_memory": {
        "label": "Cuelgue o fuga de memoria",
        "keywords": ("cuelgue", "fuga", "memoria", "threads", "jcmd", "jfr", "hang", "memory", "deadlock"),
        "tools": ["list_java_processes", "run_jfr", "summarize_jfr", "read_diagnostic_logs"],
        "first_step": "Locate the Minecraft JVM PID via list_java_processes() and inspect Threads + JFR recordings.",
    },
    "worldgen_structures": {
        "label": "Biomas, estructuras o dimensión",
        "keywords": ("biomas", "estructuras", "dimension", "worldgen", "sanctum", "citadel", "structures"),
        "tools": ["run_structure_studio", "get_latest_worldgen_audit", "run_gradle_task", "check_structure_visual_regression", "get_latest_structure_visual_smoke"],
        "first_step": "Audit NBT connectivity, worldgen-audit-latest.json distributions, and runStructureVisualSmoke renders.",
    },
    "colony_dialects": {
        "label": "Colonia Soberana, Reconstrucción o Dialectos",
        "keywords": ("colonia", "colony", "dialect", "dialecto", "nexus", "nexo", "symbiont", "simbionte", "reconstruccion", "reconstruction"),
        "tools": ["get_hive_ai_overview", "get_live_colony_status", "run_live_verify_scenario", "run_gradle_task", "inspect_sound_event"],
        "first_step": "Inspect colony/dialect enums via get_hive_ai_overview(), run '/mycelial verify colony' or get_live_colony_status(), and capture UI with runVisualSmoke -PdialectVisualSmoke=true.",
    },
    "expeditions_cycles": {
        "label": "Expediciones Post-Matriarca, Flores o Ciclos",
        "keywords": ("expedition", "expedicion", "enclave", "bloom", "flor", "cycle", "ciclo", "matriarch", "matriarca"),
        "tools": ["get_live_colony_status", "get_live_hive_status", "run_live_verify_scenario", "run_gradle_task"],
        "first_step": "Check cycle/colony live state via get_live_colony_status() and run '/mycelial verify cycles' or isolated GameTests (-PisolatedGameTests=true).",
    },
}


def _collect_report_status_summary() -> dict[str, dict[str, Any]]:
    raw_reports = {
        "live_report": get_latest_live_report(),
        "junit": get_latest_junit_report(),
        "pitest": get_latest_pitest_report(),
        "worldgen_audit": get_latest_worldgen_audit(),
        "structure_visual_smoke": get_latest_structure_visual_smoke(),
    }
    summary: dict[str, dict[str, Any]] = {}
    for key, val in raw_reports.items():
        entry: dict[str, Any] = {
            "exists": bool(val.get("exists")),
            "ok": val.get("ok"),
            "path": val.get("path"),
        }
        if "mutation_score_pct" in val:
            entry["mutation_score_pct"] = val["mutation_score_pct"]
        summary[key] = entry
    return summary


def _resolve_symptom_key(query: str) -> str | None:
    norm_q = _normalize_diag_text(query)
    if not norm_q:
        return None
    if norm_q in _DIAGNOSTIC_SYMPTOM_MATRIX:
        return norm_q
    for key, entry in _DIAGNOSTIC_SYMPTOM_MATRIX.items():
        norm_keywords = [_normalize_diag_text(k) for k in entry["keywords"]]
        if norm_q in norm_keywords:
            return key
        if any(re.search(rf"\b{re.escape(kw)}\b", norm_q) for kw in norm_keywords):
            return key
        if len(norm_q) >= 3 and (norm_q in _normalize_diag_text(entry["label"]) or norm_q in key):
            return key
    return None


@_tool(tags={"diagnostics", "skills", "meta"})
def diagnose_symptom(symptom: str = "all", include_live_state: bool = True) -> dict[str, Any]:
    """Map a mycelial-diagnostics symptom to recommended CodeMode tools and optionally attach live repo/session state."""
    def _build_live_state() -> dict[str, Any]:
        log_errors = (
            read_diagnostic_logs("run/logs/latest.log", tail_lines=30, filter_pattern="ERROR|WARN|FAIL|Exception").get("matched_lines", 0)
            if (REPO_ROOT / "run" / "logs" / "latest.log").is_file()
            else 0
        )
        return {
            "active_smoke_sessions": smoke_server_status().get("count", 0),
            "latest_log_errors": log_errors,
            "latest_reports": _collect_report_status_summary(),
        }

    if _normalize_diag_text(symptom) == "all":
        out: dict[str, Any] = {
            "ok": True,
            "symptom": "all",
            "count": len(_DIAGNOSTIC_SYMPTOM_MATRIX),
            "symptoms": _DIAGNOSTIC_SYMPTOM_MATRIX,
        }
        if include_live_state:
            out["live_state"] = _build_live_state()
        return out

    matched_key = _resolve_symptom_key(symptom)
    if matched_key is None:
        return {
            "ok": False,
            "symptom": symptom,
            "error": f"Unknown symptom '{symptom}'",
            "available_symptoms": sorted(_DIAGNOSTIC_SYMPTOM_MATRIX),
        }

    entry = _DIAGNOSTIC_SYMPTOM_MATRIX[matched_key]
    res: dict[str, Any] = {
        "ok": True,
        "symptom": matched_key,
        "query": symptom,
        "label": entry["label"],
        "tools": list(entry["tools"]),
        "first_step": entry["first_step"],
    }
    if include_live_state:
        res["live_state"] = _build_live_state()
    return res


@_tool(tags={"diagnostics", "reports", "observability"})
async def collect_diagnostic_bundle(log_tail_lines: int = 40, max_artifacts: int = 15) -> dict[str, Any]:
    """Automate Step 1 ('Delimitar y preservar') of mycelial-diagnostics with zero side effects."""
    jvms = await list_java_processes()
    logs: dict[str, Any] = {}
    for log_key, rel_log in (
        ("client_latest", "run/logs/latest.log"),
        ("gametest_latest", "run/gametest-server/logs/latest.log"),
    ):
        if (REPO_ROOT / rel_log).is_file():
            logs[log_key] = read_diagnostic_logs(
                rel_log,
                tail_lines=log_tail_lines,
                filter_pattern="ERROR|WARN|FAIL|Exception",
            )
        else:
            logs[log_key] = {"exists": False, "path": rel_log}

    return {
        "ok": True,
        "health": get_bridge_health(),
        "mod_properties": get_gradle_config().get("properties", {}),
        "java_processes": {**jvms, "minecraft_pid": _pick_minecraft_jvm_pid(jvms.get("processes", []))},
        "smoke_sessions": smoke_server_status(),
        "logs": logs,
        "reports": _collect_report_status_summary(),
        "recent_artifacts": list_diagnostic_artifacts(max_results=max_artifacts),
    }


@_tool(tags={"diagnostics"})
def list_diagnostic_artifacts(
    subdir: str = "",
    glob_pattern: str = "*",
    max_results: int = 200,
) -> dict[str, Any]:
    """List files under artifacts/diagnostics/ newest-first (JFR profiles, Observable JSONs, smoke logs, screenshots)."""
    base = REPO_ROOT / "artifacts" / "diagnostics"
    target = (base / subdir).resolve() if subdir else base
    if target != base and base not in target.parents:
        raise ValueError(f"Subdir escapes artifacts/diagnostics: {subdir}")
    if not target.exists():
        return {"exists": False, "count": 0, "returned_count": 0, "truncated": False, "files": []}
    repo_prefix_len = len(str(REPO_ROOT)) + 1
    match_all = glob_pattern == "*"
    use_path_pattern = "/" in glob_pattern or "\\" in glob_pattern
    prune_internal = not subdir
    target_prefix_len = len(str(target)) + 1
    matched: list[tuple[float, str]] = []
    stack = [str(target)]
    while stack:
        current = stack.pop()
        try:
            with os.scandir(current) as it:
                for entry in it:
                    try:
                        if entry.is_dir(follow_symlinks=False):
                            if prune_internal and entry.name in _SKIP_DIAGNOSTIC_DIRS:
                                continue
                            stack.append(entry.path)
                        elif entry.is_file(follow_symlinks=False):
                            if not match_all:
                                target_name = entry.path[target_prefix_len:].replace("\\", "/") if use_path_pattern else entry.name
                                if not fnmatch.fnmatchcase(target_name, glob_pattern):
                                    continue
                            mtime = entry.stat(follow_symlinks=False).st_mtime
                            matched.append((mtime, entry.path[repo_prefix_len:].replace("\\", "/")))
                    except OSError:
                        continue
        except OSError:
            continue
    total_count = len(matched)
    top = heapq.nlargest(max(1, max_results), matched, key=lambda item: item[0])
    files = [rel for _, rel in top]
    return {
        "exists": True,
        "count": total_count,
        "returned_count": len(files),
        "truncated": total_count > len(files),
        "files": files,
    }


@_tool(tags={"diagnostics"})
def read_diagnostic_logs(
    log_path: str = "run/logs/latest.log",
    tail_lines: int = 120,
    filter_pattern: str = "",
) -> dict[str, Any]:
    """Read and optionally regex-filter a Minecraft or diagnostic log file inside the repo."""
    target = _resolve_repo_path(log_path)
    if not target.is_file():
        return {"ok": False, "path": log_path, "error": f"Log file not found: {log_path}"}
    lines = target.read_text(encoding="utf-8", errors="replace").splitlines()
    if filter_pattern:
        try:
            compiled = re.compile(filter_pattern)
        except re.error as exc:
            return {"ok": False, "path": log_path, "error": f"Invalid regex: {exc}"}
        matched = [line for line in lines if compiled.search(line)]
    else:
        matched = lines
    selected = matched[-max(1, tail_lines) :]
    return {
        "ok": True,
        "path": log_path,
        "total_lines": len(lines),
        "matched_lines": len(matched),
        "returned_lines": len(selected),
        "content": "\n".join(selected),
    }


@_tool(tags={"diagnostics", "jfr"})
async def list_java_processes() -> dict[str, Any]:
    """List local JVM processes via JDK 21 jcmd -l so agents can identify Minecraft PIDs."""
    try:
        jcmd = _find_jdk21_tool("jcmd.exe")
    except FileNotFoundError as exc:
        return {"ok": False, "count": 0, "processes": [], "error": str(exc)}
    res = await _run_subprocess_async([str(jcmd), "-l"], timeout_secs=30, tail_lines=100)
    processes: list[dict[str, Any]] = []
    if res["ok"]:
        for line in res["stdout_tail"].splitlines():
            line = line.strip()
            if not line:
                continue
            parts = line.split(None, 1)
            if parts and parts[0].isdigit():
                processes.append(
                    {"pid": int(parts[0]), "main_class": parts[1] if len(parts) > 1 else ""}
                )
    return {"ok": res["ok"], "count": len(processes), "processes": processes, "raw": res["stdout_tail"]}


@_tool(tags={"diagnostics", "jfr"})
async def run_jfr(
    process_id: int,
    action: str = "Check",
    duration_seconds: int = 120,
) -> dict[str, Any]:
    """Run scripts/Invoke-Jfr.ps1 against a target Minecraft JVM PID (Start, Check, Stop, Threads)."""
    allowed_actions = ("Start", "Check", "Stop", "Threads")
    if action not in allowed_actions:
        raise ValueError(f"Invalid JFR action '{action}'. Expected one of: {allowed_actions}")
    if not (10 <= duration_seconds <= 3600):
        raise ValueError("duration_seconds must be between 10 and 3600")
    return await _run_ps1_async(
        "scripts/Invoke-Jfr.ps1",
        ["-ProcessId", str(process_id), "-Action", action, "-DurationSeconds", str(duration_seconds)],
        timeout_secs=60,
        tail_lines=120,
    )


@_tool(tags={"diagnostics", "jfr"})
async def summarize_jfr(relative_path: str) -> dict[str, Any]:
    """Run JDK 21 jfr.exe summary on a .jfr recording inside the repository."""
    try:
        jfr = _find_jdk21_tool("jfr.exe")
    except FileNotFoundError as exc:
        return {"ok": False, "path": relative_path, "error": str(exc)}
    target = _resolve_repo_path(relative_path)
    if not target.is_file():
        return {"ok": False, "path": relative_path, "error": f"JFR file not found: {relative_path}"}
    return await _run_subprocess_async([str(jfr), "summary", str(target)], timeout_secs=60, tail_lines=120)


@_tool(tags={"mcpilot", "lifecycle"})
async def mcpilot_command(
    args: list[str],
    mct_home: str = "",
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Run scripts/Invoke-McPilot.ps1 (e.g. ['client', 'list'] or ['--client', 'mycelial-1211', 'screenshot']).

    Args:
        args: Arguments passed directly to mct.cmd via Invoke-McPilot.ps1.
        mct_home: Optional isolated MCT_HOME directory (relative to repo root or absolute).
        timeout_secs: Subprocess timeout in seconds.
    """
    return await _run_ps1_async("scripts/Invoke-McPilot.ps1", args, mct_home=mct_home, timeout_secs=timeout_secs)


@_tool(tags={"mcpilot", "lifecycle"})
async def prepare_mcpilot_client(mct_home: str = "", timeout_secs: int = 240) -> dict[str, Any]:
    """Run scripts/Prepare-McPilotClient.ps1 to stage the mod JAR and SmartBrainLib into MC Pilot."""
    return await _run_ps1_async("scripts/Prepare-McPilotClient.ps1", mct_home=mct_home, timeout_secs=timeout_secs)


@_tool(tags={"mcpilot"})
async def build_mcpilot_compat(
    source: str = "artifacts/diagnostics/mc-pilot-source",
    timeout_secs: int = 600,
) -> dict[str, Any]:
    """Run scripts/Build-McPilotCompat.ps1 to build the patched MC Pilot 0.20.2 client mod."""
    return await _run_ps1_async(
        "scripts/Build-McPilotCompat.ps1",
        ["-Source", source],
        timeout_secs=timeout_secs,
        tail_lines=120,
    )


@_tool(tags={"mcpilot"})
async def install_mcpilot_compat(
    client_name: str = "mycelial-1211",
    mct_home: str = "",
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Run scripts/Install-McPilotCompat.ps1 to install the patched MC Pilot JAR into a stopped client."""
    return await _run_ps1_async(
        "scripts/Install-McPilotCompat.ps1",
        ["-ClientName", client_name],
        mct_home=mct_home,
        timeout_secs=timeout_secs,
    )


@_tool(tags={"mcpilot", "lifecycle"})
async def stop_mcpilot_client(
    client_name: str = "mycelial-1211",
    mct_home: str = "",
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Run scripts/Stop-McPilotClient.ps1 to cleanly stop an MC Pilot client and verify port release."""
    return await _run_ps1_async(
        "scripts/Stop-McPilotClient.ps1",
        ["-ClientName", client_name],
        mct_home=mct_home,
        timeout_secs=timeout_secs,
    )


@_tool(tags={"rcon", "diagnostics"})
async def run_rcon_verify_suite(
    host: str = "127.0.0.1",
    port: int = 25575,
    password: str = "mycelialtest",
    include_worldgen_audit: bool = False,
    worldgen_audit_radius: int = 8,
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Run scripts/rcon_verify.py (mycelial status + mycelial verify all + optional worldgen audit + stop)."""
    cmd = [sys.executable, "scripts/rcon_verify.py", host, str(port), password]
    if include_worldgen_audit:
        cmd.extend(["--worldgen-audit", "--worldgen-radius", str(worldgen_audit_radius)])
    return await _run_subprocess_async(
        cmd,
        timeout_secs=timeout_secs,
        tail_lines=100,
    )


@_tool(tags={"diagnostics", "reports"})
def get_latest_worldgen_audit() -> dict[str, Any]:
    """Read and parse artifacts/diagnostics/worldgen-audit-latest.json for AI agent inspection."""
    report_file = REPO_ROOT / "artifacts" / "diagnostics" / "worldgen-audit-latest.json"
    if not report_file.is_file():
        return {"exists": False, "path": "artifacts/diagnostics/worldgen-audit-latest.json"}
    try:
        data = json.loads(report_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {"exists": True, "ok": False, "path": "artifacts/diagnostics/worldgen-audit-latest.json", "error": str(exc)}
    return {"exists": True, "ok": True, "path": "artifacts/diagnostics/worldgen-audit-latest.json", "report": data}


@_tool(tags={"skills"})
def list_skills() -> dict[str, Any]:
    """List all workspace Open Agent Skills under .agents/skills/ and their reference files."""
    skills_dir = REPO_ROOT / ".agents" / "skills"
    if not skills_dir.is_dir():
        return {"count": 0, "skills": []}
    skills: list[dict[str, Any]] = []
    for folder in sorted(p for p in skills_dir.iterdir() if p.is_dir()):
        refs = [
            r.relative_to(folder).as_posix()
            for r in sorted((folder / "references").rglob("*.md"))
        ] if (folder / "references").is_dir() else []
        skills.append(
            {
                "name": folder.name,
                "has_skill_md": (folder / "SKILL.md").is_file(),
                "references": refs,
            }
        )
    return {"count": len(skills), "skills": skills}


@_tool(tags={"skills"})
def get_skill_guide(skill_name: str, doc: str = "SKILL.md") -> dict[str, Any]:
    """Read any workspace skill's SKILL.md or reference markdown (e.g. 'mycelial-codemode', 'mycelial-ai-hive', 'neoforge-gametest')."""
    skills_root = (REPO_ROOT / ".agents" / "skills").resolve()
    target = (skills_root / skill_name / doc).resolve()
    if skills_root not in target.parents:
        raise ValueError(f"Path escapes .agents/skills: {skill_name}/{doc}")
    if not target.is_file():
        return {"ok": False, "skill": skill_name, "doc": doc, "error": "Skill document not found"}
    return {
        "ok": True,
        "skill": skill_name,
        "doc": doc,
        "path": target.relative_to(REPO_ROOT).as_posix(),
        "content": target.read_text(encoding="utf-8", errors="replace"),
    }


@_tool(tags={"code", "gradle"})
def list_gametests(batch_filter: str = "") -> dict[str, Any]:
    """List @GameTest methods in src/main/java/**/*.java with file, line, batch, and template."""
    cache_key = f"gametests:{batch_filter}"
    cached = _cache_get(cache_key)
    if cached is not None:
        return cached
    tests: list[dict[str, Any]] = []
    for java_file in _iter_scandir_files(JAVA_SRC_ROOT, suffix=".java"):
        text = java_file.read_text(encoding="utf-8", errors="replace")
        if "@GameTest" not in text:
            continue
        lines = text.splitlines()
        rel = java_file.relative_to(REPO_ROOT).as_posix()
        pending_meta: dict[str, str] | None = None
        for idx, line in enumerate(lines, start=1):
            ann = _GAMETEST_RE.search(line)
            if ann:
                args_str = ann.group(1) or ""
                b_m = _BATCH_RE.search(args_str)
                t_m = _TMPL_RE.search(args_str)
                pending_meta = {
                    "batch": b_m.group(1) if b_m else "default",
                    "template": t_m.group(1) if t_m else "",
                }
                continue
            if pending_meta is not None:
                m = _GAMETEST_METHOD_RE.search(line)
                if m:
                    entry = {
                        "name": m.group(1),
                        "file": rel,
                        "line": idx,
                        **pending_meta,
                    }
                    if not batch_filter or entry["batch"] == batch_filter:
                        tests.append(entry)
                    pending_meta = None
    return _cache_set(cache_key, {"count": len(tests), "tests": tests})


@_tool(tags={"studios"})
def get_uv_manifest(model_id: str = "") -> dict[str, Any]:
    """Summarize scripts/model_studio/exports/uv_manifest.json, or inspect a single model_id in detail."""
    manifest_file = REPO_ROOT / "scripts" / "model_studio" / "exports" / "uv_manifest.json"
    if not manifest_file.is_file():
        return {"exists": False, "models": {}}
    try:
        data = json.loads(manifest_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {"exists": True, "ok": False, "error": str(exc), "models": {}}
    if model_id:
        if model_id not in data:
            return {"exists": True, "ok": False, "error": f"Unknown model_id '{model_id}'", "available": sorted(data)}
        return {"exists": True, "ok": True, "model_id": model_id, "manifest": data[model_id]}
    summary = {
        reg_id: {
            "class_name": info.get("class_name"),
            "tex_size": info.get("tex_size"),
            "palettes": info.get("palettes", []),
            "cube_count": len(info.get("cubes", [])),
        }
        for reg_id, info in data.items()
    }
    return {"exists": True, "ok": True, "count": len(summary), "models": summary}


def _parse_java_enum_constants(path: Path, enum_name: str) -> list[str]:
    """Extract enum constant names from a Java enum source file."""
    consts: list[str] = []
    in_enum = False
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if f"enum {enum_name}" in line:
            in_enum = True
            continue
        if in_enum:
            if ";" in line and not _ENUM_CONST_RE.match(line):
                break
            m = _ENUM_CONST_RE.match(line)
            if m:
                consts.append(m.group(1))
            if ";" in line:
                break
    return consts


@_tool(tags={"code", "hive"})
def get_hive_ai_overview() -> dict[str, Any]:
    """Summarize Mycelial Mind Hive AI and Sovereign Colony subsystems, enum constants, and SBL memory types."""
    cached = _cache_get("hive_ai_overview")
    if cached is not None:
        return cached
    enums: dict[str, list[str]] = {}
    for enum_name in (
        "StimulusType",
        "AdaptationTrait",
        "EvolutionRank",
        "HiveSynergy",
        "HiveDoctrine",
        "HiveOrder",
        "HiveProgressionTier",
        "HiveStrain",
        "PlayerHabit",
        "SwarmRole",
    ):
        path = JAVA_SRC_ROOT / "hive" / f"{enum_name}.java"
        if path.is_file():
            enums[enum_name] = _parse_java_enum_constants(path, enum_name)

    colony_enums: dict[str, list[str]] = {}
    for enum_name in (
        "SymbiontCaste",
        "SymbiontRank",
        "SymbiontTemperament",
        "SovereignTrait",
        "ColonyReconstructionStatus",
    ):
        path = JAVA_SRC_ROOT / "colony" / f"{enum_name}.java"
        if path.is_file():
            colony_enums[enum_name] = _parse_java_enum_constants(path, enum_name)

    hive_files = [
        p.relative_to(REPO_ROOT).as_posix()
        for p in sorted((JAVA_SRC_ROOT / "hive").glob("*.java"))
    ] if (JAVA_SRC_ROOT / "hive").is_dir() else []
    colony_files = [
        p.relative_to(REPO_ROOT).as_posix()
        for p in sorted((JAVA_SRC_ROOT / "colony").glob("*.java"))
    ] if (JAVA_SRC_ROOT / "colony").is_dir() else []
    ai_files = [
        p.relative_to(REPO_ROOT).as_posix()
        for p in sorted((JAVA_SRC_ROOT / "entity" / "ai").rglob("*.java"))
    ] if (JAVA_SRC_ROOT / "entity" / "ai").is_dir() else []
    mem_types = get_registered_content().get("ModMemoryTypes", [])
    return _cache_set(
        "hive_ai_overview",
        {
            "ok": True,
            "hive_files": hive_files,
            "colony_files": colony_files,
            "ai_files": ai_files,
            "enums": enums,
            "colony_enums": colony_enums,
            "memory_types": mem_types,
        },
    )


@_tool(tags={"studios", "diagnostics"})
def audit_mod_assets() -> dict[str, Any]:
    """Cross-check lang parity (en_us vs es_es), static Java/subtitle key references, blockstates/sounds, and UV sync."""
    cached = _cache_get("audit_mod_assets")
    if cached is not None:
        return cached
    assets_root = RESOURCES_ROOT / "assets" / "mycelialmind"
    lang_dir = assets_root / "lang"
    en_path = lang_dir / "en_us.json"
    es_path = lang_dir / "es_es.json"
    sounds_path = assets_root / "sounds.json"
    sounds_data: dict[str, Any] = {}
    if sounds_path.is_file():
        try:
            loaded_sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
            if isinstance(loaded_sounds, dict):
                sounds_data = loaded_sounds
        except (OSError, json.JSONDecodeError):
            sounds_data = {}
    sounds_json_count = len(sounds_data)
    sounds_dir = assets_root / "sounds"
    ogg_rel_set: set[str] = set()
    if sounds_dir.is_dir():
        sounds_prefix_len = len(str(sounds_dir)) + 1
        for ogg_path in _iter_scandir_files(sounds_dir, suffix=".ogg"):
            ogg_rel_set.add(str(ogg_path)[sounds_prefix_len:-4].replace("\\", "/"))
    ogg_files_count = len(ogg_rel_set)
    missing_sound_files: list[str] = []
    vanilla_event_redirects: list[str] = []
    for evt_id, evt_def in sounds_data.items():
        if not isinstance(evt_def, dict):
            missing_sound_files.append(str(evt_id))
            continue
        for s_entry in evt_def.get("sounds", []):
            if isinstance(s_entry, dict):
                if s_entry.get("type") == "event":
                    vanilla_event_redirects.append(str(evt_id))
                    continue
                s_name = s_entry.get("name", "")
            else:
                s_name = str(s_entry)
            if ":" in s_name:
                ns, rel = s_name.split(":", 1)
                if ns != "mycelialmind":
                    vanilla_event_redirects.append(str(evt_id))
                    continue
                ogg_rel = rel
            else:
                ogg_rel = s_name
            if ogg_rel not in ogg_rel_set:
                missing_sound_files.append(f"{evt_id}:{ogg_rel}.ogg")

    referenced_keys = {
        v["subtitle"]
        for v in sounds_data.values()
        if isinstance(v, dict) and isinstance(v.get("subtitle"), str) and v["subtitle"]
    }
    src_java_root = REPO_ROOT / "src" / "main" / "java"
    for java_file in _iter_scandir_files(src_java_root, suffix=".java"):
        try:
            java_text = java_file.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        if "Component.translatable" not in java_text:
            continue
        for key in _TRANS_RE.findall(java_text):
            if not key.endswith("."):
                referenced_keys.add(key)

    lang_report: dict[str, Any] = {"ok": False}
    if en_path.is_file() and es_path.is_file():
        try:
            en_keys = set(json.loads(en_path.read_text(encoding="utf-8")).keys())
            es_keys = set(json.loads(es_path.read_text(encoding="utf-8")).keys())
            missing_in_es = sorted(en_keys - es_keys)
            missing_in_en = sorted(es_keys - en_keys)
            missing_refs = sorted(referenced_keys - en_keys)
            lang_report = {
                "ok": not missing_in_es and not missing_in_en and not missing_refs,
                "en_count": len(en_keys),
                "es_count": len(es_keys),
                "missing_in_es": missing_in_es,
                "missing_in_en": missing_in_en,
                "referenced_keys_count": len(referenced_keys),
                "missing_referenced_keys": missing_refs,
            }
        except json.JSONDecodeError as exc:
            lang_report = {"ok": False, "error": str(exc)}

    tex_dir = assets_root / "textures"
    tex_counts = {"block": 0, "item": 0, "entity": 0, "models": 0, "model_atlas": 0}
    pbr_counts: dict[str, int] = {"normal_n": 0, "specular_s": 0, "emissive": 0, "mcmeta": 0}
    if tex_dir.is_dir():
        tex_prefix_len = len(str(tex_dir)) + 1
        for tex_path in _iter_scandir_files(tex_dir):
            name = tex_path.name
            if name.endswith(".png"):
                top_cat = str(tex_path)[tex_prefix_len:].replace("\\", "/").split("/", 1)[0]
                if top_cat in tex_counts:
                    tex_counts[top_cat] += 1
                if name.endswith("_n.png"):
                    pbr_counts["normal_n"] += 1
                elif name.endswith("_s.png"):
                    pbr_counts["specular_s"] += 1
                elif name.endswith("_emissive.png"):
                    pbr_counts["emissive"] += 1
            elif name.endswith(".png.mcmeta"):
                pbr_counts["mcmeta"] += 1

    models_dir = assets_root / "models"
    model_json_counts = {
        "block": sum(1 for e in os.scandir(models_dir / "block") if e.name.endswith(".json") and e.is_file(follow_symlinks=False)) if (models_dir / "block").is_dir() else 0,
        "item": sum(1 for e in os.scandir(models_dir / "item") if e.name.endswith(".json") and e.is_file(follow_symlinks=False)) if (models_dir / "item").is_dir() else 0,
    }
    blockstates_dir = assets_root / "blockstates"
    blockstates_count = sum(1 for e in os.scandir(blockstates_dir) if e.name.endswith(".json") and e.is_file(follow_symlinks=False)) if blockstates_dir.is_dir() else 0

    uv_info = get_uv_manifest()
    uv_models = set(uv_info.get("models", {}).keys()) if uv_info.get("ok") else set()
    reg_entities = set(get_registered_content().get("ModEntities", []))
    entities_without_uv = sorted(
        e for e in reg_entities if e not in uv_models and e not in ("acid_spit", "neutralizing_spore", "purifier_grenade")
    )

    particles_dir = assets_root / "particles"
    particle_tex_dir = assets_root / "textures" / "particle"
    registered_particles = get_registered_content().get("ModParticles", [])
    particle_json_count = (
        sum(1 for e in os.scandir(particles_dir) if e.name.endswith(".json") and e.is_file(follow_symlinks=False))
        if particles_dir.is_dir() else 0
    )
    particle_texture_count = (
        sum(1 for e in os.scandir(particle_tex_dir) if e.name.endswith(".png") and e.is_file(follow_symlinks=False))
        if particle_tex_dir.is_dir() else 0
    )
    missing_particle_assets: list[str] = []
    for particle_id in registered_particles:
        descriptor = particles_dir / f"{particle_id}.json"
        if not descriptor.is_file():
            missing_particle_assets.append(f"{particle_id}:json")
            continue
        try:
            payload = json.loads(descriptor.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            missing_particle_assets.append(f"{particle_id}:invalid-json")
            continue
        textures = payload.get("textures", []) if isinstance(payload, dict) else []
        if not textures:
            missing_particle_assets.append(f"{particle_id}:no-textures")
        for texture in textures:
            name = texture.split(":", 1)[1] if isinstance(texture, str) and ":" in texture else None
            if not name or not (particle_tex_dir / f"{name}.png").is_file():
                missing_particle_assets.append(f"{particle_id}:{texture}")
    vanilla_particle_hits = 0
    for java_file in _iter_scandir_files(src_java_root, suffix=".java"):
        try:
            java_text = java_file.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        vanilla_particle_hits += java_text.count("ParticleTypes.")
        vanilla_particle_hits += java_text.count("new DustParticleOptions")
    particle_manifest = REPO_ROOT / "scripts" / "particle_studio" / "exports" / "particle_manifest.json"
    particle_studio_ok = particle_manifest.is_file()

    qr_path = REPO_ROOT / "scripts" / "sound_studio" / "exports" / "sound_quality_report.json"
    audio_quality_ok = False
    if qr_path.is_file():
        try:
            qr_data = json.loads(qr_path.read_text(encoding="utf-8"))
            audio_quality_ok = bool(qr_data.get("ok"))
        except (OSError, json.JSONDecodeError):
            audio_quality_ok = False

    return _cache_set(
        "audit_mod_assets",
        {
            "ok": (
                bool(lang_report.get("ok"))
                and not entities_without_uv
                and sounds_json_count > 0
                and not missing_sound_files
                and not vanilla_event_redirects
                and audio_quality_ok
                and not missing_particle_assets
                and vanilla_particle_hits == 0
                and particle_json_count >= 30
                and len(registered_particles) >= 30
            ),
            "lang": lang_report,
            "texture_counts": tex_counts,
            "pbr_counts": pbr_counts,
            "model_json_counts": model_json_counts,
            "blockstates_count": blockstates_count,
            "sounds_json_count": sounds_json_count,
            "ogg_files_count": ogg_files_count,
            "missing_sound_files": missing_sound_files,
            "vanilla_event_redirects": len(vanilla_event_redirects),
            "audio_quality_ok": audio_quality_ok,
            "uv_model_count": len(uv_models),
            "entities_without_uv": entities_without_uv,
            "particle_count": len(registered_particles),
            "particles_json_count": particle_json_count,
            "particle_texture_count": particle_texture_count,
            "missing_particle_assets": missing_particle_assets,
            "vanilla_particle_hits": vanilla_particle_hits,
            "particle_studio_ok": particle_studio_ok,
        },
    )


@_tool(tags={"code", "registry"})
def list_datapack_content() -> dict[str, Any]:
    """Summarize data/mycelialmind/ datapack files and external tag/data_map namespaces (minecraft, c, neoforge)."""
    cached = _cache_get("datapack_content")
    if cached is not None:
        return cached
    data_base = RESOURCES_ROOT / "data"
    data_root = data_base / "mycelialmind"
    if not data_root.is_dir():
        return {"ok": False, "total": 0, "categories": {}, "external_total": 0, "external_namespaces": {}}
    categories: dict[str, list[str]] = {}
    total = 0
    for path in _iter_scandir_files(data_root):
        rel = path.relative_to(data_root).as_posix()
        parts = rel.split("/")
        cat = f"{parts[0]}/{parts[1]}" if parts[0] == "worldgen" and len(parts) > 1 else parts[0]
        categories.setdefault(cat, []).append(rel)
        total += 1
    external_namespaces: dict[str, list[str]] = {}
    for ns_dir in sorted(p for p in data_base.iterdir() if p.is_dir() and p.name != "mycelialmind"):
        ext_files = [p.relative_to(ns_dir).as_posix() for p in _iter_scandir_files(ns_dir)]
        if ext_files:
            external_namespaces[ns_dir.name] = ext_files
    external_total = sum(len(v) for v in external_namespaces.values())
    return _cache_set(
        "datapack_content",
        {
            "ok": True,
            "total": total,
            "counts": {k: len(v) for k, v in sorted(categories.items())},
            "categories": categories,
            "external_total": external_total,
            "external_counts": {k: len(v) for k, v in sorted(external_namespaces.items())},
            "external_namespaces": external_namespaces,
        },
    )


@_tool(tags={"code", "gradle"})
def list_unit_tests() -> dict[str, Any]:
    """List all JUnit 5 @Test methods in src/test/java/**/*.java with file and line numbers."""
    cached = _cache_get("unit_tests")
    if cached is not None:
        return cached
    test_root = REPO_ROOT / "src" / "test" / "java"
    tests: list[dict[str, Any]] = []
    for java_file in _iter_scandir_files(test_root, suffix=".java"):
        text = java_file.read_text(encoding="utf-8", errors="replace")
        if "@Test" not in text:
            continue
        lines = text.splitlines()
        rel = java_file.relative_to(REPO_ROOT).as_posix()
        pending = False
        for idx, line in enumerate(lines, start=1):
            if _UNIT_ANN_RE.search(line):
                pending = True
                continue
            if pending:
                m = _UNIT_METHOD_RE.search(line)
                if m:
                    tests.append({"name": m.group(1), "file": rel, "line": idx})
                    pending = False
    return _cache_set("unit_tests", {"ok": True, "count": len(tests), "tests": tests})


@_tool(tags={"gradle", "reports"})
def get_latest_pitest_report(max_survived: int = 20) -> dict[str, Any]:
    """Summarize build/reports/pitest/mutations.csv (status counts, mutation score, and survived sample)."""
    csv_file = REPO_ROOT / "build" / "reports" / "pitest" / "mutations.csv"
    if not csv_file.is_file():
        return {"exists": False, "path": "build/reports/pitest/mutations.csv"}

    counts: dict[str, int] = {}
    survived: list[dict[str, Any]] = []
    total = 0
    with csv_file.open("r", encoding="utf-8", errors="replace", newline="") as fh:
        for row in csv.reader(fh):
            if len(row) < 6:
                continue
            total += 1
            status = row[5].strip()
            counts[status] = counts.get(status, 0) + 1
            if status in {"SURVIVED", "NO_COVERAGE"} and len(survived) < max(0, max_survived):
                survived.append(
                    {
                        "file": row[0],
                        "class": row[1],
                        "mutator": row[2].rsplit(".", 1)[-1],
                        "method": row[3],
                        "line": int(row[4]) if row[4].isdigit() else row[4],
                        "status": status,
                    }
                )
    killed = counts.get("KILLED", 0)
    score = round((killed / total) * 100.0, 2) if total > 0 else 0.0
    return {
        "exists": True,
        "ok": True,
        "path": "build/reports/pitest/mutations.csv",
        "total_mutations": total,
        "mutation_score_pct": score,
        "status_counts": counts,
        "survived_sample": survived,
    }


@_tool(tags={"gradle", "reports"})
def get_latest_junit_report() -> dict[str, Any]:
    """Summarize JUnit 5 XML reports in build/test-results/test/TEST-*.xml."""
    results_dir = REPO_ROOT / "build" / "test-results" / "test"
    xml_files = sorted(results_dir.glob("TEST-*.xml")) if results_dir.is_dir() else []
    if not xml_files:
        return {"exists": False, "path": "build/test-results/test"}

    total = failed = errors = skipped = 0
    time_secs = 0.0
    failures: list[dict[str, str]] = []
    for xml_path in xml_files:
        try:
            root = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue
        total += int(root.attrib.get("tests", 0))
        failed += int(root.attrib.get("failures", 0))
        errors += int(root.attrib.get("errors", 0))
        skipped += int(root.attrib.get("skipped", 0))
        time_secs += float(root.attrib.get("time", 0.0))
        for tc in root.findall("testcase"):
            fail_node = tc.find("failure") or tc.find("error")
            if fail_node is not None:
                failures.append(
                    {
                        "classname": tc.attrib.get("classname", ""),
                        "name": tc.attrib.get("name", ""),
                        "message": fail_node.attrib.get("message", "")[:300],
                    }
                )
    return {
        "exists": True,
        "ok": failed == 0 and errors == 0,
        "suites": len(xml_files),
        "total": total,
        "passed": total - failed - errors - skipped,
        "failed": failed,
        "errors": errors,
        "skipped": skipped,
        "time_secs": round(time_secs, 3),
        "failures": failures[:20],
    }


@_tool(tags={"code", "gradle"})
def get_gradle_config() -> dict[str, Any]:
    """Read key-value properties from gradle.properties (versions, mod metadata, memory)."""
    cached = _cache_get("gradle_config")
    if cached is not None:
        return cached
    props_file = REPO_ROOT / "gradle.properties"
    if not props_file.is_file():
        return {"exists": False, "properties": {}}
    props: dict[str, str] = {}
    for line in props_file.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        k, v = stripped.split("=", 1)
        props[k.strip()] = v.strip()
    return _cache_set("gradle_config", {"exists": True, "properties": props})


@_tool(tags={"meta", "observability"})
def list_tool_tags() -> dict[str, Any]:
    """List semantic tags and the tools registered under each tag for CodeMode discovery."""
    by_tag: dict[str, list[str]] = {}
    for tool_name, tags in sorted(_TOOL_TAGS.items()):
        for tag in tags:
            by_tag.setdefault(tag, []).append(tool_name)
    return {
        "ok": True,
        "tagged_tools": len(_TOOL_TAGS),
        "tags": {k: sorted(v) for k, v in sorted(by_tag.items())},
    }


@_tool(tags={"meta", "observability"})
def get_bridge_metrics() -> dict[str, Any]:
    """Return in-memory call counts, error counts, avg/max latencies, and TTL cache statistics."""
    summary: dict[str, dict[str, Any]] = {}
    for name, m in sorted(_METRICS.items()):
        calls = m["calls"]
        summary[name] = {
            "calls": calls,
            "errors": m["errors"],
            "avg_ms": round(m["total_ms"] / calls, 2) if calls else 0.0,
            "max_ms": round(m["max_ms"], 2),
        }
    return {
        "ok": True,
        "cache": {
            "ttl_secs": _CACHE_TTL_SECS,
            "entries": len(_CACHE),
            **_CACHE_STATS,
        },
        "active_live_sessions": len(_LIVE_SESSIONS),
        "tools": summary,
    }


@_tool(tags={"meta", "observability"})
def clear_bridge_cache() -> dict[str, Any]:
    """Invalidate all in-memory TTL cache entries in the MCP bridge."""
    cleared = _invalidate_cache()
    return {"ok": True, "cleared_entries": cleared}


@_tool(tags={"meta", "diagnostics"})
def capture_audit_snapshot(label: str = "baseline") -> dict[str, Any]:
    """Capture a lightweight project audit snapshot (tests, pitest, registries, assets, datapacks, visual SSIM) for diffing."""
    reg = get_registered_content()
    gt = list_gametests()
    ut = list_unit_tests()
    assets = audit_mod_assets()
    dp = list_datapack_content()
    vis = check_visual_regression()
    cfg = get_gradle_config()
    junit = get_latest_junit_report()
    pitest = get_latest_pitest_report()
    snap = {
        "label": label,
        "captured_at": round(time.time(), 2),
        "mod_version": cfg.get("properties", {}).get("mod_version", ""),
        "gametests_count": gt.get("count", 0),
        "unit_tests_count": ut.get("count", 0),
        "junit_passed": junit.get("passed", 0),
        "pitest_score_pct": pitest.get("mutation_score_pct"),
        "registries": {k: len(v) for k, v in reg.items()},
        "asset_audit_ok": assets.get("ok", False),
        "lang_en": assets.get("lang", {}).get("en_count", 0),
        "lang_es": assets.get("lang", {}).get("es_count", 0),
        "datapack_total": dp.get("total", 0),
        "external_datapack_total": dp.get("external_total", 0),
        "visual_ok": vis.get("ok", False),
        "visual_ssim": vis.get("ssim"),
    }
    _AUDIT_SNAPSHOTS[label] = snap
    return {"ok": True, "snapshot": snap}


@_tool(tags={"meta", "diagnostics"})
def diff_audit_snapshots(before_label: str, after_label: str) -> dict[str, Any]:
    """Compare two previously captured audit snapshots and report numeric/boolean deltas."""
    if before_label not in _AUDIT_SNAPSHOTS:
        return {"ok": False, "error": f"Unknown snapshot '{before_label}'", "available": sorted(_AUDIT_SNAPSHOTS)}
    if after_label not in _AUDIT_SNAPSHOTS:
        return {"ok": False, "error": f"Unknown snapshot '{after_label}'", "available": sorted(_AUDIT_SNAPSHOTS)}
    b = _AUDIT_SNAPSHOTS[before_label]
    a = _AUDIT_SNAPSHOTS[after_label]
    deltas: dict[str, Any] = {}
    for key in (
        "gametests_count",
        "unit_tests_count",
        "junit_passed",
        "lang_en",
        "lang_es",
        "datapack_total",
        "external_datapack_total",
    ):
        diff = a[key] - b[key]
        if diff != 0:
            deltas[key] = {"before": b[key], "after": a[key], "delta": diff}
    for key in ("visual_ssim", "pitest_score_pct", "mod_version"):
        if b.get(key) != a.get(key):
            deltas[key] = {"before": b.get(key), "after": a.get(key)}
    reg_deltas: dict[str, Any] = {}
    for rk in sorted(set(b["registries"]) | set(a["registries"])):
        bv = b["registries"].get(rk, 0)
        av = a["registries"].get(rk, 0)
        if av != bv:
            reg_deltas[rk] = {"before": bv, "after": av, "delta": av - bv}
    if reg_deltas:
        deltas["registries"] = reg_deltas
    return {
        "ok": True,
        "before": before_label,
        "after": after_label,
        "changed": bool(deltas),
        "deltas": deltas,
    }


def _parse_csv_tags(match: re.Match[str] | None) -> list[str]:
    if not match:
        return []
    return [item.strip() for item in match.group(1).split(",") if item.strip() and item.strip().lower() != "none"]


def _parse_bool_field(pattern: str, raw: str) -> bool | None:
    match = re.search(pattern, raw, re.IGNORECASE)
    return (match.group(1).lower() == "true") if match else None


def _parse_hive_status_output(raw: str) -> dict[str, Any]:
    """Parse '/mycelial status' output from MycelialCommands.java into structured fields."""
    threat_m = re.search(r"Threat:\s*([0-9]+(?:[.,][0-9]+)?)%", raw)
    tier_m = re.search(r"Threat:\s*[0-9]+(?:[.,][0-9]+)?%\s*\[([A-Z_]+)\]", raw) or re.search(r"Tier:\s*([A-Z_]+)", raw)
    milestone_m = re.search(r"Milestone:\s*([0-9]+)%", raw)
    links_m = re.search(r"Links:\s*([0-9]+)", raw)
    sup_m = re.search(r"Suppressed:\s*([0-9]+)s", raw)
    rem_m = re.search(r"Remission:\s*([0-9]+)s", raw)
    nearest_m = re.search(r"NearestNode:\s*([^|\n]+)", raw)
    nearest_raw = nearest_m.group(1).strip() if nearest_m else ""
    return {
        "threat_pct": float(threat_m.group(1).replace(",", ".")) if threat_m else None,
        "tier": tier_m.group(1) if tier_m else None,
        "milestone_pct": int(milestone_m.group(1)) if milestone_m else None,
        "protected": _parse_bool_field(r"Protected:\s*(true|false)", raw),
        "traits": _parse_csv_tags(re.search(r"Traits:\s*([^|\n]+)", raw)),
        "tier2_traits": _parse_csv_tags(re.search(r"Tier2:\s*([^|\n]+)", raw)),
        "synergies": _parse_csv_tags(re.search(r"Synergies:\s*([^|\n]+)", raw)),
        "links": int(links_m.group(1)) if links_m else None,
        "suppressed_secs": int(sup_m.group(1)) if sup_m else None,
        "nearest_node": nearest_raw if nearest_raw and nearest_raw.lower() != "none" else None,
        "breached": _parse_bool_field(r"Breached:\s*(true|false)", raw),
        "matriarch_defeated": _parse_bool_field(r"MatriarchDefeated:\s*(true|false)", raw),
        "remission_secs": int(rem_m.group(1)) if rem_m else None,
        "raw": raw,
    }


@_tool(tags={"rcon", "hive", "lifecycle"})
async def get_live_hive_status(
    host: str = "127.0.0.1",
    port: int = 25575,
    password: str = "mycelialtest",
) -> dict[str, Any]:
    """Run '/mycelial status' via RCON and parse threat, tier, milestone, zone, traits, synergies, links, nearest node, breach, and remission."""
    res = await rcon_command("mycelial status", host=host, port=port, password=password)
    if not res.get("ok"):
        return res
    return {"ok": True, **_parse_hive_status_output(str(res.get("response", "")))}


def _parse_colony_status_output(raw: str) -> dict[str, Any]:
    """Parse '/mycelial colony status' output from MycelialCommands.java into structured fields."""
    empty_m = re.search(r"No Sovereign Colony found within \d+ blocks\. Total in dim:\s*([0-9]+)", raw)
    if empty_m:
        return {
            "found": False,
            "total_in_dim": int(empty_m.group(1)),
            "raw": raw,
        }
    nexus_m = re.search(r"Nexus=\s*([^|\n]+)", raw)
    tier_m = re.search(r"Tier=\s*([0-9]+)", raw)
    radius_m = re.search(r"Radius=\s*([0-9]+)m", raw)
    pop_m = re.search(r"Pop=\s*([0-9]+)/([0-9]+)", raw)
    bio_m = re.search(r"Biomass=\s*([0-9]+)", raw)
    morale_m = re.search(r"Morale=\s*([0-9]+(?:[.,][0-9]+)?)%", raw)
    xp_m = re.search(r"DomainXP=\s*([0-9]+)", raw)
    traits_m = re.search(r"Traits=\s*(0x[0-9A-Fa-f]+)", raw)
    outposts_m = re.search(r"Outposts=\s*([0-9]+)", raw)
    orders_m = re.search(r"Orders=\s*([0-9]+)", raw)
    return {
        "found": nexus_m is not None,
        "nexus": nexus_m.group(1).strip() if nexus_m else None,
        "tier": int(tier_m.group(1)) if tier_m else None,
        "radius_m": int(radius_m.group(1)) if radius_m else None,
        "population": int(pop_m.group(1)) if pop_m else None,
        "population_cap": int(pop_m.group(2)) if pop_m else None,
        "biomass": int(bio_m.group(1)) if bio_m else None,
        "morale_pct": float(morale_m.group(1).replace(",", ".")) if morale_m else None,
        "domain_xp": int(xp_m.group(1)) if xp_m else None,
        "trait_mask_hex": traits_m.group(1) if traits_m else None,
        "outposts": int(outposts_m.group(1)) if outposts_m else None,
        "orders": int(orders_m.group(1)) if orders_m else None,
        "raw": raw,
    }


def _parse_cycle_status_output(raw: str) -> dict[str, Any]:
    """Parse '/mycelial cycle status' output from MycelialCommands.java into structured fields."""
    cycle_m = re.search(r"Cycle=\s*([0-9]+)", raw)
    mut_m = re.search(r"Mutators=\[([^\]]*)\]", raw)
    comp_m = re.search(r"Completed=\s*([0-9]+)", raw)
    best_m = re.search(r"BestScore=\s*([0-9]+)\s*\(Cycle\s+([0-9]+),\s*Last=\s*([0-9]+)\s+([^\)\s]+)\)", raw)
    mutators = _parse_csv_tags(mut_m) if mut_m else []
    return {
        "unlocked": _parse_bool_field(r"Unlocked=\s*(true|false)", raw),
        "active": _parse_bool_field(r"Active=\s*(true|false)", raw),
        "cycle_number": int(cycle_m.group(1)) if cycle_m else None,
        "mutators": mutators,
        "completed_cycles": int(comp_m.group(1)) if comp_m else None,
        "best_score": int(best_m.group(1)) if best_m else None,
        "best_cycle": int(best_m.group(2)) if best_m else None,
        "last_score": int(best_m.group(3)) if best_m else None,
        "last_grade": best_m.group(4) if best_m else None,
        "raw": raw,
    }


@_tool(tags={"rcon", "hive", "lifecycle"})
async def get_live_colony_status(
    host: str = "127.0.0.1",
    port: int = 25575,
    password: str = "mycelialtest",
    include_cycle: bool = True,
    timeout_secs: float = 10.0,
) -> dict[str, Any]:
    """Run '/mycelial colony status' (and optional '/mycelial cycle status') via RCON and return parsed telemetry."""
    col_res = await rcon_command("mycelial colony status", host=host, port=port, password=password, timeout_secs=timeout_secs)
    if not col_res.get("ok"):
        return col_res
    out: dict[str, Any] = {
        "ok": True,
        "colony": _parse_colony_status_output(str(col_res.get("response", ""))),
    }
    if include_cycle:
        cyc_res = await rcon_command("mycelial cycle status", host=host, port=port, password=password, timeout_secs=timeout_secs)
        if cyc_res.get("ok"):
            out["cycle"] = _parse_cycle_status_output(str(cyc_res.get("response", "")))
        else:
            out["cycle"] = {"ok": False, "error": cyc_res.get("error", "RCON cycle status failed")}
    return out


@_tool(tags={"rcon", "diagnostics", "lifecycle"})
async def run_live_verify_scenario(
    scenario: str = "all",
    host: str = "127.0.0.1",
    port: int = 25575,
    password: str = "mycelialtest",
    timeout_secs: float = 30.0,
) -> dict[str, Any]:
    """Run '/mycelial verify <scenario>' via RCON and return structured pass/fail status."""
    if not re.fullmatch(r"[a-z0-9_]+", scenario):
        return {"ok": False, "scenario": scenario, "error": "Invalid scenario name"}
    res = await rcon_command(f"mycelial verify {scenario}", host=host, port=port, password=password, timeout_secs=timeout_secs)
    if not res.get("ok"):
        return res
    raw = str(res.get("response", ""))
    passed = ("PASSED" in raw or "PASS" in raw) and ("FAILED" not in raw and "FAIL" not in raw)
    return {"ok": passed, "scenario": scenario, "passed": passed, "raw": raw}


@_tool(tags={"lifecycle", "rcon", "diagnostics"})
async def start_smoke_server(
    world_type: str = "flat",
    rcon_port: int = 0,
    server_port: int = 0,
    enable_diagnostics: bool = False,
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Start an isolated headless smoke server under artifacts/diagnostics/ with RCON enabled (and optional -Pdiagnostics)."""
    session_id = f"live-{uuid.uuid4().hex[:8]}"
    run_dir = REPO_ROOT / "artifacts" / "diagnostics" / f"live-smoke-{session_id}"
    run_dir.mkdir(parents=True, exist_ok=True)
    r_port = rcon_port or _find_free_port()
    s_port = server_port or _find_free_port()
    while s_port == r_port:
        s_port = _find_free_port()
    password = f"mycelial-{uuid.uuid4().hex[:6]}"
    (run_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    props = [
        "server-ip=127.0.0.1",
        f"server-port={s_port}",
        "enable-rcon=true",
        f"rcon.port={r_port}",
        f"rcon.password={password}",
        "online-mode=false",
        "allow-nether=false",
        "spawn-protection=0",
        "max-tick-time=-1",
        "view-distance=2",
        "simulation-distance=2",
    ]
    if world_type == "flat":
        props.append(
            'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}'
        )
        props.append("level-type=minecraft\\:flat")
    (run_dir / "server.properties").write_text("\n".join(props) + "\n", encoding="utf-8")

    log_file = run_dir / "bridge-server.log"
    log_fh = log_file.open("w+b")
    cmd = [
        "cmd.exe",
        "/c",
        "gradlew.bat",
        "runSmokeServer",
        f"-PsmokeRunDirectory={run_dir}",
        "--max-workers=1",
    ]
    if enable_diagnostics:
        cmd.append("-Pdiagnostics")
    try:
        proc = subprocess.Popen(
            cmd,
            cwd=str(REPO_ROOT),
            env=_build_gradle_env(),
            stdin=subprocess.DEVNULL,
            stdout=log_fh,
            stderr=subprocess.STDOUT,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
    except OSError as exc:
        log_fh.close()
        shutil.rmtree(run_dir, ignore_errors=True)
        return {"ok": False, "session_id": session_id, "error": f"Failed to launch smoke server: {exc}"}
    sess = {
        "session_id": session_id,
        "server_proc": proc,
        "log_fh": log_fh,
        "log_path": log_file.relative_to(REPO_ROOT).as_posix(),
        "run_dir": run_dir,
        "rcon_port": r_port,
        "server_port": s_port,
        "rcon_password": password,
        "started_at": time.monotonic(),
        "client_name": None,
        "mct_home": "",
    }
    _LIVE_SESSIONS[session_id] = sess

    deadline = time.monotonic() + timeout_secs
    ready = False
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            break
        text = log_file.read_text(encoding="utf-8", errors="replace")
        if "RCON running on" in text:
            ready = True
            break
        await asyncio.sleep(0.5)

    if not ready:
        _kill_process_tree(proc)
        log_fh.close()
        _LIVE_SESSIONS.pop(session_id, None)
        tail = "\n".join(log_file.read_text(encoding="utf-8", errors="replace").splitlines()[-40:])
        shutil.rmtree(run_dir, ignore_errors=True)
        return {"ok": False, "session_id": session_id, "error": "Smoke server failed to reach RCON ready", "log_tail": tail}

    return {
        "ok": True,
        "session_id": session_id,
        "server_pid": proc.pid,
        "server_port": s_port,
        "rcon_port": r_port,
        "rcon_password": password,
        "log_path": sess["log_path"],
    }


@_tool(tags={"lifecycle", "diagnostics"})
def smoke_server_status(session_id: str = "") -> dict[str, Any]:
    """Inspect active bridge-owned smoke server sessions (PID, ports, liveness, uptime)."""
    if session_id:
        sess = _LIVE_SESSIONS.get(session_id)
        if not sess:
            return {"ok": False, "exists": False, "session_id": session_id, "active_sessions": sorted(_LIVE_SESSIONS)}
        proc = sess["server_proc"]
        return {
            "ok": True,
            "exists": True,
            "session_id": session_id,
            "running": proc.poll() is None,
            "server_pid": proc.pid,
            "server_port": sess["server_port"],
            "rcon_port": sess["rcon_port"],
            "client_name": sess.get("client_name"),
            "uptime_secs": round(time.monotonic() - sess["started_at"], 1),
            "log_path": sess["log_path"],
        }
    return {
        "ok": True,
        "count": len(_LIVE_SESSIONS),
        "sessions": [
            {
                "session_id": sid,
                "running": s["server_proc"].poll() is None,
                "server_pid": s["server_proc"].pid,
                "server_port": s["server_port"],
                "rcon_port": s["rcon_port"],
                "client_name": s.get("client_name"),
            }
            for sid, s in sorted(_LIVE_SESSIONS.items())
        ],
    }


@_tool(tags={"lifecycle", "rcon"})
async def stop_smoke_server(session_id: str = "", cleanup_dir: bool = False) -> dict[str, Any]:
    """Gracefully stop a bridge-owned smoke server via RCON 'stop' (with process-tree kill fallback)."""
    sid = session_id or (next(iter(_LIVE_SESSIONS)) if len(_LIVE_SESSIONS) == 1 else "")
    sess = _LIVE_SESSIONS.pop(sid, None)
    if not sess:
        return {"ok": False, "error": f"Session not found: '{session_id}'", "active_sessions": sorted(_LIVE_SESSIONS)}
    proc = sess["server_proc"]
    graceful = False
    if proc.poll() is None:
        await rcon_command("stop", host="127.0.0.1", port=sess["rcon_port"], password=sess["rcon_password"])
        try:
            await asyncio.to_thread(proc.wait, 25)
            graceful = True
        except subprocess.TimeoutExpired:
            _kill_process_tree(proc)
    try:
        sess["log_fh"].close()
    except OSError:
        pass
    if cleanup_dir:
        shutil.rmtree(sess["run_dir"], ignore_errors=True)
    return {"ok": True, "session_id": sid, "graceful_rcon_stop": graceful, "cleaned_dir": cleanup_dir}


@_tool(tags={"lifecycle", "mcpilot", "rcon"})
async def start_live_session(
    client_name: str = "mycelial-1211",
    mct_home: str = "",
    world_type: str = "flat",
    enable_diagnostics: bool = False,
    launch_client: bool = True,
    timeout_secs: int = 120,
) -> dict[str, Any]:
    """Coordinate an isolated smoke server + optional MC Pilot client launch in a single session."""
    srv = await start_smoke_server(
        world_type=world_type,
        enable_diagnostics=enable_diagnostics,
        timeout_secs=timeout_secs,
    )
    if not srv.get("ok"):
        return srv
    sid = srv["session_id"]
    client_res: dict[str, Any] | None = None
    if launch_client:
        client_res = await mcpilot_command(["client", "launch", client_name, "--no-server"], mct_home=mct_home, timeout_secs=120)
        if not client_res.get("ok"):
            cleanup = await stop_smoke_server(session_id=sid, cleanup_dir=True)
            return {
                "ok": False,
                "session_id": sid,
                "error": "MC Pilot client launch failed; smoke server stopped",
                "server": srv,
                "client": client_res,
                "cleanup": cleanup,
            }
        if sid in _LIVE_SESSIONS:
            _LIVE_SESSIONS[sid]["client_name"] = client_name
            _LIVE_SESSIONS[sid]["mct_home"] = mct_home
    return {
        "ok": True,
        "session_id": sid,
        "server": srv,
        "client": client_res,
    }


@_tool(tags={"lifecycle", "mcpilot", "rcon"})
async def stop_live_session(session_id: str = "", cleanup_dir: bool = False) -> dict[str, Any]:
    """Stop both the MC Pilot client (if launched by this session) and the isolated smoke server."""
    sid = session_id or (next(iter(_LIVE_SESSIONS)) if len(_LIVE_SESSIONS) == 1 else "")
    sess = _LIVE_SESSIONS.get(sid)
    if not sess:
        return {"ok": False, "error": f"Session not found: '{session_id}'", "active_sessions": sorted(_LIVE_SESSIONS)}
    client_res: dict[str, Any] | None = None
    if sess.get("client_name"):
        client_res = await stop_mcpilot_client(client_name=sess["client_name"], mct_home=sess.get("mct_home", ""))
    srv_res = await stop_smoke_server(session_id=sid, cleanup_dir=cleanup_dir)
    return {
        "ok": bool(srv_res.get("ok", False) and (client_res.get("ok", False) if client_res is not None else True)),
        "session_id": sid,
        "client_stop": client_res,
        "server_stop": srv_res,
    }


def _parse_spark_tps_output(text: str) -> dict[str, Any]:
    """Parse spark '/spark tps' chat history output into structured TPS, MSPT, and CPU metrics."""
    parsed: dict[str, Any] = {"ok": False}
    # MC Pilot returns chat history as JSON objects with a `plain` field. Its
    # stdout can be truncated at the end, so extract complete `plain` values
    # from the response instead of requiring the entire JSON document to parse.
    plain_messages: list[str] = []
    for match in re.finditer(r'"plain"\s*:\s*("(?:\\.|[^"\\])*")', text):
        try:
            plain_messages.append(json.loads(match.group(1)))
        except json.JSONDecodeError:
            continue
    if plain_messages:
        text = "\n".join(
            re.sub(r"^\s*\[[^\]]+\]\s*", "", message)
            for message in plain_messages
        )

    tps_m = re.search(
        r"TPS from last 5s, 10s, 1m[^:]*:\s*\*?([0-9.]+),\s*\*?([0-9.]+),\s*\*?([0-9.]+)",
        text,
        re.IGNORECASE,
    )
    if tps_m:
        parsed["tps_5s"] = float(tps_m.group(1))
        parsed["tps_10s"] = float(tps_m.group(2))
        parsed["tps_1m"] = float(tps_m.group(3))
        parsed["ok"] = True
    mspt_m = re.search(
        r"Tick durations \(min/med/95%ile/max\s*ms\)[^:]*:\s*([0-9.]+)/([0-9.]+)/([0-9.]+)/([0-9.]+)",
        text,
        re.IGNORECASE,
    )
    if mspt_m:
        parsed["mspt_min"] = float(mspt_m.group(1))
        parsed["mspt_med"] = float(mspt_m.group(2))
        parsed["mspt_p95"] = float(mspt_m.group(3))
        parsed["mspt_max"] = float(mspt_m.group(4))
        parsed["ok"] = True
    def cpu_percent_for(label: str) -> float | None:
        target = f"({label})"
        for line in text.splitlines():
            if target not in line.lower():
                continue
            values = re.findall(r"([0-9.]+)%", line)
            if not values:
                continue
            # Older spark output placed process and system on one line; use
            # the percentage immediately before each label in that format.
            if "(process)" in line.lower() and "(system)" in line.lower():
                label_match = re.search(rf"([0-9.]+)%\s*\({label}\)", line, re.IGNORECASE)
                if label_match:
                    return float(label_match.group(1))
            # Current spark output lists 5s/1m/15m values on one labeled line.
            return float(values[0])
        return None

    cpu_process = cpu_percent_for("process")
    cpu_system = cpu_percent_for("system")
    if cpu_process is not None:
        parsed["cpu_process_pct"] = cpu_process
    if cpu_system is not None:
        parsed["cpu_system_pct"] = cpu_system
    return parsed


@_tool(tags={"diagnostics", "mcpilot", "lifecycle"})
async def collect_spark_tps(
    client_name: str = "mycelial-1211",
    mct_home: str = "",
    wait_secs: float = 1.5,
) -> dict[str, Any]:
    """Run 'spark tps' via MC Pilot chat command and parse TPS/MSPT/CPU from chat history."""
    send_res = await mcpilot_command(
        ["--client", client_name, "chat", "command", "spark tps"],
        mct_home=mct_home,
        timeout_secs=30,
    )
    if not send_res.get("ok"):
        return {"ok": False, "error": "Failed to send spark tps command", "detail": send_res}
    await asyncio.sleep(max(0.2, wait_secs))
    hist_res = await mcpilot_command(
        ["--client", client_name, "chat", "history", "--last", "16"],
        mct_home=mct_home,
        timeout_secs=30,
    )
    if not hist_res.get("ok"):
        return {"ok": False, "error": "Failed to read MC Pilot chat history", "detail": hist_res}
    raw = hist_res.get("stdout_tail", "")
    parsed = _parse_spark_tps_output(raw)
    parsed["raw_tail"] = raw[-1000:]
    return parsed


@_tool(tags={"visual", "mcpilot", "lifecycle"})
async def capture_mcpilot_screenshot(
    client_name: str = "mycelial-1211",
    output_path: str = "",
    mct_home: str = "",
) -> dict[str, Any]:
    """Capture a screenshot via MC Pilot into artifacts/diagnostics/live-reports/ and return dimensions."""
    reports_dir = REPO_ROOT / "artifacts" / "diagnostics" / "live-reports"
    reports_dir.mkdir(parents=True, exist_ok=True)
    rel_out = output_path or f"artifacts/diagnostics/live-reports/shot-{uuid.uuid4().hex[:8]}.png"
    target = _resolve_repo_path(rel_out)
    target.parent.mkdir(parents=True, exist_ok=True)
    res = await mcpilot_command(
        ["--client", client_name, "screenshot", "--output", str(target)],
        mct_home=mct_home,
        timeout_secs=45,
    )
    if not res.get("ok") or not target.is_file():
        return {"ok": False, "path": rel_out, "error": "Screenshot capture failed", "detail": res}
    from PIL import Image

    with Image.open(target) as im:
        w, h = im.size
    return {"ok": True, "path": target.relative_to(REPO_ROOT).as_posix(), "width": w, "height": h}


_LIVE_SCENARIO_PRESETS: dict[str, list[str]] = {
    "progression_ramp": [
        "mycelial reset",
        "mycelial stimulus MELEE 120",
        "mycelial status",
        "mycelial stimulus FIRE 120",
        "mycelial status",
        "mycelial stimulus PROJECTILE 150",
        "mycelial status",
    ],
    "swarm_load": [
        "mycelial verify hive_ai",
        "mycelial verify tactical_ai",
        "mycelial verify chimera",
        "mycelial status",
    ],
    "diplomacy_and_memory": [
        "mycelial diplomacy AMBER 40",
        "mycelial schism CRIMSON",
        "mycelial habits",
        "mycelial verify memory",
    ],
    "full_smoke_matrix": [
        "mycelial status",
        "mycelial verify all",
    ],
    "homeostasis_and_dimension": [
        "mycelial homeostasis status",
        "mycelial verify field_ai",
        "mycelial verify homeostasis",
        "mycelial verify dimension_progression",
        "mycelial status",
    ],
    "colony_and_dialects": [
        "mycelial colony status",
        "mycelial verify colony",
        "mycelial status",
    ],
    "cycles_and_endgame": [
        "mycelial cycle status",
        "mycelial verify cycles",
        "mycelial verify dimension_progression",
        "mycelial status",
    ],
}


_IGNORED_JVM_MARKERS = ("GradleDaemon", "GradleWrapperMain", "jdk.jcmd", "JCmd", "KotlinCompileDaemon")
_MINECRAFT_JVM_MARKERS = ("BootstrapLauncher", "ServerMain", "cpw.mods", "net.minecraft", "net.neoforged")


def _pick_minecraft_jvm_pid(processes: list[dict[str, Any]]) -> int | None:
    """Pick the live Minecraft/NeoForge JVM PID from jcmd -l output, filtering Gradle and jcmd itself."""
    candidates: list[tuple[int, str]] = []
    for proc in processes:
        pid = proc.get("pid")
        main_class = str(proc.get("main_class", "") or "").strip()
        if not isinstance(pid, int) or not main_class:
            continue
        if any(marker in main_class for marker in _IGNORED_JVM_MARKERS):
            continue
        if any(marker in main_class for marker in _MINECRAFT_JVM_MARKERS):
            return pid
        candidates.append((pid, main_class))
    return candidates[0][0] if candidates else None


@_tool(tags={"lifecycle", "rcon", "diagnostics"})
async def run_live_scenario(
    preset: str = "progression_ramp",
    custom_commands: list[str] | None = None,
    session_id: str = "",
    rcon_port: int = 0,
    rcon_password: str = "mycelialtest",
    enable_diagnostics: bool = False,
    capture_perf: bool = False,
    jfr_duration_secs: int = 10,
    capture_screenshot: bool = False,
    client_name: str = "mycelial-1211",
    mct_home: str = "",
) -> dict[str, Any]:
    """Run a live RCON scenario preset (or custom commands), optionally collect spark/JFR + screenshot, and save JSON report."""
    if preset == "custom":
        commands = list(custom_commands or [])
        if not commands:
            return {"ok": False, "error": "custom_commands required when preset='custom'"}
    elif preset in _LIVE_SCENARIO_PRESETS:
        commands = list(_LIVE_SCENARIO_PRESETS[preset])
    else:
        return {"ok": False, "error": f"Unknown preset '{preset}'. Expected: {sorted(_LIVE_SCENARIO_PRESETS)} or 'custom'"}

    if session_id and session_id not in _LIVE_SESSIONS:
        return {
            "ok": False,
            "error": f"Unknown session_id '{session_id}'",
            "active_sessions": sorted(_LIVE_SESSIONS),
        }

    started = time.monotonic()
    run_id = f"{preset}-{time.strftime('%Y%m%d-%H%M%S')}-{uuid.uuid4().hex[:4]}"
    auto_started_sid = ""
    active_port = rcon_port
    active_pass = rcon_password
    active_client = ""

    if session_id:
        sess = _LIVE_SESSIONS[session_id]
        active_port = sess["rcon_port"]
        active_pass = sess["rcon_password"]
        active_client = sess.get("client_name") or ""
    elif active_port == 0:
        launch_cli = capture_screenshot or (capture_perf and enable_diagnostics)
        started_sess = await start_live_session(
            client_name=client_name,
            mct_home=mct_home,
            enable_diagnostics=enable_diagnostics,
            launch_client=launch_cli,
        )
        if not started_sess.get("ok"):
            return {"ok": False, "run_id": run_id, "error": "Failed to start ephemeral live session", "detail": started_sess}
        auto_started_sid = started_sess["session_id"]
        active_port = started_sess["server"]["rcon_port"]
        active_pass = started_sess["server"]["rcon_password"]
        active_client = client_name if launch_cli else ""

    try:
        jfr_dur = max(10, min(3600, int(jfr_duration_secs)))
        jfr_pid: int | None = None
        jfr_jvms_count = 0
        jfr_jvms_ok = False
        jfr_start_res: dict[str, Any] | None = None
        jfr_abs_path = ""
        if capture_perf and not active_client:
            jvms = await list_java_processes()
            jfr_jvms_ok = bool(jvms.get("ok", False))
            jfr_jvms_count = int(jvms.get("count", 0))
            jfr_pid = _pick_minecraft_jvm_pid(jvms.get("processes", []))
            if jfr_pid is not None:
                jfr_start_res = await run_jfr(process_id=jfr_pid, action="Start", duration_seconds=jfr_dur)
                if jfr_start_res.get("ok"):
                    m_jfr = re.search(r"JFR output path:\s*(.+\.jfr)", str(jfr_start_res.get("stdout_tail", "")))
                    if m_jfr:
                        jfr_abs_path = m_jfr.group(1).strip()

        batch_res = await run_rcon_batch(commands, port=active_port, password=active_pass)
        hive_status = await get_live_hive_status(port=active_port, password=active_pass)

        perf_data: dict[str, Any] | None = None
        if capture_perf:
            if active_client:
                perf_data = {"source": "spark", **(await collect_spark_tps(client_name=active_client, mct_home=mct_home))}
            elif jfr_pid is None:
                perf_data = {
                    "source": "jfr_fallback",
                    "ok": jfr_jvms_ok,
                    "jfr_recorded": False,
                    "pid": None,
                    "jvms_count": jfr_jvms_count,
                }
            else:
                jfr_recorded = False
                rel_jfr = ""
                summary_ok = False
                summary_tail = ""
                if jfr_start_res and jfr_start_res.get("ok"):
                    await run_jfr(process_id=jfr_pid, action="Stop", duration_seconds=jfr_dur)
                    if jfr_abs_path:
                        jfr_file = Path(jfr_abs_path)
                        if jfr_file.is_file():
                            try:
                                rel_jfr = jfr_file.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
                            except ValueError:
                                rel_jfr = ""
                            if rel_jfr:
                                jfr_sum = await summarize_jfr(rel_jfr)
                                summary_ok = bool(jfr_sum.get("ok", False))
                                summary_tail = str(jfr_sum.get("stdout_tail", ""))[-1000:]
                                jfr_recorded = summary_ok
                perf_data = {
                    "source": "jfr_fallback",
                    "ok": jfr_recorded,
                    "jfr_recorded": jfr_recorded,
                    "pid": jfr_pid,
                    "jfr_path": rel_jfr,
                    "summary_ok": summary_ok,
                    "summary_tail": summary_tail,
                    "jvms_count": jfr_jvms_count,
                }

        shot_data: dict[str, Any] | None = None
        if capture_screenshot and active_client:
            shot_data = await capture_mcpilot_screenshot(
                client_name=active_client,
                output_path=f"artifacts/diagnostics/live-reports/{run_id}.png",
                mct_home=mct_home,
            )

        failed_steps = [
            r for r in batch_res.get("results", [])
            if "FAIL" in r.get("response", "") or "ERROR" in r.get("response", "")
        ]
        report: dict[str, Any] = {
            "ok": bool(batch_res.get("ok")) and not failed_steps,
            "run_id": run_id,
            "preset": preset,
            "duration_secs": round(time.monotonic() - started, 2),
            "commands_run": len(batch_res.get("results", [])),
            "failed_steps": failed_steps,
            "hive_status": hive_status,
            "perf": perf_data,
            "screenshot": shot_data,
            "results": batch_res.get("results", []),
        }
        reports_dir = REPO_ROOT / "artifacts" / "diagnostics" / "live-reports"
        reports_dir.mkdir(parents=True, exist_ok=True)
        run_path = reports_dir / f"{run_id}.json"
        latest_path = reports_dir / "live-report-latest.json"
        serialized = json.dumps(report, indent=2)
        run_path.write_text(serialized, encoding="utf-8")
        latest_path.write_text(serialized, encoding="utf-8")
        report["report_path"] = run_path.relative_to(REPO_ROOT).as_posix()
        return report
    finally:
        if auto_started_sid:
            await stop_live_session(session_id=auto_started_sid, cleanup_dir=True)


@_tool(tags={"diagnostics", "reports"})
def get_latest_live_report() -> dict[str, Any]:
    """Read artifacts/diagnostics/live-reports/live-report-latest.json."""
    latest = REPO_ROOT / "artifacts" / "diagnostics" / "live-reports" / "live-report-latest.json"
    if not latest.is_file():
        return {"exists": False, "path": "artifacts/diagnostics/live-reports/live-report-latest.json"}
    try:
        data = json.loads(latest.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {"exists": True, "ok": False, "error": str(exc)}
    return {"exists": True, "ok": bool(data.get("ok", True)), "path": latest.relative_to(REPO_ROOT).as_posix(), "report": data}


@_tool(tags={"diagnostics", "reports"})
def compare_live_reports(before_path: str, after_path: str) -> dict[str, Any]:
    """Compare two live scenario JSON reports (deltas in duration, hive threat, spark TPS/MSPT, and screenshot SSIM)."""
    b_file = _resolve_repo_path(before_path)
    a_file = _resolve_repo_path(after_path)
    if not b_file.is_file() or not a_file.is_file():
        return {"ok": False, "error": f"Report file missing: before={b_file.is_file()}, after={a_file.is_file()}"}
    try:
        b = json.loads(b_file.read_text(encoding="utf-8"))
        a = json.loads(a_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {"ok": False, "error": str(exc)}

    deltas: dict[str, Any] = {
        "ok_before": b.get("ok"),
        "ok_after": a.get("ok"),
        "duration_delta_secs": round(float(a.get("duration_secs", 0)) - float(b.get("duration_secs", 0)), 2),
        "failed_steps_delta": len(a.get("failed_steps", [])) - len(b.get("failed_steps", [])),
    }
    b_hive, a_hive = b.get("hive_status") or {}, a.get("hive_status") or {}
    if b_hive.get("threat_pct") is not None and a_hive.get("threat_pct") is not None:
        deltas["threat_pct_delta"] = round(a_hive["threat_pct"] - b_hive["threat_pct"], 2)

    b_perf, a_perf = b.get("perf") or {}, a.get("perf") or {}
    for k in ("tps_10s", "mspt_med", "mspt_p95"):
        if b_perf.get(k) is not None and a_perf.get(k) is not None:
            deltas[f"{k}_delta"] = round(float(a_perf[k]) - float(b_perf[k]), 3)

    b_shot = (b.get("screenshot") or {}).get("path")
    a_shot = (a.get("screenshot") or {}).get("path")
    if b_shot and a_shot:
        vis = check_visual_regression(baseline_path=b_shot, current_path=a_shot)
        if "ssim" in vis:
            deltas["screenshot_ssim"] = vis["ssim"]
            deltas["screenshot_phash_dist"] = vis.get("phash_hamming_distance")

    return {"ok": True, "before": before_path, "after": after_path, "deltas": deltas}


# --- Read-only MCP resources ----------------------------------------------
# CodeMode collapses the tool surface, so a client that only browses resources
# used to see an empty list. These expose the bridge state and the workspace
# skills without side effects; nothing here mutates the repo or starts JVMs.

@mcp.resource(
    "mycelial://bridge/health",
    name="bridge_health",
    description="Runtime health of this bridge process (FastMCP/Python/JDK, tool count, instance identity).",
    mime_type="application/json",
)
def resource_bridge_health() -> str:
    return json.dumps(get_bridge_health(), indent=2, ensure_ascii=False)


@mcp.resource(
    "mycelial://bridge/tools",
    name="bridge_tools",
    description="Every bridge tool with its semantic tags (use get_schema for parameters).",
    mime_type="application/json",
)
def resource_bridge_tools() -> str:
    return json.dumps(
        {
            "count": len(_TOOL_TAGS),
            "tools": [
                {"name": name, "tags": tags}
                for name, tags in sorted(_TOOL_TAGS.items())
            ],
        },
        indent=2,
        ensure_ascii=False,
    )


@mcp.resource(
    "mycelial://bridge/tags",
    name="bridge_tags",
    description="Semantic tag groups and the tools registered under each tag.",
    mime_type="application/json",
)
def resource_bridge_tags() -> str:
    return json.dumps(list_tool_tags(), indent=2, ensure_ascii=False)


@mcp.resource(
    "mycelial://memory/tail",
    name="memory_tail",
    description="Last 60 lines of MEMORY.md: baseline and most recent project sessions.",
    mime_type="text/markdown",
)
def resource_memory_tail() -> str:
    tail = get_memory_tail(lines=60)
    header = f"<!-- MEMORY.md: {tail.get('total_lines', 0)} lines total -->\n\n"
    return header + str(tail.get("content", ""))


@mcp.resource(
    "mycelial://skills",
    name="skills_index",
    description="Workspace Open Agent Skills and their reference documents.",
    mime_type="application/json",
)
def resource_skills_index() -> str:
    return json.dumps(list_skills(), indent=2, ensure_ascii=False)


@mcp.resource(
    "mycelial://skills/{skill_name}",
    name="skill_guide",
    description="A workspace skill's SKILL.md (for example mycelial-codemode).",
    mime_type="text/markdown",
)
def resource_skill_guide(skill_name: str) -> str:
    guide = get_skill_guide(skill_name=skill_name)
    if not guide.get("ok"):
        return f"# {skill_name}\n\n{guide.get('error', 'Skill document not found')}\n"
    return str(guide.get("content", ""))


@mcp.resource(
    "mycelial://skills/{skill_name}/{document*}",
    name="skill_reference",
    description="A reference document inside a skill (for example references/deepseek-harness.md).",
    mime_type="text/markdown",
)
def resource_skill_reference(skill_name: str, document: str) -> str:
    guide = get_skill_guide(skill_name=skill_name, doc=document)
    if not guide.get("ok"):
        return f"# {skill_name}/{document}\n\n{guide.get('error', 'Skill document not found')}\n"
    return str(guide.get("content", ""))


if __name__ == "__main__":
    mcp.run()
