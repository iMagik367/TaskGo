# Script para atualizar todos os arquivos TypeScript para usar getFirestore() ao invés de admin.firestore()
# Isso garante que todos os dados sejam gravados no database 'taskgo' ao invés de 'default'

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Atualizando Firestore para usar 'taskgo'" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$functionsSrcPath = Join-Path $PSScriptRoot "src"
$filesUpdated = 0
$filesSkipped = 0

# Lista de arquivos para atualizar
$files = Get-ChildItem -Path $functionsSrcPath -Recurse -Filter "*.ts" | Where-Object {
    $content = Get-Content $_.FullName -Raw
    return $content -match "admin\.firestore\(\)"
}

Write-Host "Encontrados $($files.Count) arquivos para atualizar" -ForegroundColor Yellow
Write-Host ""

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Verificar se já tem o import de getFirestore
    $hasImport = $content -match "import.*getFirestore.*from.*firestore"
    
    # Adicionar import se não existir
    if (-not $hasImport) {
        # Encontrar a linha do último import de admin
        if ($content -match "(import \* as admin from ['\`"]firebase-admin['\`"];[\r\n]+)") {
            $content = $content -replace "(import \* as admin from ['\`"]firebase-admin['\`"];[\r\n]+)", "`$1import {getFirestore} from './utils/firestore';`r`n"
        } elseif ($content -match "(import \* as admin from ['\`"]firebase-admin['\`"];)") {
            $content = $content -replace "(import \* as admin from ['\`"]firebase-admin['\`"];)", "`$1`r`nimport {getFirestore} from './utils/firestore';"
        } else {
            # Tentar adicionar após qualquer import de admin
            if ($content -match "(import.*admin.*from.*firebase-admin[^\r\n]*[\r\n]+)") {
                $content = $content -replace "(import.*admin.*from.*firebase-admin[^\r\n]*[\r\n]+)", "`$1import {getFirestore} from './utils/firestore';`r`n"
            }
        }
        
        # Se ainda não adicionou, adicionar após o primeiro import
        if (-not ($content -match "import.*getFirestore")) {
            if ($content -match "^(import[^\r\n]*[\r\n]+)") {
                $content = $content -replace "^(import[^\r\n]*[\r\n]+)", "`$1import {getFirestore} from './utils/firestore';`r`n"
            }
        }
    }
    
    # Substituir admin.firestore() por getFirestore()
    # Padrões comuns:
    # - const db = admin.firestore();
    # - const db = admin.firestore()
    # - admin.firestore().collection(...)
    # - var db = admin.firestore();
    # - let db = admin.firestore();
    
    $content = $content -replace "const db = admin\.firestore\(\);", "const db = getFirestore();"
    $content = $content -replace "const db = admin\.firestore\(\)", "const db = getFirestore()"
    $content = $content -replace "var db = admin\.firestore\(\);", "var db = getFirestore();"
    $content = $content -replace "let db = admin\.firestore\(\);", "let db = getFirestore();"
    $content = $content -replace "admin\.firestore\(\)\.collection\(", "getFirestore().collection("
    
    # Substituir instâncias diretas de admin.firestore() que não estão em variáveis
    # Mas manter as que já estão corretas ou são para o database default (como na migração)
    if ($file.Name -ne "migrate-database.ts" -and $file.Name -ne "firestore.ts") {
        # Substituir padrões como: await admin.firestore().collection(...)
        $content = $content -replace "await admin\.firestore\(\)\.", "await getFirestore()."
        $content = $content -replace "admin\.firestore\(\)\.get\(\)", "getFirestore().get()"
    }
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "✅ Atualizado: $($file.Name)" -ForegroundColor Green
        $filesUpdated++
    } else {
        Write-Host "⏭️  Pulado: $($file.Name) (sem mudanças necessárias)" -ForegroundColor Gray
        $filesSkipped++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Arquivos atualizados: $filesUpdated" -ForegroundColor Green
Write-Host "Arquivos pulados: $filesSkipped" -ForegroundColor Gray
Write-Host ""
Write-Host "Próximo passo: Executar 'npm run build' para verificar se há erros" -ForegroundColor Yellow
