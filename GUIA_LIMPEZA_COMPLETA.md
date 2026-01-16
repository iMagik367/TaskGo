# 🧹 Guia: Limpeza Completa de Dados

Este guia mostra como fazer uma limpeza completa de usuários e dados relacionados para começar testes do zero.

---

## ⚠️ ATENÇÃO

**Esta ação é IRREVERSÍVEL!** Todos os usuários e dados relacionados serão excluídos permanentemente.

---

## 🚀 COMO EXECUTAR A LIMPEZA

### 1. Executar Script de Limpeza

```powershell
cd functions
node scripts/cleanup-all-users.js
```

### 2. O que o Script Faz

1. **Lista todos os usuários** do Firebase Auth (em batches de 100)
2. **Exclui cada usuário** do Firebase Auth
3. **Remove documentos órfãos** na coleção `/users` do Firestore (se existirem)
4. **Gera relatório** do que foi excluído

---

## 📊 RESULTADO ESPERADO

```
⚠️  INICIANDO LIMPEZA COMPLETA DE USUÁRIOS
⚠️  Esta ação é IRREVERSÍVEL!

📋 Listando e excluindo usuários do Firebase Auth...

📦 Processando batch: X usuários
✓ Excluído: usuario1@email.com
✓ Excluído: usuario2@email.com
...

🔍 Verificando documentos órfãos no Firestore...

═══════════════════════════════════════
✅ Limpeza concluída!
   Usuários excluídos do Auth: X
   Documentos órfãos excluídos: Y
   Erros: 0
═══════════════════════════════════════

✅ Ambiente limpo e pronto para testes do zero!
   Novos usuários criarão Custom Claims automaticamente via onUserCreate
```

---

## ✅ APÓS A LIMPEZA

### 1. Novos Usuários Receberão Custom Claims Automaticamente

Quando novos usuários forem criados:
- A função `onUserCreate` será acionada automaticamente
- Custom Claim `role: 'user'` será definida automaticamente
- Documento será criado no Firestore com role inicial

### 2. Não Precisa Executar Migração

Como todos os usuários foram excluídos:
- ✅ Não há usuários antigos para migrar
- ✅ Novos usuários já terão Custom Claims desde o início
- ✅ Ambiente limpo para testes

---

## 🔄 OUTRAS LIMPEZAS (Opcional)

Se você também quiser limpar outras coleções do Firestore, pode fazer manualmente via Firebase Console:

### Coleções que podem ser limpas:
- `services` - Serviços criados por usuários
- `products` - Produtos criados por usuários
- `orders` - Pedidos de serviços/produtos
- `conversations` - Conversas entre usuários
- `notifications` - Notificações
- `reviews` - Avaliações

**⚠️ CUIDADO:** Limpar essas coleções pode afetar referências e integridade dos dados.

---

## 📝 COMANDO RÁPIDO

```powershell
# Limpeza completa em um comando
cd functions && node scripts/cleanup-all-users.js
```

---

## 🆘 TROUBLESHOOTING

### Erro: "Permission denied"
- Verificar se o service account tem permissões de admin
- Verificar se o arquivo de credenciais está correto

### Erro: "Quota exceeded"
- Aguardar alguns minutos e tentar novamente
- Firebase tem limites de rate para exclusões em massa

### Documentos órfãos não foram excluídos
- Pode ser que não existam documentos órfãos
- Ou que os documentos tenham IDs diferentes dos usuários excluídos
- Isso não afeta o funcionamento do sistema

---

**Última atualização:** 2024
