# 🤖 Guia: Criar Script Robo para Relatório de Pré-Lançamento

## 📋 O que é um Script Robo?

O Script Robo é um arquivo JSON que contém uma sequência de ações automatizadas para testar seu app. Ele guia o relatório de pré-lançamento do Google Play a testar fluxos específicos do seu aplicativo.

## 🎯 Método Recomendado: Gravar no Android Studio

A melhor forma de criar um script Robo é gravá-lo diretamente no Android Studio. Isso garante que as ações sejam precisas e funcionem com seu app.

### Passo 1: Abrir a Ferramenta de Gravação

1. Abra o **Android Studio**
2. Vá em **Tools** > **Firebase** > **Test Lab** > **Record Robo Script and Use it to Guide your Robo Test**
3. Se não aparecer essa opção, certifique-se de que o plugin do Firebase está instalado

### Passo 2: Selecionar Dispositivo

1. Escolha um **emulador** ou **dispositivo físico** conectado
2. Certifique-se de que o app está instalado e pode ser executado

### Passo 3: Gravar as Ações

1. **Inicie a gravação** quando solicitado
2. **Navegue pelo app** executando os fluxos principais:
   - Tela de Splash
   - Tela de Login (não precisa fazer login real, apenas explorar)
   - Tela de Cadastro
   - Tela Home
   - Navegação entre telas (Serviços, Produtos, Perfil)
   - Configurações
   - Política de Privacidade
   - Termos de Uso

3. **Dica:** Não precisa fazer login real. Apenas explore as telas para que o script saiba navegar pelo app.

### Passo 4: Salvar o Script

1. Após gravar, **pare a gravação**
2. O Android Studio salvará um arquivo JSON
3. **Salve o arquivo** em um local seguro (ex: `robo_script.json` na raiz do projeto)

## 📤 Upload no Google Play Console

### Passo 1: Acessar Configurações

1. Acesse o [Google Play Console](https://play.google.com/console)
2. Selecione seu app **TaskGo**
3. Vá em **Testes** > **Relatório de pré-lançamento**
4. Clique em **Configurações do relatório de pré-lançamento**

### Passo 2: Fazer Upload do Script

1. Na seção **"Uso de script Robo"**
2. Clique em **"Enviar"** ou arraste o arquivo `robo_script.json`
3. Aguarde o upload ser concluído

### Passo 3: Verificar

1. O script aparecerá na lista de scripts configurados
2. O Google Play usará este script nos próximos relatórios de pré-lançamento

## 📝 Script Robo Manual (Alternativa)

Se não conseguir gravar no Android Studio, você pode usar o arquivo `robo_script.json` que foi criado na raiz do projeto. Este script contém ações básicas para:

- Navegar pela tela de login
- Explorar telas principais
- Acessar configurações e políticas

**⚠️ Nota:** Como o app usa Jetpack Compose, o script manual pode ter limitações. A gravação no Android Studio é mais precisa.

## 🔍 Estrutura do Script Robo

O script Robo é um JSON com a seguinte estrutura:

```json
[
  {
    "crawlStage": "crawl",
    "contextDescriptor": {
      "condition": "app_under_test_shown"
    },
    "actions": [
      {
        "eventType": "VIEW_CLICKED",
        "delayTime": 1000,
        "elementDescriptors": [
          {
            "text": "Texto visível na tela"
          }
        ]
      }
    ]
  }
]
```

### Tipos de Eventos

- `VIEW_CLICKED`: Clicar em um elemento
- `VIEW_TEXT_CHANGED`: Digitar texto em um campo
- `VIEW_SCROLLED`: Rolar uma lista ou tela
- `VIEW_DISPLAYED`: Aguardar uma tela aparecer

### Element Descriptors

Para apps Compose, use principalmente:
- `text`: Texto visível na tela
- `hint`: Texto de dica em campos de texto
- `className`: Classe do componente (menos confiável em Compose)

## ⚠️ Limitações com Jetpack Compose

- Scripts Robo podem ter dificuldades com apps Compose
- A gravação no Android Studio geralmente funciona melhor
- Se o script não funcionar, o Google Play ainda executará testes automáticos básicos

## ✅ Checklist

- [ ] Plugin Firebase instalado no Android Studio
- [ ] App compilado e instalado em emulador/dispositivo
- [ ] Script Robo gravado ou arquivo `robo_script.json` criado
- [ ] Script enviado para Google Play Console
- [ ] Configuração verificada na Play Console

## 📚 Recursos

- [Documentação Firebase Test Lab - Robo Scripts](https://firebase.google.com/docs/test-lab/android/robo-scripts-reference)
- [Google Play Console - Relatórios de Pré-Lançamento](https://support.google.com/googleplay/android-developer/answer/7002270)

## 🎉 Pronto!

Após fazer o upload do script Robo, o Google Play usará essas ações para testar seu app automaticamente em cada relatório de pré-lançamento!






