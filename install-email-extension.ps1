# Script para instalar a extensão Trigger Email from Firestore
# Uso: .\install-email-extension.ps1

param(
    [string]$ProjectId = "task-go-ee85f",
    [string]$Location = "",
    [switch]$UninstallOnly,
    [switch]$CheckOnly
)

$EXTENSION_ID = "firebase/firestore-send-email"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instalação da Extensão de Email" -ForegroundColor Cyan
Write-Host "  Trigger Email from Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version 2>&1
    Write-Host "✅ Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Firebase CLI não encontrado. Instale com: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

# Verificar se gcloud CLI está instalado
try {
    $gcloudVersion = gcloud --version 2>&1 | Select-Object -First 1
    Write-Host "✅ gcloud CLI encontrado: $gcloudVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ gcloud CLI não encontrado. Instale do site oficial do Google Cloud." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Passo 1: Verificar região do Firestore
Write-Host "🔍 Passo 1: Verificando região do Firestore..." -ForegroundColor Cyan
Write-Host ""

try {
    $databases = gcloud firestore databases list --project=$ProjectId --format=json 2>&1 | ConvertFrom-Json
    
    if ($databases.Count -eq 0) {
        Write-Host "❌ Nenhum banco de dados Firestore encontrado!" -ForegroundColor Red
        exit 1
    }
    
    $defaultDb = $databases | Where-Object { $_.name -eq "(default)" }
    
    if (-not $defaultDb) {
        Write-Host "❌ Banco de dados '(default)' não encontrado!" -ForegroundColor Red
        Write-Host "Bancos encontrados:" -ForegroundColor Yellow
        $databases | ForEach-Object { Write-Host "  - $($_.name) em $($_.locationId)" }
        exit 1
    }
    
    $firestoreLocation = $defaultDb.locationId
    Write-Host "✅ Firestore encontrado na região: $firestoreLocation" -ForegroundColor Green
    
    # Se Location não foi especificada, usar a do Firestore
    if ([string]::IsNullOrEmpty($Location)) {
        $Location = $firestoreLocation
        Write-Host "📍 Usando região do Firestore: $Location" -ForegroundColor Yellow
    } else {
        if ($Location -ne $firestoreLocation) {
            Write-Host "⚠️ AVISO: Região especificada ($Location) difere da região do Firestore ($firestoreLocation)" -ForegroundColor Yellow
            Write-Host "   Isso pode causar problemas. Recomendado usar: $firestoreLocation" -ForegroundColor Yellow
            $confirm = Read-Host "   Continuar mesmo assim? (s/N)"
            if ($confirm -ne "s" -and $confirm -ne "S") {
                Write-Host "❌ Operação cancelada." -ForegroundColor Red
                exit 0
            }
        }
    }
    
} catch {
    Write-Host "❌ Erro ao verificar região do Firestore: $_" -ForegroundColor Red
    exit 1
}

if ($CheckOnly) {
    Write-Host ""
    Write-Host "✅ Verificação concluída. Região: $Location" -ForegroundColor Green
    exit 0
}

Write-Host ""

# Passo 2: Verificar extensões instaladas
Write-Host "🔍 Passo 2: Verificando extensões instaladas..." -ForegroundColor Cyan
Write-Host ""

try {
    $extensions = firebase ext:list --project=$ProjectId --json 2>&1 | ConvertFrom-Json
    
    if ($extensions.result -and $extensions.result.Count -gt 0) {
        Write-Host "Extensões encontradas:" -ForegroundColor Yellow
        $emailExtensions = $extensions.result | Where-Object { $_.ref -like "*firestore-send-email*" }
        
        if ($emailExtensions.Count -gt 0) {
            foreach ($ext in $emailExtensions) {
                Write-Host "  - $($ext.ref) (ID: $($ext.instanceId))" -ForegroundColor Yellow
            }
            
            if (-not $UninstallOnly) {
                Write-Host ""
                Write-Host "⚠️ Extensão de email já instalada!" -ForegroundColor Yellow
                $action = Read-Host "Deseja desinstalar antes de reinstalar? (s/N)"
                
                if ($action -eq "s" -or $action -eq "S") {
                    foreach ($ext in $emailExtensions) {
                        Write-Host "🗑️ Desinstalando: $($ext.instanceId)..." -ForegroundColor Yellow
                        firebase ext:uninstall $ext.instanceId --project=$ProjectId --force 2>&1 | Out-Null
                        Write-Host "✅ Desinstalado: $($ext.instanceId)" -ForegroundColor Green
                    }
                } else {
                    Write-Host "❌ Operação cancelada. Desinstale manualmente primeiro." -ForegroundColor Red
                    exit 0
                }
            } else {
                foreach ($ext in $emailExtensions) {
                    Write-Host "🗑️ Desinstalando: $($ext.instanceId)..." -ForegroundColor Yellow
                    firebase ext:uninstall $ext.instanceId --project=$ProjectId --force 2>&1 | Out-Null
                    Write-Host "✅ Desinstalado: $($ext.instanceId)" -ForegroundColor Green
                }
                Write-Host ""
                Write-Host "✅ Desinstalação concluída!" -ForegroundColor Green
                exit 0
            }
        } else {
            Write-Host "✅ Nenhuma extensão de email encontrada." -ForegroundColor Green
        }
    } else {
        Write-Host "✅ Nenhuma extensão instalada." -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️ Não foi possível listar extensões (pode ser normal se não houver nenhuma): $_" -ForegroundColor Yellow
}

if ($UninstallOnly) {
    exit 0
}

Write-Host ""

# Passo 3: Verificar APIs habilitadas
Write-Host "✅ Passo 3: Verificando APIs necessárias..." -ForegroundColor Cyan
Write-Host ""

$requiredApis = @(
    "cloudfunctions.googleapis.com",
    "firestore.googleapis.com",
    "cloudbuild.googleapis.com",
    "secretmanager.googleapis.com",
    "run.googleapis.com"
)

foreach ($api in $requiredApis) {
    try {
        $status = gcloud services list --enabled --project=$ProjectId --filter="name:$api" --format="value(name)" 2>&1
        
        if ($status -like "*$api*") {
            Write-Host "  ✅ $api" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️ Habilitando $api..." -ForegroundColor Yellow
            gcloud services enable $api --project=$ProjectId 2>&1 | Out-Null
            Write-Host "  ✅ $api habilitada" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⚠️ Erro ao verificar/habilitar $api: $_" -ForegroundColor Yellow
    }
}

Write-Host ""

# Passo 4: Instalar extensão
Write-Host "📦 Passo 4: Instalando extensão..." -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️ IMPORTANTE: Você precisará fornecer os seguintes parâmetros durante a instalação:" -ForegroundColor Yellow
Write-Host "   - SMTP Connection URI (ex: smtps://user:pass@smtp.example.com:465)" -ForegroundColor Yellow
Write-Host "   - Default From Email" -ForegroundColor Yellow
Write-Host "   - Default Reply To Email" -ForegroundColor Yellow
Write-Host ""
Write-Host "📍 Região que será usada: $Location" -ForegroundColor Cyan
Write-Host ""

$confirm = Read-Host "Continuar com a instalação? (s/N)"
if ($confirm -ne "s" -and $confirm -ne "S") {
    Write-Host "❌ Operação cancelada." -ForegroundColor Red
    exit 0
}

Write-Host ""
Write-Host "Iniciando instalação interativa..." -ForegroundColor Green
Write-Host ""

try {
    firebase ext:install $EXTENSION_ID --project=$ProjectId
    
    Write-Host ""
    Write-Host "✅ Instalação iniciada!" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️ NOTA: A instalação pode levar alguns minutos para completar." -ForegroundColor Yellow
    Write-Host "   Verifique o progresso no Firebase Console ou com:" -ForegroundColor Yellow
    Write-Host "   firebase ext:list --project=$ProjectId" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ Erro durante a instalação: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Dicas:" -ForegroundColor Yellow
    Write-Host "   1. Verifique se você tem permissões no projeto" -ForegroundColor Yellow
    Write-Host "   2. Verifique se o billing está habilitado" -ForegroundColor Yellow
    Write-Host "   3. Tente instalar manualmente: firebase ext:install $EXTENSION_ID --project=$ProjectId" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Passo 5: Verificar instalação
Write-Host "🔍 Passo 5: Verificando instalação..." -ForegroundColor Cyan
Write-Host ""

Start-Sleep -Seconds 5

try {
    $extensions = firebase ext:list --project=$ProjectId --json 2>&1 | ConvertFrom-Json
    
    if ($extensions.result) {
        $installed = $extensions.result | Where-Object { $_.ref -like "*firestore-send-email*" }
        
        if ($installed.Count -gt 0) {
            Write-Host "✅ Extensão instalada:" -ForegroundColor Green
            foreach ($ext in $installed) {
                Write-Host "   - $($ext.ref)" -ForegroundColor Green
                Write-Host "     Estado: $($ext.state)" -ForegroundColor Cyan
            }
        } else {
            Write-Host "⚠️ Extensão ainda não aparece na lista (pode estar sendo instalada)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "⚠️ Não foi possível verificar status: $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instalação Concluída!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Próximos passos:" -ForegroundColor Yellow
Write-Host "   1. Verifique o status: firebase ext:list --project=$ProjectId" -ForegroundColor Cyan
Write-Host "   2. Verifique as Cloud Functions: gcloud functions list --project=$ProjectId" -ForegroundColor Cyan
Write-Host "   3. Configure os parâmetros SMTP se necessário" -ForegroundColor Cyan
Write-Host "   4. Teste enviando um email através do Firestore" -ForegroundColor Cyan
Write-Host ""

















