# Script para corrigir automaticamente TaskGoCard quebrados
# Adiciona Column dentro do TaskGoCard quando necessário

Write-Host "========================================"
Write-Host "CORREÇÃO AUTOMÁTICA DE TASKGOCARD"
Write-Host "========================================"
Write-Host ""

$ErrorActionPreference = "Stop"

# Diretório base
$baseDir = "app\src\main\java\com\taskgoapp\taskgo"

# Encontrar todos os arquivos .kt
$files = Get-ChildItem -Path $baseDir -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }

Write-Host "Encontrados $($files.Count) arquivos Kotlin"
Write-Host ""

$totalCorrigidos = 0
$totalArquivos = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $modified = $false
    
    # Padrão 1: TaskGoCard com conteúdo direto (sem Column)
    # Procura por: TaskGoCard(...) { seguido de Text, Row, etc. sem Column
    $pattern1 = '(?s)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*(?!\s*Column\s*\()\s*(Text\s*\(|Row\s*\(|Spacer\s*\(|Button\s*\(|OutlinedButton\s*\(|Icon\s*\(|AsyncImage\s*\(|HorizontalDivider\s*\(|VerticalDivider\s*\(|Card\s*\()'
    
    if ($content -match $pattern1) {
        # Encontrou TaskGoCard quebrado - precisa adicionar Column
        Write-Host "Corrigindo: $($file.Name)"
        
        # Estratégia: encontrar TaskGoCard e adicionar Column dentro
        # Procurar por padrão mais específico
        $content = $content -replace '(?s)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*(?!\s*Column\s*\()\s*', {
            param($match)
            $cardStart = $match.Groups[1].Value
            $indent = "        "  # 8 espaços padrão
            
            # Verificar indentação do próximo elemento
            $afterMatch = $match.Groups[0].Value
            $lines = $afterMatch -split "`n"
            if ($lines.Count -gt 1) {
                $secondLine = $lines[1]
                if ($secondLine -match '^(\s+)') {
                    $indent = $matches[1]
                }
            }
            
            # Adicionar Column
            "$cardStart`n$indent    Column(`n$indent        modifier = Modifier.fillMaxWidth(),`n$indent        verticalArrangement = Arrangement.spacedBy(8.dp)`n$indent    ) {`n$indent        "
        }
        
        $modified = $true
    }
    
    # Padrão 2: TaskGoCard com estrutura quebrada (chaves fechando incorretamente)
    # Procura por: TaskGoCard(...) { seguido de ) { (estrutura quebrada)
    $pattern2 = '(?s)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*\)\s*\{'
    
    if ($content -match $pattern2) {
        Write-Host "Corrigindo estrutura quebrada: $($file.Name)"
        
        $content = $content -replace '(?s)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*\)\s*\{', {
            param($match)
            $cardStart = $match.Groups[1].Value
            $indent = "        "
            "$cardStart`n$indent    Column(`n$indent        modifier = Modifier.fillMaxWidth(),`n$indent        verticalArrangement = Arrangement.spacedBy(8.dp)`n$indent    ) {`n$indent        "
        }
        
        $modified = $true
    }
    
    # Padrão 3: TaskGoCard com indentação incorreta e conteúdo direto
    # Procura por TaskGoCard seguido de linha com Text/Row/etc sem Column
    $pattern3 = '(?m)^(\s*)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*$\s*^(\s+)(Text\s*\(|Row\s*\(|Spacer\s*\(|Button\s*\(|OutlinedButton\s*\(|Icon\s*\(|AsyncImage\s*\(|HorizontalDivider\s*\(|VerticalDivider\s*\(|Card\s*\()'
    
    if ($content -match $pattern3) {
        Write-Host "Corrigindo indentação: $($file.Name)"
        
        $content = $content -replace '(?m)^(\s*)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*$\s*^(\s+)(Text\s*\(|Row\s*\(|Spacer\s*\(|Button\s*\(|OutlinedButton\s*\(|Icon\s*\(|AsyncImage\s*\(|HorizontalDivider\s*\(|VerticalDivider\s*\(|Card\s*\()', {
            param($match)
            $cardIndent = $match.Groups[1].Value
            $cardLine = $match.Groups[2].Value
            $contentIndent = $match.Groups[3].Value
            $contentStart = $match.Groups[4].Value
            
            "$cardIndent$cardLine`n$contentIndent    Column(`n$contentIndent        modifier = Modifier.fillMaxWidth(),`n$contentIndent        verticalArrangement = Arrangement.spacedBy(8.dp)`n$contentIndent    ) {`n$contentIndent        "
        }
        
        $modified = $true
    }
    
    if ($modified) {
        # Salvar arquivo
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $totalCorrigidos++
        Write-Host "  ✓ Corrigido" -ForegroundColor Green
    }
    
    $totalArquivos++
}

Write-Host ""
Write-Host "========================================"
Write-Host "CORREÇÃO CONCLUÍDA"
Write-Host "========================================"
Write-Host "Arquivos processados: $totalArquivos"
Write-Host "Arquivos corrigidos: $totalCorrigidos"
Write-Host ""

if ($totalCorrigidos -gt 0) {
    Write-Host "Execute o build para verificar se há mais erros."
} else {
    Write-Host "Nenhum arquivo precisou de correção automática."
}
