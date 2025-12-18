# Correção: Erros de Firebase APIs e App Check

## Problemas Identificados nos Logs

Os logs mostram dois problemas principais que estão impedindo o login:

### 1. Firebase App Check API Não Habilitada
**Erro:**
```
Firebase App Check API has not been used in project 605187481719 before or it is disabled.
```

**Solução:**
1. Acesse: https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719
2. Clique em **"ENABLE"** para habilitar a API
3. Aguarde alguns minutos para a propagação

### 2. Firebase Installations API Não Habilitada
**Erro:**
```
Firebase Installations API has not been used in project 605187481719 before or it is disabled.
```

**Solução:**
1. Acesse: https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719
2. Clique em **"ENABLE"** para habilitar a API
3. Aguarde alguns minutos para a propagação

### 3. Token de Debug do App Check
**Token de debug recomendado:**
```
4D4F1322-E272-454F-9396-ED80E3DBDBD7
```

**Solução:**
1. Acesse o Firebase Console: https://console.firebase.google.com/
2. Selecione o projeto **task-go-ee85f**
3. Vá em **App Check** no menu lateral
4. Clique em **"Manage debug tokens"**
5. Adicione o token: `4D4F1322-E272-454F-9396-ED80E3DBDBD7`
6. Clique em **"Add"**

## Passos Rápidos para Resolver

### Passo 1: Habilitar APIs no Google Cloud Console

Abra estas URLs no navegador (substitua `605187481719` pelo ID do seu projeto se diferente):

1. **Firebase App Check API:**
   https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719

2. **Firebase Installations API:**
   https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719

Clique em **"ENABLE"** em ambas as páginas.

### Passo 2: Configurar Debug Token no Firebase Console

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/appcheck
2. Clique em **"Apps"** e selecione seu app Android
3. Clique em **"Manage debug tokens"**
4. Adicione o token: `4D4F1322-E272-454F-9396-ED80E3DBDBD7`
5. Salve

### Passo 3: Aguardar Propagação

Após habilitar as APIs, aguarde **5-10 minutos** para que as mudanças sejam propagadas nos sistemas do Google.

### Passo 4: Testar Novamente

1. Feche completamente o app
2. Abra o app novamente
3. Tente fazer login

## Alternativa: Desabilitar App Check Temporariamente (Apenas para Debug)

Se você não conseguir habilitar as APIs imediatamente, pode desabilitar temporariamente o App Check para desenvolvimento:

**ATENÇÃO:** Isso deve ser usado APENAS para desenvolvimento e testes. Nunca faça isso em produção!

O código já está configurado para usar `DebugAppCheckProviderFactory` em builds de debug, mas ainda precisa das APIs habilitadas no Google Cloud Console.

## Verificação

Após habilitar as APIs, você deve ver nos logs:
- ✅ Sem erros de "API has not been used"
- ✅ Token de App Check obtido com sucesso
- ✅ Login funcionando corretamente

## Notas Importantes

1. **Projeto ID:** Certifique-se de que o projeto ID `605187481719` corresponde ao seu projeto Firebase
2. **Permissões:** Você precisa ter permissões de administrador no projeto para habilitar APIs
3. **Cobrança:** Habilitar essas APIs não gera custos adicionais, mas verifique se seu projeto tem billing habilitado se necessário

