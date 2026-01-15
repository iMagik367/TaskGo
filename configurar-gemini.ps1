# Script para configurar GEMINI_API_KEY no Firebase Functions
# Este script configura a variável usando firebase functions:config:set

param(
    [string]$ApiKey = ""
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Configurar GEMINI_API_KEY no Firebase" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Se não foi passada como parâmetro, solicitar
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    Write-Host "Por favor, forneça sua chave da API Gemini." -ForegroundColor Yellow
    Write-Host "Obtenha uma em: https://aistudio.google.com/app/apikey" -ForegroundColor Yellow
    Write-Host ""
    $ApiKey = Read-Host "Digite sua GEMINI_API_KEY"
    
    if ([string]::IsNullOrWhiteSpace($ApiKey)) {
        Write-Host ""
        Write-Host "❌ Operação cancelada. Chave não fornecida." -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Configurando variável de ambiente..." -ForegroundColor Yellow

# Para Firebase Functions, usar functions:config:set
# O código lê process.env.GEMINI_API_KEY, então precisamos configurar como gemini.api_key
# Firebase converte gemini.api_key para GEMINI_API_KEY automaticamente

$command = 'firebase functions:config:set gemini.api_key="' + $ApiKey + '"'
Write-Host "Executando: firebase functions:config:set gemini.api_key=***" -ForegroundColor Gray

Invoke-Expression $command

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Variável configurada com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE: Você precisa fazer redeploy das functions:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   firebase deploy --only functions" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Ou apenas das functions de AI Chat:" -ForegroundColor Yellow
    Write-Host "   firebase deploy --only functions:aiChatProxy" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Erro ao configurar variável." -ForegroundColor Red
    Write-Host ""
    Write-Host "Verifique:" -ForegroundColor Yellow
    Write-Host "  1. Se está autenticado: firebase login" -ForegroundColor White
    Write-Host "  2. Se está no projeto correto: firebase projects:list" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "Configuração concluída!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green


