# Migração de Database: Default → Taskgo

## ✅ O QUE FOI FEITO

### 1. Cloud Functions (Backend)
- ✅ Criado helper `getFirestore()` em `functions/src/utils/firestore.ts`
- ✅ Atualizados **29 arquivos** para usar `getFirestore()` ao invés de `admin.firestore()`
- ✅ Todas as Cloud Functions agora gravam no database **'taskgo'**
- ✅ Criada função de migração `migrateDatabaseToTaskgo` para copiar dados de 'default' para 'taskgo'

### 2. Android App (Frontend)
- ✅ Atualizado `FirebaseModule.kt` para usar `FirebaseFirestore.getInstance(app, "taskgo")`
- ⚠️ **PENDENTE**: Atualizar outros 9 arquivos que usam `FirebaseFirestore.getInstance()` diretamente

## 📋 ARQUIVOS ANDROID QUE PRECISAM SER ATUALIZADOS

Os seguintes arquivos ainda usam `FirebaseFirestore.getInstance()` diretamente e precisam ser atualizados:

1. `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/SecuritySettingsScreen.kt`
2. `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/PrivacyScreen.kt`
3. `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/AccountScreen.kt`
4. `app/src/main/java/com/taskgoapp/taskgo/core/security/DocumentVerificationManager.kt`
5. `app/src/main/java/com/taskgoapp/taskgo/navigation/OrderChatNavigationScreen.kt`
6. `app/src/main/java/com/taskgoapp/taskgo/core/design/UserAvatarNameLoader.kt`
7. `app/src/main/java/com/taskgoapp/taskgo/feature/orders/presentation/ShipmentScreen.kt`
8. `app/src/main/java/com/taskgoapp/taskgo/feature/products/presentation/OrderTrackingViewModel.kt`
9. `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/ConsentHistoryScreen.kt`

**Solução**: Substituir `FirebaseFirestore.getInstance()` por:
```kotlin
FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "taskgo")
```

## 🚀 PRÓXIMOS PASSOS

### 1. Executar Migração de Dados
Após fazer deploy das Cloud Functions, execute a função de migração:

```typescript
// Chamar via Firebase Console ou via app
firebase.functions().httpsCallable('migrateDatabaseToTaskgo')()
```

### 2. Atualizar Arquivos Android Restantes
Atualizar os 9 arquivos listados acima para usar o database 'taskgo'.

### 3. Testar
- Verificar se novos dados são gravados em 'taskgo'
- Verificar se leituras funcionam corretamente
- Validar que não há mais dados sendo gravados em 'default'

### 4. Deletar Database 'default' (APÓS VALIDAÇÃO)
⚠️ **ATENÇÃO**: Só deletar o database 'default' após confirmar que:
- Todos os dados foram migrados
- O app está funcionando corretamente com 'taskgo'
- Não há mais gravações em 'default'

## 📝 NOTAS TÉCNICAS

- Firebase Admin SDK v12+ suporta múltiplos databases Firestore (feature Enterprise)
- O método `admin.app().firestore('taskgo')` pode não estar disponível em todas as versões
- Se houver erro ao acessar 'taskgo', o código fallback usa 'default' (com log de erro)
- A migração processa em batches de 500 documentos (limite do Firestore)

## ⚠️ IMPORTANTE

**NÃO DELETAR O DATABASE 'default' ANTES DE:**
1. Executar a migração completa
2. Validar que todos os dados foram copiados
3. Confirmar que o app está funcionando 100% com 'taskgo'
4. Verificar logs para garantir que não há mais gravações em 'default'
