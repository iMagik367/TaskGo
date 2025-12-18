# Plano de Implementação Completa

## 📋 Tarefas a Implementar

### ✅ 1. Realtime Database - CONCLUÍDO
- [x] Criar regras do Realtime Database (`database.rules.json`)
- [x] Adicionar dependência no `build.gradle.kts`
- [x] Configurar no `FirebaseModule.kt`
- [x] Habilitar persistência offline

### ✅ 2. Cloud Function para Exclusão de Conta - CONCLUÍDO
- [x] Criar `deleteAccount.ts`
- [x] Exportar no `index.ts`
- [ ] Fazer deploy da função

### ⏳ 3. Correção de Salvamento
- [ ] Corrigir salvamento de produtos
- [ ] Corrigir salvamento de serviços
- [ ] Corrigir salvamento de ordens de serviço
- [ ] Garantir que dados são salvos no Firestore E Realtime Database

### ⏳ 4. Diferenciação Serviço vs Ordem de Serviço
- [ ] **Serviço** = O que o prestador oferece (ServiceFirestore)
  - Aparece na HomeScreen e aba Serviços
  - Criado em ServiceFormScreen
- [ ] **Ordem de Serviço** = Pedido de serviço do cliente (OrderFirestore)
  - Aparece na HomeScreen e aba Serviços
  - Criado em CreateWorkOrderScreen

### ⏳ 5. Tela de Perfil do Prestador/Loja
- [x] Criar `ProviderProfileScreen.kt`
- [x] Criar `ProviderProfileViewModel.kt`
- [ ] Adicionar navegação
- [ ] Integrar com botões de avaliação e mensagem
- [ ] Mostrar produtos para lojas

### ⏳ 6. Exibição na HomeScreen
- [ ] Adicionar serviços oferecidos na HomeScreen
- [ ] Adicionar ordens de serviço na HomeScreen
- [ ] Filtrar por localização

### ⏳ 7. Criptografia de Dados
- [ ] Configurar HTTPS/TLS (já configurado no Firebase)
- [ ] Verificar se todas as conexões usam HTTPS

---

## 🔧 Próximos Passos Imediatos

1. **Corrigir salvamento** - Verificar por que produtos/serviços não estão sendo salvos
2. **Adicionar serviços na HomeScreen** - Mostrar serviços oferecidos
3. **Adicionar navegação para perfil** - Conectar cards de prestadores à tela de perfil
4. **Fazer deploy das regras do Realtime Database**

