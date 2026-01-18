# Script para habilitar Firestore API via Firebase CLI e verificar configuração
# Projeto: task-go-ee85f

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HABILITANDO FIRESTORE API" -ForegroundColor Cyan
Write-Host "Projeto: task-go-ee85f" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Firebase CLI está instalado
Write-Host "[1/6] Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version 2>&1 | Select-Object -First 1
    Write-Host "✅ Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Firebase CLI não encontrado!" -ForegroundColor Red
    Write-Host "Instalando Firebase CLI..." -ForegroundColor Yellow
    npm install -g firebase-tools
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Erro ao instalar Firebase CLI" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Firebase CLI instalado" -ForegroundColor Green
}

# Verificar autenticação
Write-Host ""
Write-Host "[2/6] Verificando autenticação Firebase..." -ForegroundColor Yellow
try {
    $firebaseUser = firebase login:list --json 2>&1 | ConvertFrom-Json
    if ($firebaseUser) {
        Write-Host "✅ Autenticado no Firebase" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Não autenticado. Fazendo login..." -ForegroundColor Yellow
        firebase login --no-localhost
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Erro ao fazer login no Firebase" -ForegroundColor Red
            exit 1
        }
        Write-Host "✅ Login realizado com sucesso" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Fazendo login no Firebase..." -ForegroundColor Yellow
    firebase login --no-localhost
}

# Verificar projeto
Write-Host ""
Write-Host "[3/6] Verificando projeto Firebase..." -ForegroundColor Yellow
$firebaseRc = Get-Content ".firebaserc" -ErrorAction SilentlyContinue
if ($firebaseRc -match "task-go-ee85f") {
    Write-Host "✅ Projeto configurado no .firebaserc" -ForegroundColor Green
} else {
    Write-Host "⚠️  Configurando projeto..." -ForegroundColor Yellow
    firebase use task-go-ee85f
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Erro ao configurar projeto" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Projeto configurado" -ForegroundColor Green
}

# Verificar databases
Write-Host ""
Write-Host "[4/6] Verificando databases do Firestore..." -ForegroundColor Yellow
try {
    $databases = firebase firestore:databases:list --project=task-go-ee85f 2>&1
    Write-Host "Databases encontrados:" -ForegroundColor White
    Write-Host $databases -ForegroundColor White
    
    if ($databases -match "taskgo") {
        Write-Host "✅ Database 'taskgo' encontrado" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Database 'taskgo' não encontrado na lista" -ForegroundColor Yellow
        Write-Host "   Verifique no Firebase Console se o database está criado" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Erro ao listar databases: $_" -ForegroundColor Yellow
    Write-Host "   Isso pode ser normal se a API ainda não estiver habilitada" -ForegroundColor Yellow
}

# Deploy das regras (para garantir que estão atualizadas)
Write-Host ""
Write-Host "[5/6] Verificando Firestore Rules..." -ForegroundColor Yellow
if (Test-Path "firestore.rules") {
    Write-Host "✅ Arquivo firestore.rules encontrado" -ForegroundColor Green
    Write-Host "Fazendo deploy das regras..." -ForegroundColor Yellow
    firebase deploy --only firestore:rules --project=task-go-ee85f
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Firestore Rules deployadas com sucesso" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Aviso ao fazer deploy das regras (pode ser normal se já estiverem atualizadas)" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Arquivo firestore.rules não encontrado" -ForegroundColor Red
}

# Instruções finais
Write-Host ""
Write-Host "[6/6] Instruções para habilitar API manualmente..." -ForegroundColor Yellow
Write-Host ""
Write-Host "⚠️  IMPORTANTE: A Cloud Firestore API precisa ser habilitada no Google Cloud Console" -ForegroundColor Yellow
Write-Host ""
Write-Host "Opção 1 - Via Web (Recomendado):" -ForegroundColor Cyan
Write-Host "1. Acesse: https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f" -ForegroundColor White
Write-Host "2. Clique em 'ENABLE' (Habilitar)" -ForegroundColor White
Write-Host "3. Aguarde alguns minutos para a API ser ativada" -ForegroundColor White
Write-Host ""
Write-Host "Opção 2 - Via gcloud CLI (se instalado):" -ForegroundColor Cyan
Write-Host "gcloud services enable firestore.googleapis.com --project=task-go-ee85f" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ CONCLUÍDO!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Yellow
Write-Host "1. Habilite a Cloud Firestore API no link acima" -ForegroundColor White
Write-Host "2. Aguarde 2-5 minutos para a API ser totalmente ativada" -ForegroundColor White
Write-Host "3. Execute: .\verificar-firestore-config.ps1 para verificar" -ForegroundColor White
Write-Host "4. Teste o cadastro/login no app" -ForegroundColor White
Write-Host ""
