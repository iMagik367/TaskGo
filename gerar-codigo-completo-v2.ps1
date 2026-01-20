# Script para gerar documento completo com todo o codigo do TaskGo
# Classifica Frontend vs Backend automaticamente

$outputFile = "CODIGO_COMPLETO_TASKGO.md"
$basePath = "app\src\main\java\com\taskgoapp\taskgo"
$ErrorActionPreference = "Continue"

# Funcao para classificar se e Frontend ou Backend
function Classify-File {
    param($filePath)
    
    if ($filePath -like "*\functions\*") {
        return "[BACKEND]"
    }
    if ($filePath -like "*\firestore.rules*" -or $filePath -like "*\firestore.indexes*") {
        return "[BACKEND]"
    }
    return "[FRONTEND]"
}

# Funcao para obter secao do arquivo
function Get-Section {
    param($filePath)
    
    $relativePath = $filePath.Replace($basePath + "\", "")
    $parts = $relativePath.Split("\")
    
    if ($parts[0] -eq "feature") {
        if ($parts.Length -gt 1) {
            return "Features - $($parts[1])"
        }
        return "Features"
    }
    if ($parts[0] -eq "core") {
        if ($parts.Length -gt 1) {
            return "Core - $($parts[1])"
        }
        return "Core"
    }
    if ($parts[0] -eq "data") {
        if ($parts.Length -gt 1) {
            return "Data Layer - $($parts[1])"
        }
        return "Data Layer"
    }
    if ($parts[0] -eq "di") {
        return "Dependency Injection"
    }
    if ($parts[0] -eq "domain") {
        return "Domain Layer"
    }
    return "Outros"
}

# Cabecalho do documento
$dateStr = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$header = @"
# Codigo Completo - TaskGo App
## Analise Completa de Todo o Codigo

**Data de Criacao**: $dateStr
**Versao**: 1.0.97
**Status**: Codigo Completo para Analise

---

# CLASSIFICACAO: FRONTEND vs BACKEND

## [FRONTEND] (Android App)
- Todas as Features (Telas, ViewModels, Composables)
- Data Layer (Repositorios, Models)
- Core (Location, Firebase Helpers, Utils)
- Dependency Injection (Hilt Modules)
- Domain Layer (Interfaces, Use Cases)

## [BACKEND] (Firebase)
- Cloud Functions (functions/)
- Firestore Security Rules (firestore.rules)
- Firestore Indexes (firestore.indexes.json)

---

"@

# Escrever cabecalho
[System.IO.File]::WriteAllText($outputFile, $header, [System.Text.Encoding]::UTF8)

# Agrupar arquivos por secao
Write-Host "Buscando arquivos..." -ForegroundColor Cyan
$files = Get-ChildItem -Path $basePath -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue | Sort-Object FullName
Write-Host "Encontrados $($files.Count) arquivos" -ForegroundColor Green

$sections = @{}

foreach ($file in $files) {
    try {
        $section = Get-Section $file.FullName
        if (-not $sections.ContainsKey($section)) {
            $sections[$section] = @()
        }
        $sections[$section] += $file
    } catch {
        Write-Host "Erro ao processar arquivo: $($file.FullName) - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "Processando $($sections.Keys.Count) secoes..." -ForegroundColor Cyan

$totalProcessed = 0
$totalErrors = 0

# Processar cada secao
foreach ($sectionName in ($sections.Keys | Sort-Object)) {
    $sectionFiles = $sections[$sectionName]
    
    Write-Host "`nSecao: $sectionName ($($sectionFiles.Count) arquivos)" -ForegroundColor Yellow
    
    # Titulo da secao
    $newline = [Environment]::NewLine
    $sectionTitle = $newline + "# $sectionName" + $newline + $newline
    [System.IO.File]::AppendAllText($outputFile, $sectionTitle, [System.Text.Encoding]::UTF8)
    
    # Processar cada arquivo na secao
    foreach ($file in $sectionFiles) {
        try {
            $classification = Classify-File $file.FullName
            $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
            
            Write-Host "  Processando: $relativePath" -ForegroundColor Gray
            
            # Titulo do arquivo
            $newline = [Environment]::NewLine
            $fileTitle = $newline + "## ${classification}: ${relativePath}" + $newline + $newline
            [System.IO.File]::AppendAllText($outputFile, $fileTitle, [System.Text.Encoding]::UTF8)
            
            # Codigo do arquivo
            $newline = [Environment]::NewLine
            $codeBlock = "```kotlin" + $newline
            [System.IO.File]::AppendAllText($outputFile, $codeBlock, [System.Text.Encoding]::UTF8)
            
            try {
                $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
                [System.IO.File]::AppendAllText($outputFile, $content, [System.Text.Encoding]::UTF8)
            } catch {
                $newline = [Environment]::NewLine
                $errorMsg = "// ERRO AO LER ARQUIVO: $($_.Exception.Message)" + $newline
                [System.IO.File]::AppendAllText($outputFile, $errorMsg, [System.Text.Encoding]::UTF8)
                $totalErrors++
            }
            
            $newline = [Environment]::NewLine
            $codeBlockEnd = $newline + "```" + $newline
            [System.IO.File]::AppendAllText($outputFile, $codeBlockEnd, [System.Text.Encoding]::UTF8)
            
            $totalProcessed++
            
            if ($totalProcessed % 10 -eq 0) {
                Write-Host "  Progresso: $totalProcessed/$($files.Count) arquivos processados" -ForegroundColor Cyan
            }
        } catch {
            Write-Host "  ERRO ao processar arquivo: $($file.FullName) - $($_.Exception.Message)" -ForegroundColor Red
            $totalErrors++
        }
    }
}

Write-Host "`n`n========================================" -ForegroundColor Green
Write-Host "Documento gerado com sucesso!" -ForegroundColor Green
Write-Host "Arquivo: $outputFile" -ForegroundColor White
Write-Host "Total de arquivos processados: $totalProcessed" -ForegroundColor White
Write-Host "Total de erros: $totalErrors" -ForegroundColor $(if ($totalErrors -gt 0) { "Yellow" } else { "Green" })
Write-Host "========================================" -ForegroundColor Green
