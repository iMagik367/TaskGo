# Script para habilitar Cloud Firestore API automaticamente
# Usa Service Management API do Google Cloud

$projectId = "task-go-ee85f"
$apiName = "firestore.googleapis.com"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HABILITANDO CLOUD FIRESTORE API" -ForegroundColor Cyan
Write-Host "Projeto: $projectId" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Metodo 1: Tentar via gcloud
Write-Host "[1/3] Tentando via gcloud CLI..." -ForegroundColor Yellow
try {
    $gcloudCheck = gcloud --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "gcloud encontrado, habilitando API..." -ForegroundColor Yellow
        gcloud config set project $projectId 2>&1 | Out-Null
        gcloud services enable $apiName --project=$projectId 2>&1 | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "OK API habilitada via gcloud!" -ForegroundColor Green
            Write-Host "Aguardando 30 segundos..." -ForegroundColor Yellow
            Start-Sleep -Seconds 30
            
            # Verificar
            $enabled = gcloud services list --enabled --filter="name:$apiName" --format="value(name)" 2>&1
            if ($enabled -match $apiName) {
                Write-Host "OK API confirmada como habilitada!" -ForegroundColor Green
                exit 0
            }
        }
    }
} catch {
    Write-Host "gcloud nao disponivel" -ForegroundColor Yellow
}

# Metodo 2: Abrir link direto
Write-Host ""
Write-Host "[2/3] Abrindo Google Cloud Console..." -ForegroundColor Yellow
$enableUrl = "https://console.cloud.google.com/apis/library/$apiName`?project=$projectId"
Start-Process $enableUrl
Write-Host "OK Link aberto no navegador" -ForegroundColor Green
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Cyan
Write-Host "1. Clique no botao ENABLE (Habilitar)" -ForegroundColor White
Write-Host "2. Aguarde 2-5 minutos" -ForegroundColor White
Write-Host "3. Execute: .\verificar-firestore-config.ps1" -ForegroundColor White
Write-Host ""

# Metodo 3: Tentar via Firebase CLI (pode habilitar automaticamente)
Write-Host "[3/3] Tentando via Firebase CLI..." -ForegroundColor Yellow
try {
    Write-Host "Fazendo deploy das regras (isso pode habilitar a API automaticamente)..." -ForegroundColor Yellow
    firebase deploy --only firestore:rules --project=$projectId 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK Deploy realizado. A API pode ter sido habilitada automaticamente." -ForegroundColor Green
        Write-Host "Aguarde 2-5 minutos e teste o app." -ForegroundColor Yellow
    }
} catch {
    Write-Host "Erro ao fazer deploy via Firebase CLI" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CONCLUIDO" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
