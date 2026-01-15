# ✅ Solução Final: Correção da Extensão Trigger Email from Firestore

## 🔍 Problema Identificado

O erro indica que o Firestore está configurado como **multi-região `nam5`**, não como região única `us-central1`.

**Erro:**
```
Database '(default)' does not exist in region 'us-central1'. 
Did you mean region 'nam5'?
```

## 📋 O que é `nam5`?

`nam5` é uma **localização multi-região** do Google Cloud que inclui:
- `us-central1` (Iowa) - Read-Write
- `us-central2` (Oklahoma) - Read-Write  
- `us-east1` (South Carolina) - Witness

Seu Firestore foi criado como multi-região para alta disponibilidade.

## ✅ Solução: Usar `nam5` como Location

### Passo 1: Desinstalar Extensão Atual

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/extensions
2. Encontre "Trigger Email from Firestore" (estado ERRORED)
3. Clique nos três pontos (⋮) ou no botão de ação
4. Selecione "Desinstalar" ou "Uninstall"
5. Confirme a desinstalação

### Passo 2: Reinstalar com Região Correta

1. No mesmo console, clique em **"Browse Extensions"** ou **"Navegar por extensões"**
2. Procure por **"Trigger Email from Firestore"**
3. Clique em **"Install"** ou **"Instalar"**
4. Durante a instalação, configure:
   - **Cloud Functions location**: Selecione **"Iowa (us-central1)"** do dropdown ⚠️ **IMPORTANTE!**
   - **Firestore Instance Location**: Selecione **"Iowa (us-central1)"** do dropdown (se disponível)
   - **Firestore Database**: `(default)`
   - **SMTP Connection URI**: Suas credenciais SMTP
   - **Default FROM address**: Seu email remetente
   - **Default REPLY-TO address**: Email para respostas
   - Outros parâmetros conforme necessário
5. Complete a instalação

### Via CLI (Alternativa)

```bash
firebase ext:install firebase/firestore-send-email --project=task-go-ee85f
```

Durante a instalação interativa:
- Quando perguntado sobre **Location**, selecione: **"Iowa (us-central1)"** do dropdown
- **NOTA**: `nam5` não aparece no dropdown, mas `us-central1` é compatível
- Configure os outros parâmetros

## 🔍 Verificar Instalação

Após a instalação:

```bash
# Verificar status
firebase ext:list --project=task-go-ee85f

# Verificar Cloud Functions criadas
# (as funções devem estar na região nam5)
```

## ⚠️ Pontos Importantes

1. **Use `nam5`, não `us-central1`**: 
   - Seu Firestore está configurado como multi-região `nam5`
   - A extensão DEVE usar a mesma região

2. **Região não pode ser alterada**:
   - Uma vez criado, o Firestore não pode mudar de região
   - Use sempre `nam5` para este projeto

3. **Consistência**:
   - Firestore: `nam5`
   - Extensão Location: `nam5`
   - Cloud Functions: `nam5` (criadas automaticamente pela extensão)

## 📝 Checklist

- [ ] Desinstalar extensão antiga (estado ERRORED)
- [ ] Instalar extensão usando Location: **"Iowa (us-central1)"** do dropdown
- [ ] Configurar credenciais SMTP
- [ ] Verificar instalação bem-sucedida
- [ ] Testar envio de email

## 🔗 Links Úteis

- Console Extensions: https://console.firebase.google.com/project/task-go-ee85f/extensions
- Firestore Databases: https://console.cloud.google.com/firestore/databases?project=task-go-ee85f
- Documentação: https://firebase.google.com/docs/firestore/locations

## 🎯 Resumo Rápido

**O problema:** Extensão tentando usar região incorreta, Firestore está em multi-região `nam5`

**A solução:** Reinstalar a extensão selecionando **"Iowa (us-central1)"** do dropdown (compatível com `nam5`)

**Resultado:** Extensão funcionando corretamente na região compatível com o Firestore

















