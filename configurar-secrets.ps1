# 🔐 Script PowerShell para Configurar Secrets no Firebase
# Execute este script para configurar todos os secrets necessários

Write-Host "🔐 Configurando Secrets no Firebase" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Firebase CLI está instalado
$firebaseInstalled = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebaseInstalled) {
    Write-Host "❌ Firebase CLI não encontrado!" -ForegroundColor Red
    Write-Host "Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Firebase CLI encontrado" -ForegroundColor Green
Write-Host ""

# Lista de secrets a configurar
$secrets = @(
    @{Name="SMTP_HOST"; Description="SMTP Host (ex: smtp.gmail.com)"; Required=$true},
    @{Name="SMTP_PORT"; Description="SMTP Port (ex: 465 ou 587)"; Required=$true},
    @{Name="SMTP_USER"; Description="SMTP User (seu email)"; Required=$true},
    @{Name="SMTP_PASSWORD"; Description="SMTP Password (senha de app para Gmail)"; Required=$true},
    @{Name="EMAIL_DEFAULT_FROM"; Description="Email padrão 'From' (ex: noreply@taskgo.app)"; Required=$true},
    @{Name="EMAIL_DEFAULT_REPLY_TO"; Description="Email padrão 'Reply-To' (ex: suporte@taskgo.app)"; Required=$true},
    @{Name="STRIPE_SECRET_KEY"; Description="Stripe Secret Key (sk_live_... ou sk_test_...)"; Required=$false},
    @{Name="STRIPE_PUBLISHABLE_KEY"; Description="Stripe Publishable Key (pk_live_... ou pk_test_...)"; Required=$false},
    @{Name="STRIPE_WEBHOOK_SECRET"; Description="Stripe Webhook Secret (whsec_...)"; Required=$false},
    @{Name="GEMINI_API_KEY"; Description="Gemini API Key (para chat IA)"; Required=$false},
    @{Name="OPENAI_API_KEY"; Description="OpenAI API Key (opcional - para fallback)"; Required=$false}
)

Write-Host "📋 Secrets que serão configurados:" -ForegroundColor Yellow
foreach ($secret in $secrets) {
    $required = if ($secret.Required) { "OBRIGATÓRIO" } else { "Opcional" }
    Write-Host "  - $($secret.Name) ($required)" -ForegroundColor Gray
}
Write-Host ""

# Perguntar se deseja continuar
$continue = Read-Host "Deseja continuar? (S/N)"
if ($continue -ne "S" -and $continue -ne "s") {
    Write-Host "Operação cancelada." -ForegroundColor Yellow
    exit 0
}

Write-Host ""

# Configurar cada secret
$configuredSecrets = @()
$skippedSecrets = @()

foreach ($secret in $secrets) {
    Write-Host "─────────────────────────────────────────" -ForegroundColor Gray
    Write-Host "🔐 Configurando: $($secret.Name)" -ForegroundColor Cyan
    Write-Host "   $($secret.Description)" -ForegroundColor Gray
    
    $required = if ($secret.Required) { "OBRIGATÓRIO" } else { "Opcional" }
    Write-Host "   Status: $required" -ForegroundColor $(if ($secret.Required) { "Yellow" } else { "Gray" })
    Write-Host ""
    
    # Perguntar se deseja configurar (se opcional)
    if (-not $secret.Required) {
        $configure = Read-Host "Deseja configurar este secret? (S/N)"
        if ($configure -ne "S" -and $configure -ne "s") {
            Write-Host "⏭️  Pulando $($secret.Name)" -ForegroundColor Yellow
            $skippedSecrets += $secret.Name
            Write-Host ""
            continue
        }
    }
    
    # Executar comando Firebase para configurar secret
    try {
        Write-Host "💡 Dica: O valor será solicitado interativamente para segurança" -ForegroundColor Green
        Write-Host ""
        
        # Executar comando Firebase
        firebase functions:secrets:set $secret.Name
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ $($secret.Name) configurado com sucesso!" -ForegroundColor Green
            $configuredSecrets += $secret.Name
        } else {
            Write-Host "❌ Erro ao configurar $($secret.Name)" -ForegroundColor Red
            Write-Host "   Verifique os logs acima para mais detalhes" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ Erro ao configurar $($secret.Name): $_" -ForegroundColor Red
    }
    
    Write-Host ""
}

# Resumo final
Write-Host "═════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "📊 RESUMO DA CONFIGURAÇÃO" -ForegroundColor Cyan
Write-Host "═════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "✅ Secrets configurados: $($configuredSecrets.Count)" -ForegroundColor Green
foreach ($name in $configuredSecrets) {
    Write-Host "   - $name" -ForegroundColor Green
}

if ($skippedSecrets.Count -gt 0) {
    Write-Host ""
    Write-Host "⏭️  Secrets pulados: $($skippedSecrets.Count)" -ForegroundColor Yellow
    foreach ($name in $skippedSecrets) {
        Write-Host "   - $name" -ForegroundColor Yellow
    }
}

Write-Host ""

# Verificar se há secrets obrigatórios não configurados
$requiredSecrets = $secrets | Where-Object { $_.Required }
$missingRequired = $requiredSecrets | Where-Object { $configuredSecrets -notcontains $_.Name }

if ($missingRequired.Count -gt 0) {
    Write-Host "⚠️  ATENÇÃO: Secrets obrigatórios não configurados:" -ForegroundColor Red
    foreach ($secret in $missingRequired) {
        Write-Host "   - $($secret.Name)" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "⚠️  Você DEVE configurar todos os secrets obrigatórios antes do deploy!" -ForegroundColor Red
} else {
    Write-Host "✅ Todos os secrets obrigatórios foram configurados!" -ForegroundColor Green
}

Write-Host ""
Write-Host "═════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🚀 PRÓXIMOS PASSOS" -ForegroundColor Cyan
Write-Host "═════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Validar que todos os secrets estão configurados:" -ForegroundColor Yellow
Write-Host "   firebase functions:secrets:access --list" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Fazer redeploy das functions:" -ForegroundColor Yellow
Write-Host "   firebase deploy --only functions" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Testar em produção" -ForegroundColor Yellow
Write-Host ""

Write-Host "✅ Script concluído!" -ForegroundColor Green
