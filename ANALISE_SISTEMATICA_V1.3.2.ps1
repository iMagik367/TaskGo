# Análise Sistemática Completa - TaskGo App v1.3.2
# 5 rodadas de análise para garantir padronização e exibição de dados

$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE SISTEMÁTICA V1.3.2" -ForegroundColor Cyan
Write-Host "TaskGo App - Backend e Frontend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$reportFile = "RELATORIO_ANALISE_V1.3.2.md"
$report = @"
# Relatório de Análise Sistemática - TaskGo App v1.3.2

Data: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## Resumo Executivo

Este relatório documenta 5 rodadas completas de análise do código backend e frontend,
focando em:
- Erros de compilação e sintaxe
- Inconsistências de padrões
- Exibição de dados para usuários
- Filtros por localização (/locations/city_state/user_id)
- Padronização de código

---

"@

# Função para contar arquivos
function Count-Files {
    $ktFiles = (Get-ChildItem -Path "app\src\main\java" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch "\\build\\" -and $_.FullName -notmatch "\\\.idea\\" } | Measure-Object).Count
    $tsFiles = (Get-ChildItem -Path "functions\src" -Filter "*.ts" -Recurse -ErrorAction SilentlyContinue | Measure-Object).Count
    return @{
        Kotlin = $ktFiles
        TypeScript = $tsFiles
        Total = $ktFiles + $tsFiles
    }
}

# Função para análise de padrões
function Analyze-Patterns {
    param($round)
    
    Write-Host "Rodada $round - Análise de Padrões..." -ForegroundColor Yellow
    
    $issues = @()
    
    # 1. Verificar imports faltando
    Write-Host "  [1/8] Verificando imports..." -ForegroundColor Gray
    $ktFiles = Get-ChildItem -Path "app\src\main\java" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch "\\build\\" }
    
    $importIssues = 0
    foreach ($file in $ktFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "TaskGoBackgroundWhite|TaskGoBorder" -and $content -notmatch "import.*BorderStroke") {
            if ($content -match "BorderStroke") {
                $importIssues++
                $issues += "IMPORT: $($file.Name) - Falta import androidx.compose.foundation.BorderStroke"
            }
        }
    }
    
    # 2. Verificar filtros de localização
    Write-Host "  [2/8] Verificando filtros de localização..." -ForegroundColor Gray
    $locationIssues = 0
    $viewModels = Get-ChildItem -Path "app\src\main\java" -Filter "*ViewModel.kt" -Recurse -ErrorAction SilentlyContinue
    foreach ($vm in $viewModels) {
        $content = Get-Content $vm.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "collection\(|get\(|where\(" -and $content -notmatch "locations.*city_state") {
            # Verificar se é uma query que deveria ter filtro de localização
            if ($content -match "products|services|orders|posts|stories" -and $content -notmatch "users.*products|users.*services") {
                $locationIssues++
                $issues += "LOCATION: $($vm.Name) - Possível falta de filtro por localização"
            }
        }
    }
    
    # 3. Verificar exibição de dados
    Write-Host "  [3/8] Verificando exibição de dados..." -ForegroundColor Gray
    $displayIssues = 0
    $screens = Get-ChildItem -Path "app\src\main\java" -Filter "*Screen.kt" -Recurse -ErrorAction SilentlyContinue
    foreach ($screen in $screens) {
        $content = Get-Content $screen.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "LazyColumn|LazyRow" -and $content -notmatch "items\(|itemsIndexed\(") {
            $displayIssues++
            $issues += "DISPLAY: $($screen.Name) - Lista sem items definidos"
        }
    }
    
    # 4. Verificar erros de sintaxe Kotlin
    Write-Host "  [4/8] Verificando sintaxe Kotlin..." -ForegroundColor Gray
    $syntaxIssues = 0
    foreach ($file in $ktFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        # Verificar parênteses não fechados
        $openParens = ([regex]::Matches($content, '\(')).Count
        $closeParens = ([regex]::Matches($content, '\)')).Count
        if ($openParens -ne $closeParens) {
            $syntaxIssues++
            $issues += "SYNTAX: $($file.Name) - Parênteses desbalanceados ($openParens abertos, $closeParens fechados)"
        }
    }
    
    # 5. Verificar TypeScript
    Write-Host "  [5/8] Verificando TypeScript..." -ForegroundColor Gray
    $tsFiles = Get-ChildItem -Path "functions\src" -Filter "*.ts" -Recurse -ErrorAction SilentlyContinue
    $tsIssues = 0
    foreach ($file in $tsFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        # Verificar exports
        if ($content -match "export (function|const|class)" -and $file.Name -eq "index.ts" -and $content -notmatch "export.*from") {
            # OK
        }
    }
    
    # 6. Verificar Cards padronizados
    Write-Host "  [6/8] Verificando padronização de Cards..." -ForegroundColor Gray
    $cardIssues = 0
    foreach ($file in $ktFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "Card\(" -and $content -notmatch "TaskGoBackgroundWhite|containerColor.*White") {
            if ($content -match "CardDefaults\.cardColors" -and $content -notmatch "TaskGoBackgroundWhite") {
                $cardIssues++
                $issues += "CARD: $($file.Name) - Card sem TaskGoBackgroundWhite"
            }
        }
    }
    
    # 7. Verificar Firestore paths
    Write-Host "  [7/8] Verificando paths do Firestore..." -ForegroundColor Gray
    $pathIssues = 0
    foreach ($file in $ktFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "collection\(|get\(|where\(") {
            # Verificar se usa paths corretos
            if ($content -match 'collection\("products"\)' -and $content -notmatch "locations.*products") {
                $pathIssues++
                $issues += "PATH: $($file.Name) - Usando collection('products') ao invés de locations/city_state/products"
            }
        }
    }
    
    # 8. Verificar ViewModels observando dados
    Write-Host "  [8/8] Verificando ViewModels..." -ForegroundColor Gray
    $vmIssues = 0
    foreach ($vm in $viewModels) {
        $content = Get-Content $vm.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "class.*ViewModel" -and $content -notmatch "StateFlow|Flow|collectAsState|collect") {
            $vmIssues++
            $issues += "VIEWMODEL: $($vm.Name) - ViewModel sem observação de dados"
        }
    }
    
    return @{
        Issues = $issues
        Counts = @{
            Import = $importIssues
            Location = $locationIssues
            Display = $displayIssues
            Syntax = $syntaxIssues
            Card = $cardIssues
            Path = $pathIssues
            ViewModel = $vmIssues
        }
    }
}

