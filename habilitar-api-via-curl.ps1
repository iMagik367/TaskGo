# Script para habilitar Cloud Firestore API via REST API
# Requer autenticação OAuth2 do Google Cloud

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HABILITANDO FIRESTORE API VIA REST" -ForegroundColor Cyan
Write-Host "Projeto: task-go-ee85f" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "⚠️  Este script requer autenticação OAuth2" -ForegroundColor Yellow
Write-Host ""
Write-Host "Para habilitar a API, você precisa:" -ForegroundColor Yellow
Write-Host "1. Obter um access token do Google Cloud" -ForegroundColor White
Write-Host "2. Fazer uma requisição POST para a API de Service Management" -ForegroundColor White
Write-Host ""
Write-Host "MÉTODO RECOMENDADO - Via Web:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Acesse diretamente este link para habilitar:" -ForegroundColor Yellow
Write-Host "https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f" -ForegroundColor Green
Write-Host ""
Write-Host "Ou use o link direto de habilitação:" -ForegroundColor Yellow
Write-Host "https://console.cloud.google.com/marketplace/product/google/firestore.googleapis.com?project=task-go-ee85f" -ForegroundColor Green
Write-Host ""

# Tentar abrir o link no navegador
Write-Host "Deseja abrir o link no navegador? (S/N)" -ForegroundColor Yellow
$response = Read-Host
if ($response -eq "S" -or $response -eq "s") {
    Start-Process "https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f"
    Write-Host "✅ Link aberto no navegador" -ForegroundColor Green
    Write-Host ""
    Write-Host "Após habilitar a API:" -ForegroundColor Yellow
    Write-Host "1. Aguarde 2-5 minutos" -ForegroundColor White
    Write-Host "2. Execute: .\verificar-firestore-config.ps1" -ForegroundColor White
    Write-Host "3. Teste o app novamente" -ForegroundColor White
}

Write-Host ""
