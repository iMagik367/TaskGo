# Script simplificado para configurar GEMINI_API_KEY
# Método direto via firebase functions:config:set

Write-Host "=== Configurar GEMINI_API_KEY ===" -ForegroundColor Cyan
Write-Host ""

# Solicitar a chave
$apiKey = Read-Host "Digite sua GEMINI_API_KEY (ou pressione Enter para cancelar)"

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Host "Operação cancelada." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Configurando..." -ForegroundColor Yellow

# Configurar via functions:config:set
# Formato: gemini.api_key (o código lê como process.env.GEMINI_API_KEY)
firebase functions:config:set gemini.api_key="$apiKey"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Configurado com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE: Faça redeploy das functions:" -ForegroundColor Yellow
    Write-Host "   firebase deploy --only functions" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "❌ Erro ao configurar. Verifique se está autenticado no Firebase." -ForegroundColor Red
    Write-Host "   Execute: firebase login" -ForegroundColor Yellow
}


