param([Parameter(Mandatory = $true)][ValidateSet('Media', 'Hotel')][string]$Suite)
$ErrorActionPreference = 'Stop'
$slotRoot = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $slotRoot ".work/$Suite/pids.json"
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Host "No saved processes for $Suite"; return }
$records = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
foreach ($record in $records) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($record.id)" -ErrorAction SilentlyContinue
    $expectedJar = [System.IO.Path]::GetFullPath($record.jar)
    if ($process -and $process.Name -eq 'java.exe' -and $process.CommandLine.Contains($expectedJar)) {
        Stop-Process -Id $record.id
        Wait-Process -Id $record.id -Timeout 15 -ErrorAction SilentlyContinue
        Write-Host "Stopped $($record.id) on port $($record.port)"
    }
}
Remove-Item -LiteralPath $pidFile
