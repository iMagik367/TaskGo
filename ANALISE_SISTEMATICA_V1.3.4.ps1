# Script de Analise Sistematica V1.3.4
# Analisa arquivos do backend e frontend 5 vezes

Write-Host "Iniciando Analise Sistematica V1.3.4..." -ForegroundColor Cyan

$rounds = 5
$allIssues = @()
$kotlinFiles = Get-ChildItem -Path "app/src/main/java" -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName
$tsFiles = Get-ChildItem -Path "functions/src" -Recurse -Filter "*.ts" | Select-Object -ExpandProperty FullName
$allFiles = $kotlinFiles + $tsFiles

Write-Host "Total de arquivos encontrados: $($allFiles.Count)" -ForegroundColor Yellow

for ($round = 1; $round -le $rounds; $round++) {
    Write-Host ""
    Write-Host "RODADA $round de $rounds" -ForegroundColor Cyan
    Write-Host ("=" * 60) -ForegroundColor Gray
    
    $roundIssues = @()
    
    foreach ($file in $allFiles) {
        $content = Get-Content $file -Raw -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        
        $relativePath = $file.Replace((Get-Location).Path + "\", "")
        $fileIssues = @()
        
        # 1. Verificar imports faltando (Kotlin)
        if ($relativePath -match "\.kt$") {
            if ($content -match "TaskGoBackgroundWhite" -and $content -notmatch "import.*TaskGoBackgroundWhite") {
                $fileIssues += "Imports faltando: TaskGoBackgroundWhite"
            }
            if ($content -match "TaskGoBorder" -and $content -notmatch "import.*TaskGoBorder") {
                $fileIssues += "Imports faltando: TaskGoBorder"
            }
            if ($content -match "BorderStroke" -and $content -notmatch "import.*BorderStroke") {
                $fileIssues += "Import faltando: BorderStroke"
            }
        }
        
        # 2. Verificar uso de GPS para city/state (CRITICO)
        if ($content -match "getAddressFromLocation.*city" -or $content -match "getAddressFromLocation.*state") {
            $fileIssues += "CRITICO: GPS usado para city/state"
        }
        if ($content -match "getAddressGuaranteed.*city" -or $content -match "getAddressGuaranteed.*state") {
            $fileIssues += "CRITICO: GPS usado para city/state"
        }
        if ($content -match "LocationUpdateService.*startLocationMonitoring") {
            $fileIssues += "CRITICO: LocationUpdateService ainda sendo usado"
        }
        
        # 3. Verificar filtros de localizacao
        if ($content -match "observeProducts|observeServices|observePosts|observeStories") {
            if ($content -notmatch "locationId|locationCollection|locations/") {
                $fileIssues += "Verificar: Query pode nao estar usando locationId"
            }
        }
        
        # 4. Verificar city/state do perfil
        if ($content -match "createOrder|createService|createProduct|createStory|createPost") {
            if ($content -notmatch "user\.city|userCity|userState|getUserLocation|observeCurrentUser") {
                $fileIssues += "Verificar: Pode nao estar usando city/state do perfil"
            }
        }
        
        # 5. Verificar sintaxe basica (parenteses/chaves)
        $openParens = ([regex]::Matches($content, '\(')).Count
        $closeParens = ([regex]::Matches($content, '\)')).Count
        $openBraces = ([regex]::Matches($content, '\{')).Count
        $closeBraces = ([regex]::Matches($content, '\}')).Count
        if (($openParens -ne $closeParens) -or ($openBraces -ne $closeBraces)) {
            $fileIssues += "Verificar: Parenteses ou chaves podem estar desbalanceados"
        }
        
        # 6. Verificar referencias a colecoes
        if ($content -match "collection\(['\`"]users['\`"]\)") {
            if ($content -notmatch "locations/.*users") {
                $fileIssues += "Verificar: Pode estar usando colecao global users"
            }
        }
        
        # 7. Verificar ViewModels observando dados
        if ($relativePath -match "ViewModel\.kt$") {
            if ($content -match "observeProducts|observeServices|observePosts") {
                if ($content -notmatch "locationState|locationId") {
                    $fileIssues += "Verificar: ViewModel pode nao estar usando locationId"
                }
            }
        }
        
        # 8. Verificar regras de seguranca
        if ($relativePath -match "\.rules$|firestore\.rules") {
            if ($content -notmatch "locations/.*users.*allow read") {
                $fileIssues += "Verificar: Regras podem nao permitir leitura"
            }
        }
        
        # 9. Verificar TypeScript (Cloud Functions)
        if ($relativePath -match "\.ts$") {
            if ($content -match "createOrder|createService|createProduct") {
                if ($content -notmatch "getUserLocation") {
                    $fileIssues += "Verificar: Cloud Function pode nao estar usando getUserLocation"
                }
            }
        }
        
        # 10. Verificar padroes de dados
        if ($content -match "ProductFirestore|ServiceFirestore|PostFirestore") {
            if ($content -notmatch "locationId|locationCollection") {
                $fileIssues += "Verificar: Model pode nao ter locationId"
            }
        }
        
        if ($fileIssues.Count -gt 0) {
            $roundIssues += @{
                File = $relativePath
                Round = $round
                Issues = $fileIssues
            }
        }
    }
    
    $allIssues += $roundIssues
    $color = if ($roundIssues.Count -eq 0) { "Green" } else { "Yellow" }
    Write-Host "Rodada $round concluida: $($roundIssues.Count) arquivos com problemas" -ForegroundColor $color
}

Write-Host ""
Write-Host "RESUMO FINAL" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Gray

$groupedIssues = $allIssues | Group-Object File
$totalIssues = $groupedIssues.Count
$color = if ($totalIssues -eq 0) { "Green" } else { "Yellow" }
Write-Host "Total de arquivos com problemas: $totalIssues" -ForegroundColor $color

if ($totalIssues -gt 0) {
    Write-Host ""
    Write-Host "ARQUIVOS COM PROBLEMAS:" -ForegroundColor Yellow
    $groupedIssues | ForEach-Object {
        Write-Host ""
        Write-Host "$($_.Name)" -ForegroundColor Yellow
        $_.Group | ForEach-Object {
            Write-Host "  Rodada $($_.Round):" -ForegroundColor Gray
            $_.Issues | ForEach-Object {
                Write-Host "    $_" -ForegroundColor Red
            }
        }
    }
} else {
    Write-Host ""
    Write-Host "Nenhum problema encontrado apos 5 rodadas!" -ForegroundColor Green
}

# Salvar relatorio
$reportPath = "RELATORIO_ANALISE_V1.3.4_$(Get-Date -Format 'yyyyMMdd_HHmmss').md"
$report = @"
# Relatorio de Analise Sistematica V1.3.4

**Data:** $(Get-Date -Format 'dd/MM/yyyy HH:mm:ss')
**Total de Arquivos Analisados:** $($allFiles.Count)
**Total de Rodadas:** $rounds
**Arquivos com Problemas:** $totalIssues

## Problemas Encontrados

$(
    if ($totalIssues -gt 0) {
        $groupedIssues | ForEach-Object {
            "### $($_.Name)`n"
            $_.Group | ForEach-Object {
                "**Rodada $($_.Round):**`n"
                $_.Issues | ForEach-Object {
                    "- $_`n"
                }
            }
            "`n"
        }
    } else {
        "Nenhum problema encontrado!"
    }
)
"@

$report | Out-File $reportPath -Encoding UTF8
Write-Host ""
Write-Host "Relatorio salvo em: $reportPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "Analise Sistematica Concluida!" -ForegroundColor Green
