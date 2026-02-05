@echo off
echo Restaurando telas do projeto antigo...

set OLD_PATH=TaskGoApp\app\src\main\java
set NEW_PATH=app\src\main\java

echo Copiando telas principais...
xcopy /E /I /Y "%OLD_PATH%\com\taskgoapp\taskgo\feature" "%NEW_PATH%\com\taskgoapp\taskgo\feature"

echo Telas restauradas!
pause
