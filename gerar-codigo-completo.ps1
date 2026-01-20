# Script para gerar documento completo com todo o código do TaskGo
# Classifica Frontend vs Backend automaticamente

$outputFile = "CODIGO_COMPLETO_TASKGO.md"
$basePath = "app\src\main\java\com\taskgoapp\taskgo"

# Função para classificar se é Frontend ou Backend
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

# Função para obter seção do arquivo
function Get-Section {
    param($filePath)
    
    $relativePath = $filePath.Replace($basePath + "\", "")
    $parts = $relativePath.Split("\")
    
    if ($parts[0] -eq "feature") {
        return "Features - $($parts[1])"
    }
    if ($parts[0] -eq "core") {
        return "Core - $($parts[1])"
    }
    if ($parts[0] -eq "data") {
        return "Data Layer - $($parts[1])"
    }
    if ($parts[0] -eq "di") {
        return "Dependency Injection"
    }
    if ($parts[0] -eq "domain") {
        return "Domain Layer"
    }
    return "Outros"
}

# Cabeçalho do documento
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

# Escrever cabeçalho
$header | Out-File -FilePath $outputFile -Encoding UTF8

# Agrupar arquivos por seção
$files = Get-ChildItem -Path $basePath -Recurse -Filter "*.kt" | Sort-Object FullName
$sections = @{}

foreach ($file in $files) {
    $section = Get-Section $file.FullName
    if (-not $sections.ContainsKey($section)) {
        $sections[$section] = @()
    }
    $sections[$section] += $file
}

# Processar cada seção
foreach ($sectionName in ($sections.Keys | Sort-Object)) {
    $sectionFiles = $sections[$sectionName]
    
    # Título da seção
    $sectionTitle = "# $sectionName`n`n"
    Add-Content -Path $outputFile -Value $sectionTitle -Encoding UTF8
    
    # Processar cada arquivo na seção
    foreach ($file in $sectionFiles) {
        $classification = Classify-File $file.FullName
        $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
        
        Write-Host "Processando: $relativePath" -ForegroundColor Cyan
        
        # Título do arquivo
        $fileTitle = "`n## ${classification}: ${relativePath}`n`n"
        Add-Content -Path $outputFile -Value $fileTitle -Encoding UTF8
        
        # Código do arquivo
        $codeBlock = "```kotlin`n"
        Add-Content -Path $outputFile -Value $codeBlock -Encoding UTF8
        
        try {
            $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
            Add-Content -Path $outputFile -Value $content -Encoding UTF8 -NoNewline
        } catch {
            $errorMsg = "// ERRO AO LER ARQUIVO: $($_.Exception.Message)"
            Add-Content -Path $outputFile -Value $errorMsg -Encoding UTF8
        }
        
        $codeBlockEnd = "`n```"
        Add-Content -Path $outputFile -Value $codeBlockEnd -Encoding UTF8
        Add-Content -Path $outputFile -Value "`n" -Encoding UTF8
    }
}

Write-Host "`n✅ Documento gerado com sucesso: $outputFile" -ForegroundColor Green
Write-Host "Total de arquivos processados: $($files.Count)" -ForegroundColor Green
