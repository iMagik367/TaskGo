# 🔧 Correção: Região do Firestore para Extensão de Email

## 📋 Problema Identificado

O erro indica que o banco de dados Firestore `(default)` não existe na região `us-central1`, e sugere usar a região `nam5`.

**Erro completo:**
```
Database '(default)' does not exist in region 'us-central1'. 
Did you mean region 'nam5'?
```

## 🔍 Entendendo o Código de Região `nam5`

O código `nam5` é um identificador interno do Google Cloud. Na verdade, `nam5` **é** `us-central1`, mas pode haver uma diferença na forma como o Firestore foi criado.

## ✅ Solução: Usar `nam5` como Location

Quando instalar a extensão, use **`nam5`** como Location em vez de `us-central1`.

### Passos para Corrigir:

1. **Desinstalar a extensão atual** (se estiver em estado ERRORED):
   - Console: https://console.firebase.google.com/project/task-go-ee85f/extensions
   - Encontre "Trigger Email from Firestore"
   - Clique em "Desinstalar"

2. **Reinstalar com a região correta**:
   - Console: https://console.firebase.google.com/project/task-go-ee85f/extensions
   - Clique em "Browse Extensions"
   - Procure "Trigger Email from Firestore"
   - Clique em "Install"
   - **IMPORTANTE**: Quando perguntado sobre **"Location"** ou **"Firestore Instance Location"**, selecione: **"Iowa (us-central1)"** do dropdown
   - **NOTA**: `nam5` não aparece no dropdown, mas `us-central1` é compatível
   - Configure os outros parâmetros (SMTP, etc.)

### Via CLI (se disponível):

```bash
firebase ext:install firebase/firestore-send-email --project=task-go-ee85f
```

Durante a instalação interativa:
- **Location**: Selecione "Iowa (us-central1)" do dropdown
- **NOTA**: `nam5` não aparece, mas `us-central1` é compatível com `nam5`
- **Firestore Database**: `(default)`
- Configure SMTP e outros parâmetros

## 🔍 Verificar Região Real do Firestore

Para verificar a região real do seu Firestore:

### Opção 1: Via Console do Firebase
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore
2. Clique em "Configurações" (ícone de engrenagem)
3. Veja a "Localização" do banco de dados

### Opção 2: Via Google Cloud Console
1. Acesse: https://console.cloud.google.com/firestore/databases?project=task-go-ee85f
2. Veja a coluna "Location" para o banco `(default)`

### Opção 3: Via gcloud CLI (se instalado)
```bash
gcloud firestore databases list --project=task-go-ee85f
```

## 📝 Notas Importantes

1. **`nam5` vs `us-central1`**: 
   - `nam5` é o código de região interno
   - `us-central1` é o nome legível
   - Ambos referem-se à mesma região física, mas o Firestore pode estar configurado com o código `nam5`

2. **Região não pode ser alterada**:
   - Uma vez criado, o Firestore não pode mudar de região
   - Se a região estiver incorreta, você precisará usar o código correto na instalação da extensão

3. **Consistência de Região**:
   - A extensão DEVE usar a mesma região do Firestore
   - Cloud Functions criadas pela extensão também devem estar na mesma região

## 🛠️ Script de Verificação Atualizado

Execute o script atualizado que agora verifica e sugere `nam5`:

```powershell
.\corrigir-extensao-email-simples.ps1
```

## ✅ Checklist de Instalação

- [ ] Desinstalar extensão antiga (se existir)
- [ ] Verificar região do Firestore no console
- [ ] Instalar extensão usando Location: **"Iowa (us-central1)"** do dropdown
- [ ] Configurar credenciais SMTP
- [ ] Verificar se a instalação foi bem-sucedida
- [ ] Testar envio de email

## 🔗 Links Úteis

- Console Firebase: https://console.firebase.google.com/project/task-go-ee85f/extensions
- Firestore Databases: https://console.cloud.google.com/firestore/databases?project=task-go-ee85f
- Documentação Firestore Locations: https://firebase.google.com/docs/firestore/locations

















