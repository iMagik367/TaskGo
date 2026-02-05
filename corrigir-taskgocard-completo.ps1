# Script robusto para corrigir TaskGoCard quebrados
# Analisa estrutura e corrige automaticamente

Write-Host "========================================"
Write-Host "CORREÇÃO COMPLETA DE TASKGOCARD"
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
    $lines = Get-Content $file.FullName -Encoding UTF8
    $newLines = @()
    $i = 0
    $modified = $false
    
    while ($i -lt $lines.Count) {
        $line = $lines[$i]
        $newLines += $line
        
        # Padrão 1: TaskGoCard com estrutura quebrada
        # Procura: TaskGoCard(...) { seguido de ) { na próxima linha
        if ($line -match '^\s*com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{?\s*$') {
            $indent = $line -replace '^(\s*).*', '$1'
            
            # Verificar próximas linhas
            if ($i + 1 -lt $lines.Count) {
                $nextLine = $lines[$i + 1]
                
                # Caso 1: Linha seguinte tem ) { (estrutura quebrada)
                if ($nextLine -match '^\s+\)\s*\{') {
                    Write-Host "Corrigindo estrutura quebrada em: $($file.Name) linha $($i + 2)"
                    $newLines = $newLines[0..($newLines.Count - 2)]  # Remove última linha
                    $newLines += "$indent    Column("
                    $newLines += "$indent        modifier = Modifier.fillMaxWidth(),"
                    $newLines += "$indent        verticalArrangement = Arrangement.spacedBy(8.dp)"
                    $newLines += "$indent    ) {"
                    $i++  # Pula a linha ) {
                    $modified = $true
                    continue
                }
                
                # Caso 2: Linha seguinte tem conteúdo direto (Text, Row, etc) sem Column
                if ($nextLine -match '^\s+(Text\s*\(|Row\s*\(|Spacer\s*\(|Button\s*\(|OutlinedButton\s*\(|Icon\s*\(|AsyncImage\s*\(|HorizontalDivider\s*\(|VerticalDivider\s*\(|Card\s*\()') {
                    # Verificar se não há Column nas próximas 5 linhas
                    $hasColumn = $false
                    for ($j = $i + 1; $j -lt [Math]::Min($i + 6, $lines.Count); $j++) {
                        if ($lines[$j] -match '^\s+Column\s*\(') {
                            $hasColumn = $true
                            break
                        }
                    }
                    
                    if (-not $hasColumn) {
                        Write-Host "Adicionando Column em: $($file.Name) linha $($i + 2)"
                        $newLines = $newLines[0..($newLines.Count - 2)]  # Remove última linha
                        $newLines += "$indent    Column("
                        $newLines += "$indent        modifier = Modifier.fillMaxWidth(),"
                        $newLines += "$indent        verticalArrangement = Arrangement.spacedBy(8.dp)"
                        $newLines += "$indent    ) {"
                        $modified = $true
                    }
                }
            }
        }
        
        # Padrão 2: TaskGoCard com indentação incorreta
        # Procura: TaskGoCard(...) { seguido de linha com indentação incorreta
        if ($line -match '^\s*com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{?\s*$') {
            $indent = $line -replace '^(\s*).*', '$1'
            
            if ($i + 1 -lt $lines.Count) {
                $nextLine = $lines[$i + 1]
                
                # Se próxima linha tem conteúdo mas não é Column e não fecha chave
                if ($nextLine -match '^\s{4,}(Text|Row|Spacer|Button|OutlinedButton|Icon|AsyncImage|HorizontalDivider|VerticalDivider|Card|if|when|for|while)\s') {
                    # Verificar se não há Column
                    $hasColumn = $false
                    for ($j = $i + 1; $j -lt [Math]::Min($i + 10, $lines.Count); $j++) {
                        if ($lines[$j] -match '^\s+Column\s*\(') {
                            $hasColumn = $true
                            break
                        }
                        if ($lines[$j] -match '^\s+\}') {
                            break
                        }
                    }
                    
                    if (-not $hasColumn) {
                        Write-Host "Corrigindo indentação em: $($file.Name) linha $($i + 2)"
                        $newLines = $newLines[0..($newLines.Count - 2)]  # Remove última linha
                        $newLines += "$indent    Column("
                        $newLines += "$indent        modifier = Modifier.fillMaxWidth(),"
                        $newLines += "$indent        verticalArrangement = Arrangement.spacedBy(8.dp)"
                        $newLines += "$indent    ) {"
                        $modified = $true
                    }
                }
            }
        }
        
        $i++
    }
    
    if ($modified) {
        # Salvar arquivo
        $newLines | Set-Content -Path $file.FullName -Encoding UTF8
        $totalCorrigidos++
        Write-Host "  ✓ Arquivo corrigido" -ForegroundColor Green
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
