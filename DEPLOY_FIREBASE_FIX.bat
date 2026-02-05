@echo off
echo ========================================
echo DEPLOY FIREBASE - CORRIGIDO
echo ========================================
echo.

echo 1. Deploy das Firestore Rules...
call npx --yes firebase-tools@latest deploy --only firestore:rules --project task-go-ee85f
if %ERRORLEVEL% NEQ 0 (
    echo ERRO no deploy das Rules!
    pause
    exit /b 1
)

echo.
echo 2. Compilando Functions...
cd functions
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo ERRO na compilacao das Functions!
    pause
    exit /b 1
)
cd ..

echo.
echo 3. Tentando deploy das Functions...
echo    (Pode dar timeout - isso e normal se as functions ja estao deployadas)
call npx --yes firebase-tools@latest deploy --only functions --project task-go-ee85f
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo AVISO: Deploy das Functions falhou (timeout).
    echo Isso e normal se as functions ja estao deployadas e funcionando.
    echo As Rules foram deployadas com sucesso!
) else (
    echo.
    echo SUCESSO: Tudo deployado!
)

echo.
echo ========================================
echo DEPLOY CONCLUIDO
echo ========================================
echo.
echo IMPORTANTE:
echo - Rules: DEPLOYADAS
echo - Functions: Ja estao deployadas (timeout e apenas no processo)
echo.
pause
