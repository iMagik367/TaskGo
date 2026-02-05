# Script para Testar Backend no Railway
# Execute após configurar todas as variáveis

param(
    [Parameter(Mandatory=$true)]
    [string]$BackendUrl
)

Write-Host "🧪 Testando Backend Railway" -ForegroundColor Cyan
Write-Host "URL: $BackendUrl" -ForegroundColor Yellow
Write-Host ""

# Test 1: Health Check
Write-Host "1. Testando Health Check..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BackendUrl/health" -Method Get
    Write-Host "   ✅ Health Check OK: $($response.status)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Health Check FALHOU: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 2: Location Update
Write-Host "2. Testando Atualização de Localização..." -ForegroundColor Yellow
try {
    $body = @{
        userId = "test-user-$(Get-Random)"
        latitude = -23.5505
        longitude = -46.6333
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$BackendUrl/api/location/update" -Method Post -Body $body -ContentType "application/json"
    Write-Host "   ✅ Localização atualizada: City ID $($response.currentCityId)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Atualização de localização FALHOU: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: Create Service Order
Write-Host "3. Testando Criação de Ordem de Serviço..." -ForegroundColor Yellow
try {
    $body = @{
        client_id = "test-client-$(Get-Random)"
        created_in_city_id = 1
        category = "Pintura"
        details = "Teste de ordem de serviço"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$BackendUrl/api/orders/service" -Method Post -Body $body -ContentType "application/json"
    Write-Host "   ✅ Ordem criada: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Criação de ordem FALHOU: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Testes concluídos!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Próximos passos:" -ForegroundColor Cyan
Write-Host "   1. Executar migrations do banco" -ForegroundColor White
Write-Host "   2. Configurar webhook do Stripe" -ForegroundColor White
Write-Host "   3. Atualizar app mobile com nova URL" -ForegroundColor White
