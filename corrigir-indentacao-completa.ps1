# Script para corrigir TODOS os problemas de indentação de TaskGoCard

$files = Get-ChildItem -Path "app\src\main\java\com\taskgoapp\taskgo\feature" -Recurse -Filter "*.kt" | Where-Object { 
    $_.FullName -notmatch "Components\.kt" 
}

$count = 0
foreach ($file in $files) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        if ($null -eq $content) { continue }
        
        $original = $content
        $modified = $false
        
        # Padrão 1: TaskGoCard( seguido de linha sem indentação adequada
        # Corrigir: com.taskgoapp.taskgo.core.design.TaskGoCard(\n        modifier = Modifier.fillMaxWidth()\n    ) {
        $pattern1 = '(?m)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\()\s*\r?\n\s*modifier\s*=\s*Modifier\.fillMaxWidth\(\)\s*\r?\n\s*\)\s*\{'
        $replacement1 = "`$1`n                modifier = Modifier.fillMaxWidth()`n            ) {"
        
        if ($content -match $pattern1) {
            $content = $content -replace $pattern1, $replacement1
            $modified = $true
        }
        
        # Padrão 2: TaskGoCard com indentação incorreta (sem espaços adequados)
        $pattern2 = '(?m)(com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\()\s*\r?\n\s{0,7}modifier\s*=\s*Modifier\.fillMaxWidth\(\)\s*\r?\n\s{0,7}\)\s*\{'
        if ($content -match $pattern2 -and $content -notmatch 'com\.taskgoapp\.taskgo\.core\.design\.TaskGoCard\(\s*\r?\n\s{15,}modifier') {
            $content = $content -replace $pattern2, $replacement1
            $modified = $true
        }
        
        if ($modified) {
            Set-Content -Path $file.FullName -Value $content -NoNewline -Encoding UTF8
            $count++
            Write-Host "Corrigido: $($file.Name)"
        }
    } catch {
        Write-Host "Erro ao processar $($file.Name): $_"
    }
}

Write-Host "`nTotal de arquivos corrigidos: $count"
