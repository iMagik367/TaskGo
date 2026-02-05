# Script para corrigir TaskGoCard quebrados
Write-Host "Corrigindo TaskGoCard quebrados..."
Write-Host ""

$baseDir = "app\src\main\java\com\taskgoapp\taskgo"
$files = Get-ChildItem -Path $baseDir -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }

$totalCorrigidos = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $original = $content
    
    # Padrao 1: TaskGoCard(...) { seguido de ) {
    $content = $content -replace '(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)\s*\)\s*\{', '$1`n        Column(`n            modifier = Modifier.fillMaxWidth(),`n            verticalArrangement = Arrangement.spacedBy(8.dp)`n        ) {'
    
    # Padrao 2: TaskGoCard com conteudo direto (sem Column)
    # Procura por TaskGoCard seguido de Text/Row/etc sem Column nas proximas linhas
    if ($content -match 'com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{') {
        # Verificar se tem Column logo apos
        $afterMatch = $content.Substring($content.IndexOf($matches[0]) + $matches[0].Length)
        if ($afterMatch -notmatch '^\s*Column\s*\(') {
            # Nao tem Column, adicionar
            $content = $content -replace '(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\s*\([^)]*\)\s*\{)', '$1`n        Column(`n            modifier = Modifier.fillMaxWidth(),`n            verticalArrangement = Arrangement.spacedBy(8.dp)`n        ) {'
        }
    }
    
    if ($content -ne $original) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Corrigido: $($file.Name)"
        $totalCorrigidos++
    }
}

Write-Host ""
Write-Host "Total corrigido: $totalCorrigidos arquivos"
