param(
    [Parameter(Mandatory = $true)][ValidateSet('Media', 'Hotel')][string]$Suite,
    [int]$PortOffset = 0
)
$ErrorActionPreference = 'Stop'
$slotRoot = Split-Path $PSScriptRoot -Parent
$suiteFolder = if ($Suite -eq 'Media') { 'DeThiSampleNew' } else { 'DeThi_SU26' }
$names = if ($Suite -eq 'Media') { @('UserService', 'PostService', 'MediaGateway') } else { @('RoomService', 'ReservationService', 'HotelGateway') }
$artifacts = if ($Suite -eq 'Media') { @('user', 'post', 'mediagateway') } else { @('room', 'reservation', 'hotelgateway') }
$ports = @((8081 + $PortOffset), (8082 + $PortOffset), (8080 + $PortOffset))
$runDir = Join-Path $slotRoot ".work/$Suite"
New-Item -ItemType Directory -Path $runDir -Force | Out-Null
foreach ($port in $ports) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "Port $port is in use. Stop the other suite first or select -PortOffset."
    }
}
$javaPath = (Get-Command java).Source
$started = @()
try {
    for ($index = 0; $index -lt 3; $index++) {
        $project = Join-Path $slotRoot "$suiteFolder/Code_PE/DE190468$($names[$index])"
        $jar = Join-Path $project "target/$($artifacts[$index])-1.0.0.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Missing $jar. Run mvn package from Slot 16 first." }
        $jar = (Resolve-Path -LiteralPath $jar).Path
        $saved = @{}
        $overrides = @{
            SERVER_PORT = [string]$ports[$index]
            PARENT_SERVICE_URL = "http://localhost:$($ports[0])"
            FIRST_SERVICE_URL = "http://localhost:$($ports[0])"
            SECOND_SERVICE_URL = "http://localhost:$($ports[1])"
        }
        try {
            foreach ($key in $overrides.Keys) {
                $saved[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
                [Environment]::SetEnvironmentVariable($key, $overrides[$key], 'Process')
            }
            $process = Start-Process -FilePath $javaPath -ArgumentList @('-Xmx256m', '-jar', "`"$jar`"") -WorkingDirectory $project -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runDir "$($artifacts[$index]).out.log") -RedirectStandardError (Join-Path $runDir "$($artifacts[$index]).err.log")
            $started += [pscustomobject]@{ id = $process.Id; jar = $jar; port = $ports[$index] }
        } finally {
            foreach ($key in $saved.Keys) { [Environment]::SetEnvironmentVariable($key, $saved[$key], 'Process') }
        }
    }
    $started | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDir 'pids.json') -Encoding UTF8
    $deadline = (Get-Date).AddSeconds(100)
    foreach ($port in $ports) {
        $ready = $false
        while ((Get-Date) -lt $deadline) {
            foreach ($record in $started) {
                if (-not (Get-Process -Id $record.id -ErrorAction SilentlyContinue)) {
                    throw "A service exited during startup. Check $runDir."
                }
            }
            try {
                $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$port/v3/api-docs" -TimeoutSec 3
                if ($response.StatusCode -eq 200) { $ready = $true; break }
            } catch { Start-Sleep -Milliseconds 500 }
        }
        if (-not $ready) { throw "Service on $port failed to start. Check $runDir." }
    }
    Write-Host "$Suite ready. Swagger: http://localhost:$($ports[2])/swagger-ui.html"
} catch {
    foreach ($record in $started) { Stop-Process -Id $record.id -ErrorAction SilentlyContinue }
    throw
}
