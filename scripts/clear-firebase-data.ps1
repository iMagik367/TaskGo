# Script para limpar TODOS os dados do Firebase
# ATENCAO: Esta acao e IRREVERSIVEL!

$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host ""
Write-Host "========================================" -ForegroundColor Red
Write-Host "LIMPEZA COMPLETA DO FIREBASE" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""
Write-Host "ATENCAO: Este script vai deletar TODOS os dados!" -ForegroundColor Yellow
Write-Host "- Todos os usuarios do Authentication" -ForegroundColor Yellow
Write-Host "- Todas as colecoes do Firestore" -ForegroundColor Yellow
Write-Host "- Todos os arquivos do Storage" -ForegroundColor Yellow
Write-Host ""
Write-Host "Esta acao e IRREVERSIVEL!" -ForegroundColor Red
Write-Host ""

$confirm = Read-Host "Digite 'CONFIRMAR' para continuar"

if ($confirm -ne "CONFIRMAR") {
    Write-Host ""
    Write-Host "Operacao cancelada." -ForegroundColor Green
    exit
}

Write-Host ""
Write-Host "Executando limpeza..." -ForegroundColor Cyan
Write-Host ""

$url = "https://us-central1-task-go-ee85f.cloudfunctions.net/clearAllData?confirm=DELETE_ALL_DATA_CONFIRMED"

try {
    $response = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing
    
    $result = $response.Content | ConvertFrom-Json
    
    if ($result.success) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "LIMPEZA CONCLUIDA COM SUCESSO!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Resultados:" -ForegroundColor Cyan
        Write-Host "  Firestore:" -ForegroundColor White
        Write-Host "    - Colecoes: $($result.results.firestore.collections)" -ForegroundColor Gray
        Write-Host "    - Documentos: $($result.results.firestore.documents)" -ForegroundColor Gray
        Write-Host "    - Usuarios: $($result.results.firestore.users)" -ForegroundColor Gray
        Write-Host "    - Subcolecoes de usuarios: $($result.results.firestore.userSubcollections)" -ForegroundColor Gray
        Write-Host "  Storage:" -ForegroundColor White
        Write-Host "    - Arquivos: $($result.results.storage.files)" -ForegroundColor Gray
        Write-Host "  Authentication:" -ForegroundColor White
        Write-Host "    - Usuarios: $($result.results.auth.users)" -ForegroundColor Gray
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "Erro durante a limpeza:" -ForegroundColor Red
        Write-Host $result.message -ForegroundColor Red
        Write-Host ""
    }
} catch {
    Write-Host ""
    Write-Host "Erro ao executar limpeza:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    
    # Tentar exibir a resposta de erro se houver
    if ($_.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Resposta do servidor:" -ForegroundColor Yellow
            Write-Host $responseBody -ForegroundColor Yellow
        } catch {
            # Ignorar se não conseguir ler a resposta
        }
    }
}

Write-Host ""
Write-Host "Pressione qualquer tecla para continuar..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")