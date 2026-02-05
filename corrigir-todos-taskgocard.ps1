# Script para corrigir TODOS os TaskGoCard quebrados
# Segue o padrão: TaskGoCard { Column { conteúdo } }

Write-Host "========================================"
Write-Host "CORREÇÃO AUTOMÁTICA DE TASKGOCARD"
Write-Host "========================================"
Write-Host ""

$baseDir = "app\src\main\java\com\taskgoapp\taskgo"
$files = Get-ChildItem -Path $baseDir -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }

Write-Host "Processando $($files.Count) arquivos..."
Write-Host ""

$totalCorrigidos = 0

foreach ($file in $files) {
    $lines = Get-Content $file.FullName -Encoding UTF8
    $newLines = @()
    $i = 0
    $modified = $false
    
    while ($i -lt $lines.Count) {
        $line = $lines[$i]
        $indent = if ($line -match '^(\s*)') { $matches[1] } else { "" }
        
        # Padrão 1: TaskGoCard(...) { seguido de ) { (estrutura quebrada)
        if ($line -match '^\s*com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{?\s*$') {
            if ($i + 1 -lt $lines.Count) {
                $nextLine = $lines[$i + 1]
                
                # Caso: linha seguinte é ) { (estrutura quebrada)
                if ($nextLine -match '^\s+\)\s*\{') {
                    Write-Host "Corrigindo estrutura quebrada: $($file.Name) linha $($i + 2)"
                    $newLines += $line
                    $newLines += "$indent    Column("
                    $newLines += "$indent        modifier = Modifier.fillMaxWidth(),"
                    $newLines += "$indent        verticalArrangement = Arrangement.spacedBy(8.dp)"
                    $newLines += "$indent    ) {"
                    $i++  # Pula a linha ) {
                    $modified = $true
                    continue
                }
                
                # Caso: linha seguinte tem conteúdo direto (Text, Row, etc) sem Column
                if ($nextLine -match '^\s+(Text\s*\(|Row\s*\(|Spacer\s*\(|Button\s*\(|OutlinedButton\s*\(|Icon\s*\(|AsyncImage\s*\(|HorizontalDivider\s*\(|VerticalDivider\s*\(|Card\s*\(|if\s*\(|when\s*\()') {
                    # Verificar se há Column nas próximas 10 linhas
                    $hasColumn = $false
                    for ($j = $i + 1; $j -lt [Math]::Min($i + 11, $lines.Count); $j++) {
                        if ($lines[$j] -match '^\s+Column\s*\(') {
                            $hasColumn = $true
                            break
                        }
                        # Se encontrar chave fechando antes, parar
                        if ($lines[$j] -match '^\s+\}') {
                            break
                        }
                    }
                    
                    if (-not $hasColumn) {
                        Write-Host "Adicionando Column: $($file.Name) linha $($i + 2)"
                        $newLines += $line
                        $newLines += "$indent    Column("
                        $newLines += "$indent        modifier = Modifier.fillMaxWidth(),"
                        $newLines += "$indent        verticalArrangement = Arrangement.spacedBy(8.dp)"
                        $newLines += "$indent    ) {"
                        $modified = $true
                        $i++
                        continue
                    }
                }
            }
        }
        
        $newLines += $line
        $i++
    }
    
    if ($modified) {
        $newLines | Set-Content -Path $file.FullName -Encoding UTF8
        $totalCorrigidos++
        Write-Host "  ✓ Corrigido" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "CONCLUÍDO: $totalCorrigidos arquivos corrigidos"
Write-Host "========================================"
