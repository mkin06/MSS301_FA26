param([string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env'))
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "Copy .env.example to .env and supply your local values first." }
$allowed = @('DB_HOST', 'DB_PORT', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET', 'JWT_EXPIRATION', 'REFRESH_EXPIRATION', 'SAGA_FAULT_INJECTION')
foreach ($line in Get-Content -LiteralPath $EnvFile) {
    $trimmed = $line.Trim()
    if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
    if ($trimmed -notmatch '^([A-Z_]+)=(.*)$') { throw 'Invalid .env entry; use KEY=value.' }
    $key = $matches[1]
    $value = $matches[2]
    if ($allowed -notcontains $key) { throw "Unsupported environment key: $key" }
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
}
if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD must be supplied.' }
if ([Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) { throw 'JWT_SECRET must have at least 32 bytes.' }
Write-Host 'Local environment loaded for this terminal; values were not printed.'
