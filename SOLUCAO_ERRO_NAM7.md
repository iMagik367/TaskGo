# 🔧 Solução: Erro nam7 vs nam5

## 🔍 Problema Identificado

O erro mostra que a extensão tentou usar `nam7`, mas seu Firestore está em `nam5`:

```
Database '(default)' does not exist in region 'nam7'. 
Did you mean region 'nam5'?
```

## 📋 O que aconteceu?

- Você selecionou uma região no dropdown que mapeou para `nam7`
- Seu Firestore está configurado em `nam5` (multi-região)
- `nam7` e `nam5` são multi-regiões diferentes e não são compatíveis

## ✅ Solução: Selecionar Região Compatível com nam5

Para que funcione com `nam5`, você DEVE selecionar uma região que seja **parte de `nam5`**.

### Regiões que fazem parte de `nam5`:
- ✅ **Iowa (us-central1)** ← **USE ESTA!**
- ✅ **Oklahoma (us-central2)** 
- ✅ **South Carolina (us-east1)**

### Regiões que NÃO funcionam:
- ❌ Qualquer região que mapeie para `nam7`
- ❌ Regiões da América do Sul (southamerica-east1, etc.)
- ❌ Regiões da Europa (europe-west1, etc.)

## 📋 Passos Corretos

### 1. Desinstalar Extensão Atual

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/extensions
2. Encontre "Trigger Email from Firestore" (estado ERRORED)
3. Clique em "Desinstalar"

### 2. Reinstalar com Região CORRETA

1. No console, clique em **"Browse Extensions"**
2. Procure **"Trigger Email from Firestore"**
3. Clique em **"Install"**
4. Durante a instalação:

   **⚠️ CRÍTICO - Cloud Functions location:**
   - **SELECIONE**: **"Iowa (us-central1)"** ✅
   - **NÃO SELECIONE**: Qualquer outra região (pode mapear para nam7)
   
   **Firestore Instance Location:**
   - Se aparecer, selecione: **"Iowa (us-central1)"** ✅
   - OU deixe em branco/automático se não houver opção
   
   **Outros parâmetros:**
   - **Firestore Database**: `(default)`
   - **SMTP Connection URI**: Suas credenciais SMTP
   - **Default FROM address**: Seu email remetente
   - **Default REPLY-TO address**: Email para respostas

5. Complete a instalação

## 🔍 Por que "Iowa (us-central1)" funciona?

- `nam5` é uma multi-região que inclui `us-central1` (Iowa)
- Quando você seleciona "Iowa (us-central1)", o sistema reconhece que é compatível com `nam5`
- Cloud Functions em `us-central1` podem acessar Firestore em `nam5` sem problemas

## ⚠️ Importante

- **SEMPRE** selecione **"Iowa (us-central1)"** para Cloud Functions location
- **NÃO** selecione outras regiões dos EUA que possam mapear para `nam7`
- **NÃO** selecione regiões fora dos EUA (América do Sul, Europa, etc.)

## 📝 Checklist

- [ ] Desinstalar extensão antiga (estado ERRORED)
- [ ] Instalar extensão novamente
- [ ] **Cloud Functions location**: Selecionar **"Iowa (us-central1)"** ⚠️ CRÍTICO
- [ ] **Firestore Instance Location**: Selecionar "Iowa (us-central1)" (se disponível)
- [ ] Configurar credenciais SMTP
- [ ] Verificar instalação bem-sucedida
- [ ] Testar envio de email

## 🎯 Resumo

**O problema:** Extensão tentou usar `nam7`, mas Firestore está em `nam5`

**A solução:** Selecionar **"Iowa (us-central1)"** no dropdown de Cloud Functions location

**Resultado:** Extensão funcionando corretamente com Firestore em `nam5`

















