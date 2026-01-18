# Script completo para habilitar Cloud Firestore API
# Projeto: task-go-ee85f

param(
    [switch]$AbrirNavegador = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HABILITANDO CLOUD FIRESTORE API" -ForegroundColor Cyan
Write-Host "Projeto: task-go-ee85f" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se gcloud esta disponivel
$gcloudAvailable = $false
try {
    $null = gcloud --version 2>&1
    $gcloudAvailable = $true
} catch {
    $gcloudAvailable = $false
}

if ($gcloudAvailable) {
    Write-Host "[METODO 1] Tentando habilitar via gcloud CLI..." -ForegroundColor Yellow
    Write-Host ""
    
    # Configurar projeto
    gcloud config set project task-go-ee85f 2>&1 | Out-Null
    
    # Habilitar Cloud Firestore API
    Write-Host "Habilitando Cloud Firestore API..." -ForegroundColor Yellow
    gcloud services enable firestore.googleapis.com --project=task-go-ee85f 2>&1 | Tee-Object -Variable output
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK Cloud Firestore API habilitada com sucesso via gcloud!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Aguardando 30 segundos para a API ser ativada..." -ForegroundColor Yellow
        Start-Sleep -Seconds 30
        
        # Verificar se foi habilitada
        $enabled = gcloud services list --enabled --filter="name:firestore.googleapis.com" --format="value(name)" 2>&1
        if ($enabled -match "firestore.googleapis.com") {
            Write-Host "OK API confirmada como habilitada!" -ForegroundColor Green
        } else {
            Write-Host "AVISO API pode ainda estar sendo ativada. Aguarde alguns minutos." -ForegroundColor Yellow
        }
    } else {
        Write-Host "AVISO Nao foi possivel habilitar via gcloud. Tentando metodo alternativo..." -ForegroundColor Yellow
        Write-Host ""
        $gcloudAvailable = $false
    }
}

if (-not $gcloudAvailable) {
    Write-Host "[METODO 2] Habilitando via Google Cloud Console (Web)" -ForegroundColor Yellow
    Write-Host ""
    
    $enableUrl = "https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f"
    
    Write-Host "Para habilitar a Cloud Firestore API:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Acesse este link:" -ForegroundColor White
    Write-Host "   $enableUrl" -ForegroundColor Green
    Write-Host ""
    Write-Host "2. Clique no botao ENABLE (Habilitar)" -ForegroundColor White
    Write-Host ""
    Write-Host "3. Aguarde 2-5 minutos para a API ser totalmente ativada" -ForegroundColor White
    Write-Host ""
    
    if ($AbrirNavegador) {
        Start-Process $enableUrl
        Write-Host "OK Link aberto no navegador" -ForegroundColor Green
    } else {
        $response = Read-Host "Deseja abrir o link no navegador agora? (S/N)"
        if ($response -eq "S" -or $response -eq "s") {
            Start-Process $enableUrl
            Write-Host "OK Link aberto no navegador" -ForegroundColor Green
        }
    }
    
    if ($AbrirNavegador -or $response -eq "S" -or $response -eq "s") {
        Write-Host ""
        Write-Host "Apos clicar em ENABLE, aguarde alguns minutos e execute:" -ForegroundColor Yellow
        Write-Host "   .\verificar-firestore-config.ps1" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PROXIMOS PASSOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Aguarde 2-5 minutos apos habilitar a API" -ForegroundColor White
Write-Host "2. Execute: .\verificar-firestore-config.ps1" -ForegroundColor White
Write-Host "3. Teste o cadastro/login no app" -ForegroundColor White
Write-Host ""
Write-Host "Se ainda houver erros, verifique:" -ForegroundColor Yellow
Write-Host "- Database taskgo esta criado e ativo" -ForegroundColor White
Write-Host "- Firestore Rules estao deployadas" -ForegroundColor White
Write-Host "- API Key esta correta no google-services.json" -ForegroundColor White
Write-Host ""
