# ✅ Solução Correta: Região para Extensão de Email

## 🔍 Problema Identificado

O erro sugere usar `nam5`, mas esse código **não aparece nos dropdowns** do console. Isso acontece porque:

- `nam5` é um código interno de **multi-região**
- Nos dropdowns, você vê as regiões legíveis como `us-central1` (Iowa)
- `us-central1` **é compatível** com `nam5` (faz parte da multi-região)

## ✅ Solução: Usar `us-central1` (Iowa)

Quando o dropdown pedir a região, selecione:

**"Iowa (us-central1)"** ou **"us-central1"**

Isso funcionará porque:
- `us-central1` faz parte da multi-região `nam5`
- O Firestore multi-região `nam5` aceita `us-central1` como região compatível
- É a opção visível e correta nos dropdowns

## 📋 Passos Corretos para Instalação

### 1. Desinstalar Extensão Atual (se necessário)

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/extensions
2. Encontre "Trigger Email from Firestore" (estado ERRORED)
3. Clique em "Desinstalar"

### 2. Reinstalar com Região Correta

1. No console, clique em **"Browse Extensions"**
2. Procure **"Trigger Email from Firestore"**
3. Clique em **"Install"**
4. Durante a instalação:

   **⚠️ CRÍTICO - Para "Cloud Functions location":**
   - **SELECIONE**: **"Iowa (us-central1)"** ✅
   - **NÃO SELECIONE**: Outras regiões (podem mapear para nam7 e causar erro)
   
   **Para "Firestore Instance Location":**
   - Selecione: **"Iowa (us-central1)"** ✅ (se disponível)
   - OU deixe em branco/automático se não houver opção
   
   **Outros parâmetros:**
   - **Firestore Database**: `(default)`
   - **SMTP Connection URI**: Suas credenciais SMTP
   - **Default FROM address**: Seu email remetente
   - **Default REPLY-TO address**: Email para respostas

5. Complete a instalação

## 🔍 Por que isso funciona?

- Seu Firestore está em multi-região `nam5`
- `nam5` inclui `us-central1` (Iowa) como uma das regiões
- Cloud Functions e extensões podem usar `us-central1` mesmo quando o Firestore está em `nam5`
- O sistema reconhece a compatibilidade automaticamente

## ⚠️ Importante

- **NÃO** tente digitar `nam5` manualmente (não funcionará)
- **USE** `us-central1` ou "Iowa (us-central1)" do dropdown
- **NÃO** selecione outras regiões que possam mapear para `nam7` (causará erro)
- Isso é a forma correta de referenciar a região compatível com `nam5`

## 📝 Checklist

- [ ] Desinstalar extensão antiga (se existir)
- [ ] Instalar extensão
- [ ] **Cloud Functions location**: Selecionar "Iowa (us-central1)"
- [ ] **Firestore Instance Location**: Selecionar "Iowa (us-central1)" (se disponível)
- [ ] Configurar credenciais SMTP
- [ ] Verificar instalação bem-sucedida
- [ ] Testar envio de email

## 🎯 Resumo

**O problema:** `nam5` não aparece no dropdown

**A solução:** Use **"Iowa (us-central1)"** do dropdown - é compatível com `nam5`

**Resultado:** Extensão funcionando corretamente

















