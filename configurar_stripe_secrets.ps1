# Script Completo para Configurar Todos os Secrets do Stripe no Firebase
# Execute este script na pasta raiz do projeto
#
# Secrets que serão configurados:
# 1. STRIPE_SECRET_KEY - Chave privada do Stripe (obrigatória)
# 2. STRIPE_PUBLISHABLE_KEY - Chave pública do Stripe (obrigatória)
# 3. STRIPE_WEBHOOK_SECRET - Secret do webhook (opcional, pode configurar depois)
# 4. STRIPE_REFRESH_URL - URL de retorno após onboarding (obrigatória)
# 5. STRIPE_RETURN_URL - URL de retorno após onboarding (obrigatória)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Configuração Completa de Secrets" -ForegroundColor Cyan
Write-Host "  do Stripe no Firebase" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
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
Write-Host "Vamos configurar os seguintes secrets:" -ForegroundColor Yellow
Write-Host "1. STRIPE_SECRET_KEY (chave privada) - OBRIGATÓRIA" -ForegroundColor White
Write-Host "2. STRIPE_PUBLISHABLE_KEY (chave pública) - OBRIGATÓRIA" -ForegroundColor White
Write-Host "3. STRIPE_WEBHOOK_SECRET (secret do webhook) - OPCIONAL" -ForegroundColor Gray
Write-Host "4. STRIPE_REFRESH_URL (URL de retorno) - OBRIGATÓRIA" -ForegroundColor White
Write-Host "5. STRIPE_RETURN_URL (URL de retorno) - OBRIGATÓRIA" -ForegroundColor White
Write-Host ""

# Secret 1: STRIPE_SECRET_KEY
Write-Host "----------------------------------------" -ForegroundColor Cyan
Write-Host "1. STRIPE_SECRET_KEY" -ForegroundColor Yellow
Write-Host "Cole sua chave privada do Stripe (sk_live_... ou sk_test_...):" -ForegroundColor White
$stripeSecretKey = Read-Host -AsSecureString
$stripeSecretKeyPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($stripeSecretKey))

if ([string]::IsNullOrWhiteSpace($stripeSecretKeyPlain)) {
    Write-Host "❌ Chave não pode estar vazia!" -ForegroundColor Red
    exit 1
}

Write-Host "Configurando STRIPE_SECRET_KEY..." -ForegroundColor Yellow
echo $stripeSecretKeyPlain | firebase functions:secrets:set STRIPE_SECRET_KEY
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STRIPE_SECRET_KEY configurado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao configurar STRIPE_SECRET_KEY" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Secret 2: STRIPE_PUBLISHABLE_KEY
Write-Host "----------------------------------------" -ForegroundColor Cyan
Write-Host "2. STRIPE_PUBLISHABLE_KEY" -ForegroundColor Yellow
Write-Host "Cole sua chave pública do Stripe (pk_live_... ou pk_test_...):" -ForegroundColor White
$stripePublishableKey = Read-Host -AsSecureString
$stripePublishableKeyPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($stripePublishableKey))

if ([string]::IsNullOrWhiteSpace($stripePublishableKeyPlain)) {
    Write-Host "❌ Chave não pode estar vazia!" -ForegroundColor Red
    exit 1
}

Write-Host "Configurando STRIPE_PUBLISHABLE_KEY..." -ForegroundColor Yellow
echo $stripePublishableKeyPlain | firebase functions:secrets:set STRIPE_PUBLISHABLE_KEY
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STRIPE_PUBLISHABLE_KEY configurado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao configurar STRIPE_PUBLISHABLE_KEY" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Secret 3: STRIPE_WEBHOOK_SECRET (Opcional)
Write-Host "----------------------------------------" -ForegroundColor Cyan
Write-Host "3. STRIPE_WEBHOOK_SECRET (OPCIONAL)" -ForegroundColor Yellow
Write-Host "Cole o secret do webhook do Stripe (whsec_...):" -ForegroundColor White
Write-Host "(Você obterá isso após configurar o webhook no Stripe Dashboard)" -ForegroundColor Gray
Write-Host "Pressione Enter para pular e configurar depois, ou cole o secret:" -ForegroundColor Yellow
$stripeWebhookSecret = Read-Host -AsSecureString
$stripeWebhookSecretPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($stripeWebhookSecret))

