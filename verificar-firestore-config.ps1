# Script para verificar configuração completa do Firestore
# Projeto: task-go-ee85f

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICANDO CONFIGURAÇÃO FIRESTORE" -ForegroundColor Cyan
Write-Host "Projeto: task-go-ee85f" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar projeto atual
Write-Host "[1/6] Verificando projeto atual..." -ForegroundColor Yellow
$currentProject = gcloud config get-value project 2>&1
if ($currentProject -match "task-go-ee85f") {
    Write-Host "✅ Projeto configurado: $currentProject" -ForegroundColor Green
} else {
    Write-Host "⚠️  Projeto atual: $currentProject" -ForegroundColor Yellow
    Write-Host "Configurando para task-go-ee85f..." -ForegroundColor Yellow
    gcloud config set project task-go-ee85f
    Write-Host "✅ Projeto configurado" -ForegroundColor Green
}

# Verificar APIs
Write-Host ""
Write-Host "[2/6] Verificando APIs habilitadas..." -ForegroundColor Yellow
$apis = @(
    "firestore.googleapis.com",
    "firebase.googleapis.com",
    "identitytoolkit.googleapis.com",
    "securetoken.googleapis.com"
)

foreach ($api in $apis) {
    $enabled = gcloud services list --enabled --filter="name:$api" --format="value(name)" 2>&1
    if ($enabled -match $api) {
        Write-Host "✅ $api" -ForegroundColor Green
    } else {
        Write-Host "❌ $api NÃO HABILITADA" -ForegroundColor Red
    }
}

# Verificar databases via Firebase CLI
Write-Host ""
Write-Host "[3/6] Verificando databases do Firestore..." -ForegroundColor Yellow
try {
    $databases = firebase firestore:databases:list --project=task-go-ee85f 2>&1
    Write-Host "Databases encontrados:" -ForegroundColor White
    Write-Host $databases -ForegroundColor White
    
    if ($databases -match "taskgo") {
        Write-Host "✅ Database 'taskgo' encontrado" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Database 'taskgo' não encontrado" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Erro ao listar databases: $_" -ForegroundColor Yellow
}

# Verificar regras do Firestore
Write-Host ""
Write-Host "[4/6] Verificando Firestore Rules..." -ForegroundColor Yellow
if (Test-Path "firestore.rules") {
    Write-Host "✅ Arquivo firestore.rules encontrado" -ForegroundColor Green
    
    # Verificar se está deployado
    try {
        $rules = firebase firestore:rules:get --project=task-go-ee85f 2>&1
        if ($rules) {
            Write-Host "✅ Firestore Rules estão deployadas" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Não foi possível verificar regras deployadas" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠️  Erro ao verificar regras: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Arquivo firestore.rules não encontrado" -ForegroundColor Red
}

# Verificar configuração do Firebase
Write-Host ""
Write-Host "[5/6] Verificando configuração do Firebase..." -ForegroundColor Yellow
if (Test-Path "app\google-services.json") {
    Write-Host "✅ google-services.json encontrado" -ForegroundColor Green
    
    # Verificar se contém a API key correta
    $googleServices = Get-Content "app\google-services.json" -Raw | ConvertFrom-Json
    $apiKey = $googleServices.project_info.api_key.current_key
    if ($apiKey -eq "AIzaSyD9JIxB5lzJUou1hUHBxNMGC4DVjEtIY_k") {
        Write-Host "✅ API Key correta no google-services.json" -ForegroundColor Green
    } else {
        Write-Host "⚠️  API Key no google-services.json: $apiKey" -ForegroundColor Yellow
        Write-Host "   Esperado: AIzaSyD9JIxB5lzJUou1hUHBxNMGC4DVjEtIY_k" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ google-services.json não encontrado" -ForegroundColor Red
}

# Verificar se está logado no Firebase
Write-Host ""
Write-Host "[6/6] Verificando autenticação Firebase..." -ForegroundColor Yellow
try {
    $firebaseUser = firebase login:list --json 2>&1 | ConvertFrom-Json
    if ($firebaseUser) {
        Write-Host "✅ Autenticado no Firebase" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Não autenticado no Firebase. Execute: firebase login" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Erro ao verificar autenticação: $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ VERIFICAÇÃO CONCLUÍDA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