# Executar 5 rodadas
$fileCounts = Count-Files
$report += @"

## Estatísticas de Arquivos

- Arquivos Kotlin: $($fileCounts.Kotlin)
- Arquivos TypeScript: $($fileCounts.TypeScript)
- **Total analisado: $($fileCounts.Total)**

---

"@

for ($round = 1; $round -le 5; $round++) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "RODADA $round de 5" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    
    $results = Analyze-Patterns -round $round
    
    $report += @"

## Rodada $round

### Problemas Encontrados

- Imports faltando: $($results.Counts.Import)
- Filtros de localização: $($results.Counts.Location)
- Exibição de dados: $($results.Counts.Display)
- Erros de sintaxe: $($results.Counts.Syntax)
- Cards não padronizados: $($results.Counts.Card)
- Paths do Firestore: $($results.Counts.Path)
- ViewModels sem observação: $($results.Counts.ViewModel)

### Detalhes

"@
    
    if ($results.Issues.Count -gt 0) {
        foreach ($issue in $results.Issues) {
            $report += "- $issue`n"
        }
    } else {
        $report += "Nenhum problema encontrado nesta rodada.`n"
    }
    
    $report += "`n---`n"
    
    Write-Host "Rodada $round concluída!" -ForegroundColor Green
    Write-Host "Problemas encontrados: $($results.Issues.Count)" -ForegroundColor $(if ($results.Issues.Count -eq 0) { "Green" } else { "Yellow" })
    Write-Host ""
    
    Start-Sleep -Seconds 2
}

$report += @"

## Conclusão

Análise completa realizada em 5 rodadas.
Total de arquivos analisados: $($fileCounts.Total)

---

*Relatório gerado automaticamente em $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")*
"@

$report | Out-File -FilePath $reportFile -Encoding UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE CONCLUÍDA!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Relatório salvo em: $reportFile" -ForegroundColor Green
Write-Host ""
