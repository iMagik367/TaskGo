# Script para corrigir a extensao Trigger Email from Firestore

$PROJECT_ID = "task-go-ee85f"
$EXTENSION_ID = "firebase/firestore-send-email"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Correcao da Extensao de Email" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar Firebase CLI
if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    Write-Host "ERRO: Firebase CLI nao encontrado!" -ForegroundColor Red
    Write-Host "   Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "OK: Firebase CLI encontrado" -ForegroundColor Green
Write-Host ""

# Verificar status atual
Write-Host "Verificando status atual da extensao..." -ForegroundColor Cyan
$extensions = firebase ext:list --project=$PROJECT_ID --json 2>&1 | ConvertFrom-Json

$currentExtension = $extensions.result | Where-Object { $_.ref -like "*firestore-send-email*" }

if ($currentExtension) {
    Write-Host "Extensao encontrada:" -ForegroundColor Yellow
    Write-Host "   - Ref: $($currentExtension.ref)" -ForegroundColor Yellow
    Write-Host "   - Instance ID: $($currentExtension.instanceId)" -ForegroundColor Yellow
    Write-Host "   - Estado: $($currentExtension.state)" -ForegroundColor $(if ($currentExtension.state -eq "ERRORED") { "Red" } else { "Yellow" })
    Write-Host ""
    
    if ($currentExtension.state -eq "ERRORED") {
        Write-Host "AVISO: A extensao esta em estado ERRORED devido a problema de regiao." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "SOLUCAO:" -ForegroundColor Cyan
        Write-Host "   Como o Firebase CLI requer modo interativo, voce precisa:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   1. Desinstalar manualmente via Console:" -ForegroundColor Cyan
        Write-Host "      a) Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor White
        Write-Host "      b) Encontre Trigger Email from Firestore" -ForegroundColor White
        Write-Host "      c) Clique em Desinstalar" -ForegroundColor White
        Write-Host ""
        Write-Host "   2. OU usar o comando abaixo:" -ForegroundColor Cyan
        Write-Host "      firebase ext:uninstall $($currentExtension.instanceId) --project=$PROJECT_ID" -ForegroundColor White
        Write-Host ""
        
        $choice = Read-Host "Deseja tentar desinstalar via CLI agora? (s/N)"
        if ($choice -eq "s" -or $choice -eq "S") {
            Write-Host ""
            Write-Host "Tentando desinstalar..." -ForegroundColor Yellow
            firebase ext:uninstall $($currentExtension.instanceId) --project=$PROJECT_ID --force 2>&1
            Write-Host ""
            Start-Sleep -Seconds 3
            
            # Verificar se foi desinstalada
            $extensionsAfter = firebase ext:list --project=$PROJECT_ID --json 2>&1 | ConvertFrom-Json
            $stillExists = $extensionsAfter.result | Where-Object { $_.ref -like "*firestore-send-email*" }
            
            if ($stillExists) {
                Write-Host "AVISO: A extensao ainda existe. Voce precisara desinstalar manualmente via Console." -ForegroundColor Yellow
                Write-Host ""
                Write-Host "   Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor Cyan
                Write-Host ""
                $wait = Read-Host "Pressione Enter apos desinstalar no console para continuar..."
            }
        } else {
            Write-Host ""
            Write-Host "AVISO: Voce precisara desinstalar manualmente primeiro." -ForegroundColor Yellow
            Write-Host "   Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor Cyan
            Write-Host ""
            $wait = Read-Host "Pressione Enter apos desinstalar no console para continuar..."
        }
    }
} else {
    Write-Host "OK: Nenhuma extensao de email encontrada. Pronto para instalar." -ForegroundColor Green
    Write-Host ""
}

# Verificar se ainda existe
Write-Host "Verificando novamente..." -ForegroundColor Cyan
$extensionsCheck = firebase ext:list --project=$PROJECT_ID --json 2>&1 | ConvertFrom-Json
$stillExists = $extensionsCheck.result | Where-Object { $_.ref -like "*firestore-send-email*" }

if ($stillExists) {
    Write-Host "ERRO: A extensao ainda esta instalada. Por favor, desinstale manualmente primeiro." -ForegroundColor Red
    Write-Host "   Console: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor Cyan
    exit 1
}

Write-Host "OK: Extensao nao encontrada. Pronto para instalacao." -ForegroundColor Green
Write-Host ""

# Instrucoes para instalacao
Write-Host "PROXIMOS PASSOS PARA INSTALACAO:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Como o Firebase CLI requer modo interativo, voce tem duas opcoes:" -ForegroundColor Yellow
Write-Host ""
Write-Host "OPCAO 1: Via Console (Recomendado)" -ForegroundColor Green
Write-Host "   1. Acesse: https://console.firebase.google.com/project/$PROJECT_ID/extensions" -ForegroundColor White
Write-Host "   2. Clique em Browse Extensions" -ForegroundColor White
Write-Host "   3. Procure por Trigger Email from Firestore" -ForegroundColor White
Write-Host "   4. Clique em Install" -ForegroundColor White
Write-Host "   5. IMPORTANTE: Quando perguntado sobre Location, use: nam5" -ForegroundColor Yellow
Write-Host "      (nam5 e a regiao multi-regiao do seu Firestore)" -ForegroundColor Yellow
Write-Host "   6. Configure os parametros SMTP" -ForegroundColor White
Write-Host ""
Write-Host "OPCAO 2: Via CLI Interativo" -ForegroundColor Green
Write-Host "   Execute o comando abaixo e siga as instrucoes:" -ForegroundColor White
Write-Host "   firebase ext:install firebase/firestore-send-email --project=$PROJECT_ID" -ForegroundColor Cyan
Write-Host ""
Write-Host "   IMPORTANTE: Quando perguntado sobre Location, digite: nam5" -ForegroundColor Yellow
Write-Host ""

$installChoice = Read-Host "Deseja tentar instalar via CLI agora? (s/N)"
if ($installChoice -eq "s" -or $installChoice -eq "S") {
    Write-Host ""
    Write-Host "Iniciando instalacao interativa..." -ForegroundColor Green
    Write-Host "Lembre-se: Selecione Iowa (us-central1) do dropdown quando perguntado!" -ForegroundColor Yellow
    Write-Host ""
    firebase ext:install $EXTENSION_ID --project=$PROJECT_ID
} else {
    Write-Host ""
    Write-Host "OK: Use uma das opcoes acima para instalar a extensao." -ForegroundColor Green
    Write-Host ""
    Write-Host "Resumo:" -ForegroundColor Cyan
    Write-Host "   - Desinstale a extensao antiga (se ainda existir)" -ForegroundColor White
    Write-Host "   - Instale novamente selecionando Location: Iowa (us-central1) do dropdown" -ForegroundColor White
    Write-Host "   - Configure as credenciais SMTP" -ForegroundColor White
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Processo Concluido!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

















