# Script PowerShell para Ativar Todas as APIs Faltantes
# Execute este script no PowerShell como administrador

$PROJECT_ID = "task-go-ee85f"

Write-Host "=== Ativando APIs Faltantes para TaskGo App ===" -ForegroundColor Cyan
Write-Host "Projeto: $PROJECT_ID" -ForegroundColor Yellow
Write-Host ""

# APIs CRÍTICAS (Prioridade Alta)
$CRITICAL_APIS = @(
    @{Name="Cloud Functions API"; ID="cloudfunctions.googleapis.com"},
    @{Name="Cloud Pub/Sub API"; ID="pubsub.googleapis.com"},
    @{Name="Cloud Scheduler API"; ID="cloudscheduler.googleapis.com"},
    @{Name="Generative Language API"; ID="generativelanguage.googleapis.com"},
    @{Name="Firebase Realtime Database API"; ID="firebasedatabase.googleapis.com"}
)

# APIs IMPORTANTES (Prioridade Média)
$IMPORTANT_APIS = @(
    @{Name="Cloud Build API"; ID="cloudbuild.googleapis.com"},
    @{Name="Artifact Registry API"; ID="artifactregistry.googleapis.com"},
    @{Name="Secret Manager API"; ID="secretmanager.googleapis.com"}
)

# APIs OPCIONAIS (Prioridade Baixa)
$OPTIONAL_APIS = @(
    @{Name="Cloud Translation API"; ID="translate.googleapis.com"},
    @{Name="Cloud Resource Manager API"; ID="cloudresourcemanager.googleapis.com"},
    @{Name="Service Usage API"; ID="serviceusage.googleapis.com"},
    @{Name="Cloud Monitoring API"; ID="monitoring.googleapis.com"},
    @{Name="Cloud Trace API"; ID="cloudtrace.googleapis.com"}
)

function Enable-API {
    param(
        [string]$ApiName,
        [string]$ApiId
    )
    
    Write-Host "Ativando: $ApiName..." -ForegroundColor White -NoNewline
    
    try {
        $result = gcloud services enable $ApiId --project=$PROJECT_ID 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✅ ATIVADA" -ForegroundColor Green
            return $true
        } else {
            Write-Host " ❌ ERRO" -ForegroundColor Red
            Write-Host "   $result" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host " ❌ EXCEÇÃO" -ForegroundColor Red
        Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Ativar APIs Críticas
Write-Host "=== APIs CRÍTICAS ===" -ForegroundColor Red
Write-Host ""
$criticalCount = 0
foreach ($api in $CRITICAL_APIS) {
    if (Enable-API -ApiName $api.Name -ApiId $api.ID) {
        $criticalCount++
    }
    Start-Sleep -Seconds 1
}

Write-Host ""
Write-Host "APIs Críticas Ativadas: $criticalCount / $($CRITICAL_APIS.Count)" -ForegroundColor $(if ($criticalCount -eq $CRITICAL_APIS.Count) { "Green" } else { "Yellow" })
Write-Host ""

# Ativar APIs Importantes
Write-Host "=== APIs IMPORTANTES ===" -ForegroundColor Yellow
Write-Host ""
$importantCount = 0
foreach ($api in $IMPORTANT_APIS) {
    if (Enable-API -ApiName $api.Name -ApiId $api.ID) {
        $importantCount++
    }
    Start-Sleep -Seconds 1
}

Write-Host ""
Write-Host "APIs Importantes Ativadas: $importantCount / $($IMPORTANT_APIS.Count)" -ForegroundColor $(if ($importantCount -eq $IMPORTANT_APIS.Count) { "Green" } else { "Yellow" })
Write-Host ""

# Perguntar se deseja ativar APIs Opcionais
Write-Host "=== APIs OPCIONAIS ===" -ForegroundColor Green
$activateOptional = Read-Host "Deseja ativar as APIs opcionais também? (S/N)"

if ($activateOptional -eq "S" -or $activateOptional -eq "s" -or $activateOptional -eq "Y" -or $activateOptional -eq "y") {
    Write-Host ""
    $optionalCount = 0
    foreach ($api in $OPTIONAL_APIS) {
        if (Enable-API -ApiName $api.Name -ApiId $api.ID) {
            $optionalCount++
        }
        Start-Sleep -Seconds 1
    }
    
    Write-Host ""
    Write-Host "APIs Opcionais Ativadas: $optionalCount / $($OPTIONAL_APIS.Count)" -ForegroundColor Green
} else {
    Write-Host "APIs opcionais ignoradas." -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== RESUMO FINAL ===" -ForegroundColor Cyan
Write-Host "APIs Críticas: $criticalCount / $($CRITICAL_APIS.Count)" -ForegroundColor $(if ($criticalCount -eq $CRITICAL_APIS.Count) { "Green" } else { "Red" })
Write-Host "APIs Importantes: $importantCount / $($IMPORTANT_APIS.Count)" -ForegroundColor $(if ($importantCount -eq $IMPORTANT_APIS.Count) { "Green" } else { "Yellow" })
Write-Host "APIs Opcionais: $(if ($activateOptional -eq "S" -or $activateOptional -eq "s" -or $activateOptional -eq "Y" -or $activateOptional -eq "y") { $optionalCount } else { "Ignoradas" })" -ForegroundColor Green
Write-Host ""
Write-Host "=== Script concluído ===" -ForegroundColor Cyan







