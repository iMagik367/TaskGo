# Script para corrigir a extensão Trigger Email from Firestore
# Este script automatiza o processo de desinstalação e reinstalação

$PROJECT_ID = "task-go-ee85f"
$EXTENSION_ID = "firebase/firestore-send-email"
$INSTANCE_ID = "firestore-send-email"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Correção da Extensão de Email" -ForegroundColor Cyan
Write-Host "  Trigger Email from Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar Firebase CLI
if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Firebase CLI não encontrado!" -ForegroundColor Red
    Write-Host "   Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Firebase CLI encontrado" -ForegroundColor Green
Write-Host ""

# Verificar status atual
Write-Host "🔍 Verificando status atual da extensão..." -ForegroundColor Cyan
$extensions = firebase ext:list --project=$PROJECT_ID --json 2>&1 | ConvertFrom-Json

$currentExtension = $extensions.result | Where-Object { $_.ref -like "*firestore-send-email*" }

if ($currentExtension) {
    Write-Host "📋 Extensão encontrada:" -ForegroundColor Yellow
    Write-Host "   - Ref: $($currentExtension.ref)" -ForegroundColor Yellow
    Write-Host "   - Instance ID: $($currentExtension.instanceId)" -ForegroundColor Yellow
    Write-Host "   - Estado: $($currentExtension.state)" -ForegroundColor $(if ($currentExtension.state -eq "ERRORED") { "Red" } else { "Yellow" })
    Write-Host ""
    
    if ($currentExtension.state -eq "ERRORED") {
        Write-Host "⚠️ A extensão está em estado ERRORED devido a problema de região." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "📝 SOLUÇÃO:" -ForegroundColor Cyan
        Write-Host "   Como o Firebase CLI requer modo interativo, você precisa:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   1. Desinstalar manualmente via Console:" -ForegroundColor Cyan
        Write-Host "      a) Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor White
        Write-Host "      b) Encontre Trigger Email from Firestore" -ForegroundColor White
        Write-Host "      c) Clique em Desinstalar" -ForegroundColor White
        Write-Host ""
        Write-Host "   2. OU usar o comando abaixo (pode não funcionar se não estiver no firebase.json):" -ForegroundColor Cyan
        Write-Host "      firebase ext:uninstall $($currentExtension.instanceId) --project=$PROJECT_ID" -ForegroundColor White
        Write-Host ""
        
        $choice = Read-Host "Deseja tentar desinstalar via CLI agora? (s/N)"
        if ($choice -eq "s" -or $choice -eq "S") {
            Write-Host ""
            Write-Host "🗑️ Tentando desinstalar..." -ForegroundColor Yellow
            firebase ext:uninstall $($currentExtension.instanceId) --project=$PROJECT_ID --force 2>&1
            Write-Host ""
            Start-Sleep -Seconds 3
            
            # Verificar se foi desinstalada
            $extensionsAfter = firebase ext:list --project=$PROJECT_ID --json 2>&1 | ConvertFrom-Json
            $stillExists = $extensionsAfter.result | Where-Object { $_.ref -like "*firestore-send-email*" }
            
            if ($stillExists) {
                Write-Host "⚠️ A extensão ainda existe. Você precisará desinstalar manualmente via Console." -ForegroundColor Yellow
                Write-Host ""
                Write-Host "   Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor Cyan
                Write-Host ""
                $wait = Read-Host "Pressione Enter após desinstalar no console para continuar..."
            }
        } else {
            Write-Host ""
            Write-Host "⚠️ Você precisará desinstalar manualmente primeiro." -ForegroundColor Yellow
            Write-Host "   Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor Cyan
            Write-Host ""
            $wait = Read-Host "Pressione Enter após desinstalar no console para continuar..."
        }
    }
} else {
    Write-Host "✅ Nenhuma extensão de email encontrada. Pronto para instalar." -ForegroundColor Green
    Write-Host ""
}

# Verificar se ainda existe
Write-Host "🔍 Verificando novamente..." -ForegroundColor Cyan
$extensionsCheck = firebase ext:list --project=$PROJECT_ID --json 2>&1 | ConvertFrom-Json
$stillExists = $extensionsCheck.result | Where-Object { $_.ref -like "*firestore-send-email*" }

if ($stillExists) {
    Write-Host "❌ A extensão ainda está instalada. Por favor, desinstale manualmente primeiro." -ForegroundColor Red
    Write-Host "   Console: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor Cyan
    exit 1
}

Write-Host "✅ Extensão não encontrada. Pronto para instalação." -ForegroundColor Green
Write-Host ""

# Instruções para instalação
Write-Host "📦 PRÓXIMOS PASSOS PARA INSTALAÇÃO:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Como o Firebase CLI requer modo interativo, você tem duas opções:" -ForegroundColor Yellow
Write-Host ""
Write-Host "OPÇÃO 1: Via Console (Recomendado)" -ForegroundColor Green
Write-Host "   1. Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor White
Write-Host "   2. Clique em Browse Extensions ou Navegar por extensoes" -ForegroundColor White
Write-Host "   3. Procure por Trigger Email from Firestore" -ForegroundColor White
Write-Host "   4. Clique em Install ou Instalar" -ForegroundColor White
Write-Host "   5. IMPORTANTE: Quando perguntado sobre Location, use: us-central1" -ForegroundColor Yellow
Write-Host "      (ou a região onde seu Firestore está configurado)" -ForegroundColor Yellow
Write-Host "   6. Configure os parâmetros SMTP" -ForegroundColor White
Write-Host ""
Write-Host "OPÇÃO 2: Via CLI Interativo" -ForegroundColor Green
Write-Host "   Execute o comando abaixo e siga as instruções:" -ForegroundColor White
Write-Host "   firebase ext:install firebase/firestore-send-email --project=$PROJECT_ID" -ForegroundColor Cyan
Write-Host ""
Write-Host "   IMPORTANTE: Quando perguntado sobre Location, digite: us-central1" -ForegroundColor Yellow
Write-Host ""

$installChoice = Read-Host "Deseja tentar instalar via CLI agora? (s/N)"
if ($installChoice -eq "s" -or $installChoice -eq "S") {
    Write-Host ""
    Write-Host "📦 Iniciando instalação interativa..." -ForegroundColor Green
    Write-Host "Lembre-se: Use us-central1 como Location quando perguntado!" -ForegroundColor Yellow
    Write-Host ""
    firebase ext:install $EXTENSION_ID --project=$PROJECT_ID
} else {
    Write-Host ""
    Write-Host "✅ Use uma das opções acima para instalar a extensão." -ForegroundColor Green
    Write-Host ""
    Write-Host "📝 Resumo:" -ForegroundColor Cyan
    Write-Host "   - Desinstale a extensão antiga (se ainda existir)" -ForegroundColor White
    Write-Host "   - Instale novamente especificando Location: us-central1" -ForegroundColor White
    Write-Host "   - Configure as credenciais SMTP" -ForegroundColor White
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Processo Concluído!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

















