# 🔴 INSTRUÇÕES URGENTES - CORRIGIR ANDROID STUDIO

## ⚠️ PROBLEMA IDENTIFICADO

O Android Studio não está conseguindo ler o `local.properties` corretamente devido a cache corrompido do Gradle.

## ✅ SOLUÇÃO PASSO A PASSO (FAÇA EXATAMENTE NESTA ORDEM)

### PASSO 1: Fechar TUDO
1. **Feche o Android Studio COMPLETAMENTE**
2. Vá no **Gerenciador de Tarefas** (Ctrl+Shift+Esc)
3. Encerre TODOS os processos:
   - `studio64.exe`
   - `java.exe` (relacionados ao Android Studio)
   - `gradle-daemon`

### PASSO 2: Executar Script de Limpeza

Abra o PowerShell **como Administrador** na pasta do projeto e execute:

```powershell
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
powershell -ExecutionPolicy Bypass -File "limpar_tudo.ps1"
```

### PASSO 3: Configurar Variáveis de Ambiente (IMPORTANTE!)

1. Pressione **Win + R**, digite `sysdm.cpl` e pressione Enter
2. Vá na aba **Avançado**
3. Clique em **Variáveis de Ambiente**
4. Em **Variáveis do Sistema**, clique em **Novo**:
   - **Nome**: `ANDROID_HOME`
   - **Valor**: `C:\Users\user\AppData\Local\Android\Sdk`
5. Clique em **Novo** novamente:
   - **Nome**: `ANDROID_SDK_ROOT`
   - **Valor**: `C:\Users\user\AppData\Local\Android\Sdk`
6. Clique em **OK** em todas as janelas
7. **REINICIE O COMPUTADOR** (obrigatório!)

### PASSO 4: Abrir Android Studio

1. Após reiniciar, abra o Android Studio
2. **NÃO** abra o projeto ainda
3. Vá em **File → Settings** (ou **Ctrl+Alt+S**)
4. Navegue até **Appearance & Behavior → System Settings → Android SDK**
5. Verifique se o caminho está: `C:\Users\user\AppData\Local\Android\Sdk`
6. Se não estiver, clique em **Edit** e configure
7. Clique em **Apply** e depois **OK**

### PASSO 5: Abrir o Projeto

1. **File → Open**
2. Selecione a pasta `C:\Users\user\AndroidStudioProjects\TaskGoApp`
3. Aguarde o Android Studio indexar o projeto

### PASSO 6: Invalidar Cache

1. **File → Invalidate Caches / Restart...**
2. Selecione **Invalidate and Restart**
3. Aguarde o Android Studio reiniciar completamente

### PASSO 7: Sincronizar Gradle

1. **File → Sync Project with Gradle Files**
2. Aguarde a sincronização completar (pode demorar 5-10 minutos na primeira vez)
3. Se aparecer algum erro, me avise qual é

### PASSO 8: Build

1. **Build → Clean Project**
2. Aguarde completar
3. **Build → Rebuild Project**
4. Aguarde completar

## 🔍 SE AINDA NÃO FUNCIONAR

Verifique no Android Studio:

1. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
   - **Gradle JDK**: Deve estar usando JDK 17
   - **Use Gradle from**: Deve ser `gradle/wrapper/gradle-wrapper.properties`

2. **File → Settings → Appearance & Behavior → System Settings → Android SDK**
   - Verifique se o SDK Platform 34 está instalado
   - Se não estiver, instale

## 📝 NOTA IMPORTANTE

O build via terminal **ESTÁ FUNCIONANDO**, o que significa que:
- O código está correto ✅
- As configurações estão corretas ✅
- O problema é ESPECÍFICO do Android Studio ❌

A causa mais provável é:
- Cache corrompido do Gradle daemon
- Android Studio não lendo `local.properties` corretamente
- Variáveis de ambiente não configuradas

A solução com variáveis de ambiente + reiniciar o computador geralmente resolve 99% dos casos.

