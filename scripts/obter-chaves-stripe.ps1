# Script para Obter Chaves do Stripe
# Busca as chaves configuradas no Firebase Secrets

Write-Host "🔍 Buscando chaves do Stripe..." -ForegroundColor Cyan
Write-Host ""

# Verificar se Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version
    Write-Host "✅ Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Firebase CLI não encontrado!" -ForegroundColor Red
    Write-Host "Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "📋 Listando secrets do Firebase..." -ForegroundColor Yellow
Write-Host ""

# Listar todos os secrets
firebase functions:secrets:access STRIPE_SECRET_KEY 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STRIPE_SECRET_KEY encontrado no Firebase" -ForegroundColor Green
    Write-Host "Para ver o valor, execute:" -ForegroundColor Yellow
    Write-Host "   firebase functions:secrets:access STRIPE_SECRET_KEY" -ForegroundColor Gray
} else {
    Write-Host "❌ STRIPE_SECRET_KEY não encontrado" -ForegroundColor Red
}

Write-Host ""

firebase functions:secrets:access STRIPE_WEBHOOK_SECRET 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STRIPE_WEBHOOK_SECRET encontrado no Firebase" -ForegroundColor Green
    Write-Host "Para ver o valor, execute:" -ForegroundColor Yellow
    Write-Host "   firebase functions:secrets:access STRIPE_WEBHOOK_SECRET" -ForegroundColor Gray
} else {
    Write-Host "❌ STRIPE_WEBHOOK_SECRET não encontrado" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Como Obter as Chaves" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Opção 1: Do Firebase Secrets" -ForegroundColor Yellow
Write-Host "   firebase functions:secrets:access STRIPE_SECRET_KEY" -ForegroundColor White
Write-Host "   firebase functions:secrets:access STRIPE_WEBHOOK_SECRET" -ForegroundColor White
Write-Host ""
Write-Host "Opção 2: Do Stripe Dashboard" -ForegroundColor Yellow
Write-Host "   1. Acesse: https://dashboard.stripe.com/apikeys" -ForegroundColor White
Write-Host "   2. Copie a 'Secret key' (sk_live_... ou sk_test_...)" -ForegroundColor White
Write-Host "   3. Para webhook: https://dashboard.stripe.com/webhooks" -ForegroundColor White
Write-Host "   4. Clique no webhook → 'Reveal' no 'Signing secret'" -ForegroundColor White
Write-Host ""
