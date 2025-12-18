# INSTRUCOES PARA EXECUTAR O SCRIPT MANUALMENTE
#
# Como o script precisa de entrada interativa (senhas), voce precisa executar manualmente:
#
# OPCAO 1: Executar no PowerShell do Windows
#   1. Abra o PowerShell
#   2. Navegue ate a pasta do projeto:
#      cd C:\Users\user\AndroidStudioProjects\TaskGoApp
#   3. Execute o script:
#      .\criar-keystore-interativo.ps1
#   4. Siga as instrucoes na tela
#
# OPCAO 2: Executar comando keytool diretamente
#   Execute este comando no PowerShell (substitua as senhas e informacoes):
#
#   keytool -genkey -v -keystore "$env:USERPROFILE\AndroidKeystores\taskgo-release-key.jks" -keyalg RSA -keysize 2048 -validity 10000 -alias taskgo-release -storepass "SUA_SENHA_AQUI" -keypass "SUA_SENHA_AQUI" -dname "CN=TaskGo App, OU=TaskGo, O=TaskGo, L=Sao Paulo, ST=SP, C=BR"
#
#   Depois crie o arquivo keystore.properties manualmente com:
#   TASKGO_RELEASE_STORE_FILE=C:/Users/user/AndroidKeystores/taskgo-release-key.jks
#   TASKGO_RELEASE_KEY_ALIAS=taskgo-release
#   TASKGO_RELEASE_STORE_PASSWORD=SUA_SENHA_AQUI
#   TASKGO_RELEASE_KEY_PASSWORD=SUA_SENHA_AQUI

