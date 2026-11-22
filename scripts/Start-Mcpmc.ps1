param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 25565,
    [string]$Username = "MycelialTester"
)

Write-Host "Starting MCPMC (Minecraft MCP Server) targeting ${HostName}:${Port} as ${Username}..." -ForegroundColor Cyan
npx -y mcpmc --host $HostName --port $Port --username $Username
