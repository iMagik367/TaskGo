# Script para habilitar Cloud Firestore API e verificar configuração
# Projeto: task-go-ee85f

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HABILITANDO FIRESTORE API" -ForegroundColor Cyan
Write-Host "Projeto: task-go-ee85f" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se gcloud está instalado
Write-Host "[1/5] Verificando se gcloud CLI está instalado..." -ForegroundColor Yellow
try {
    $gcloudVersion = gcloud --version 2>&1 | Select-Object -First 1
    Write-Host "✅ gcloud encontrado: $gcloudVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ gcloud CLI não encontrado!" -ForegroundColor Red
    Write-Host "Por favor, instale o Google Cloud SDK:" -ForegroundColor Yellow
    Write-Host "https://cloud.google.com/sdk/docs/install" -ForegroundColor Yellow
    exit 1
}

# Configurar projeto
Write-Host ""
Write-Host "[2/5] Configurando projeto..." -ForegroundColor Yellow
gcloud config set project task-go-ee85f
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao configurar projeto" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Projeto configurado: task-go-ee85f" -ForegroundColor Green

# Verificar APIs habilitadas
Write-Host ""
Write-Host "[3/5] Verificando APIs habilitadas..." -ForegroundColor Yellow
$firestoreApi = gcloud services list --enabled --filter="name:firestore.googleapis.com" --format="value(name)" 2>&1
if ($firestoreApi -match "firestore.googleapis.com") {
    Write-Host "✅ Cloud Firestore API já está habilitada" -ForegroundColor Green
} else {
    Write-Host "⚠️  Cloud Firestore API não está habilitada. Habilitando..." -ForegroundColor Yellow
    
    # Habilitar Cloud Firestore API
    Write-Host "Habilitando Cloud Firestore API..." -ForegroundColor Yellow
    gcloud services enable firestore.googleapis.com --project=task-go-ee85f
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Erro ao habilitar Cloud Firestore API" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Cloud Firestore API habilitada com sucesso!" -ForegroundColor Green
}

# Verificar outras APIs necessárias
Write-Host ""
Write-Host "[4/5] Verificando outras APIs necessárias..." -ForegroundColor Yellow
$requiredApis = @(
    "firebase.googleapis.com",
    "identitytoolkit.googleapis.com",
    "securetoken.googleapis.com"
)

foreach ($api in $requiredApis) {
    $enabled = gcloud services list --enabled --filter="name:$api" --format="value(name)" 2>&1
    if ($enabled -match $api) {
        Write-Host "✅ $api está habilitada" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Habilitando $api..." -ForegroundColor Yellow
        gcloud services enable $api --project=task-go-ee85f
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ $api habilitada" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Aviso ao habilitar $api (pode já estar habilitada)" -ForegroundColor Yellow
        }
    }
}

# Verificar databases do Firestore
Write-Host ""
Write-Host "[5/5] Verificando databases do Firestore..." -ForegroundColor Yellow
try {
    $databases = firebase firestore:databases:list --project=task-go-ee85f 2>&1
    if ($databases -match "taskgo") {
        Write-Host "✅ Database 'taskgo' encontrado" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Database 'taskgo' não encontrado na lista" -ForegroundColor Yellow
        Write-Host "Verifique no Firebase Console se o database está criado" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Não foi possível listar databases (pode ser normal)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ CONCLUÍDO!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Yellow
Write-Host "1. Aguarde alguns minutos para a API ser totalmente ativada" -ForegroundColor White
Write-Host "2. Teste o cadastro/login no app" -ForegroundColor White
Write-Host "3. Verifique os logs se ainda houver erros" -ForegroundColor White
Write-Host ""
