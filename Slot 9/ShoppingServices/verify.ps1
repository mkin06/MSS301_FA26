param([switch]$SwaggerOnly)
$ErrorActionPreference = 'Stop'
foreach ($service in @('product-service', 'inventory-service', 'order-service', 'api-gateway')) {
    Push-Location (Join-Path $PSScriptRoot $service)
    try {
        $mavenArgs = @('-B', '-ntp', 'test')
        if ($SwaggerOnly -and $service -ne 'api-gateway') {
            $mavenArgs += '-Dtest=SwaggerIntegrationTest'
        }
        & .\mvnw.cmd @mavenArgs
        if ($LASTEXITCODE -ne 0) { throw "Tests failed: $service" }
    } finally {
        Pop-Location
    }
}