if (-not [string]::IsNullOrWhiteSpace($stripeWebhookSecretPlain)) {
    Write-Host "Configurando STRIPE_WEBHOOK_SECRET..." -ForegroundColor Yellow
    echo $stripeWebhookSecretPlain | firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ STRIPE_WEBHOOK_SECRET configurado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Erro ao configurar STRIPE_WEBHOOK_SECRET" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ STRIPE_WEBHOOK_SECRET não configurado. Configure depois no Stripe Dashboard." -ForegroundColor Yellow
}

Write-Host ""

# Secret 4: STRIPE_REFRESH_URL
Write-Host "----------------------------------------" -ForegroundColor Cyan
Write-Host "4. STRIPE_REFRESH_URL" -ForegroundColor Yellow
Write-Host "URL de retorno após onboarding do Stripe Connect:" -ForegroundColor White
Write-Host "Exemplo: https://taskgo.app/settings" -ForegroundColor Gray
$stripeRefreshUrl = Read-Host "Digite a URL (ou pressione Enter para usar o padrão: https://taskgo.app/settings)"

if ([string]::IsNullOrWhiteSpace($stripeRefreshUrl)) {
    $stripeRefreshUrl = "https://taskgo.app/settings"
}

Write-Host "Configurando STRIPE_REFRESH_URL com valor: $stripeRefreshUrl" -ForegroundColor Yellow
echo $stripeRefreshUrl | firebase functions:secrets:set STRIPE_REFRESH_URL
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STRIPE_REFRESH_URL configurado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao configurar STRIPE_REFRESH_URL" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Secret 5: STRIPE_RETURN_URL
Write-Host "----------------------------------------" -ForegroundColor Cyan
Write-Host "5. STRIPE_RETURN_URL" -ForegroundColor Yellow
Write-Host "URL de retorno após onboarding do Stripe Connect:" -ForegroundColor White
Write-Host "Exemplo: https://taskgo.app/settings" -ForegroundColor Gray
$stripeReturnUrl = Read-Host "Digite a URL (ou pressione Enter para usar o padrão: https://taskgo.app/settings)"

if ([string]::IsNullOrWhiteSpace($stripeReturnUrl)) {
    $stripeReturnUrl = "https://taskgo.app/settings"
}

Write-Host "Configurando STRIPE_RETURN_URL com valor: $stripeReturnUrl" -ForegroundColor Yellow
echo $stripeReturnUrl | firebase functions:secrets:set STRIPE_RETURN_URL
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STRIPE_RETURN_URL configurado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao configurar STRIPE_RETURN_URL" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Configuração Concluída!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Secrets configurados:" -ForegroundColor Green
Write-Host "   - STRIPE_SECRET_KEY" -ForegroundColor White
Write-Host "   - STRIPE_PUBLISHABLE_KEY" -ForegroundColor White
if (-not [string]::IsNullOrWhiteSpace($stripeWebhookSecretPlain)) {
    Write-Host "   - STRIPE_WEBHOOK_SECRET" -ForegroundColor White
} else {
    Write-Host "   - STRIPE_WEBHOOK_SECRET (não configurado)" -ForegroundColor Gray
}
Write-Host "   - STRIPE_REFRESH_URL" -ForegroundColor White
Write-Host "   - STRIPE_RETURN_URL" -ForegroundColor White
Write-Host ""
Write-Host "⚠️ IMPORTANTE: Faça o deploy das functions agora:" -ForegroundColor Yellow
Write-Host "   firebase deploy --only functions" -ForegroundColor White
Write-Host ""
Write-Host "📖 Próximos passos:" -ForegroundColor Yellow
Write-Host "   1. Configure o webhook no Stripe Dashboard (se ainda não fez)" -ForegroundColor White
Write-Host "   2. Faça o deploy das functions" -ForegroundColor White
Write-Host "   3. Teste um pagamento no app" -ForegroundColor White
Write-Host ""
