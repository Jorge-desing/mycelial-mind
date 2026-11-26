# Contribuir a Mycelial Mind

## Flujo de trabajo

1. Crea una rama descriptiva desde `main` (`feat/...`, `fix/...`, `docs/...`, `chore/...`).
2. Antes de abrir un Pull Request, verifica tus cambios en local:
   - **Assets Python** (modelos, texturas, partículas):
     ```powershell
     task verify:assets
     ```
   - **Pruebas Java, compilación y `@GameTest`** (o mediante FastMCP `CodeMode` `mycelial-mind-dev`):
     ```powershell
     .\gradlew.bat test build runGameTestServer
     ```
   - **Configuración de Renovate** (si editaste `renovate.json`):
     ```powershell
     task verify:renovate
     ```
3. Usa la plantilla de Pull Request y marca las casillas de verificación aplicables.
4. Todos los PRs hacia `main` deben pasar los checks obligatorios de **Verify Mycelial Mind** (`Validate automation config`, `Asset checks`, `Build and GameTests`).
5. Si modificas subsistemas del proyecto o flujos documentados, actualiza [MEMORY.md](MEMORY.md) (solo anexión al final) y revisa las skills en `.agents/skills/`.
