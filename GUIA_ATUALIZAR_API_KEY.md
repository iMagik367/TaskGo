# 🔑 Guia para Atualizar API Key do Google Cloud

Sua nova API Key (Android): `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`

Outras chaves fornecidas pelo Firebase:
- **Browser API Key:** `AIzaSyBYiaQk5X35XJgz-4BsM4Zd7RIE7YyxxtM`
- **Gemini Developer API Key:** `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`

---

## ⚠️ IMPORTANTE

A chave API no arquivo `google-services.json` é **gerenciada automaticamente pelo Firebase**. O arquivo local foi atualizado, mas **você PRECISA atualizar no Firebase Console** para garantir que tudo funcione corretamente.

---

## 📋 ONDE A CHAVE FOI ENCONTRADA

A chave API está sendo usada em:
- ✅ `app/google-services.json` (atualizada localmente)
- ✅ Firebase Console (PRECISA atualizar)

Há duas instâncias no arquivo (para dois apps Android):
1. App: `com.example.taskgoapp`
2. App: `com.taskgo.taskgo`

---

## 🔥 COMO ATUALIZAR NO FIREBASE CONSOLE

### Opção 1: Atualizar via Firebase Console (Recomendado)

1. **Acesse o Firebase Console:**
   - Vá em https://console.firebase.google.com
   - Selecione o projeto: `task-go-ee85f`

2. **Vá em Project Settings:**
   - Clique no ícone de engrenagem (⚙️) no canto superior esquerdo
   - Selecione **Project settings**

3. **Vá na aba "Your apps":**
   - Role até a seção **Your apps**
   - Você verá os apps Android cadastrados

4. **Para cada app Android:**
   - Clique no app (`com.example.taskgoapp` ou `com.taskgo.taskgo`)
   - Role até a seção **API Keys**
   - Clique em **Add API Key** ou **Edit** na chave existente
   - Cole a nova chave Android: `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`
   - Se necessário, configure também a **Browser API Key**: `AIzaSyBYiaQk5X35XJgz-4BsM4Zd7RIE7YyxxtM`
   - Clique em **Save**

5. **Baixar novo google-services.json:**
   - Na mesma página, clique em **Download google-services.json**
   - Substitua o arquivo `app/google-services.json` pelo novo

### Opção 2: Atualizar via Google Cloud Console

1. **Acesse Google Cloud Console:**
   - Vá em https://console.cloud.google.com
   - Selecione o projeto: `task-go-ee85f`

2. **Vá em APIs & Services > Credentials:**
   - No menu lateral, vá em **APIs & Services** > **Credentials**

3. **Encontre a chave antiga:**
   - Procure pela chave: `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`
   - Clique nela para editar ou copie para criar uma nova, se necessário

4. **Atualizar a chave:**
   - Se for uma chave existente, você pode:
     - **Opção A:** Editar a chave existente e alterar as restrições/permissões
     - **Opção B:** Criar uma nova chave e atualizar no Firebase

5. **Se criar nova chave:**
   - Clique em **Create Credentials** > **API Key**
   - Cole a nova chave Android em um local seguro
   - Configure as restrições necessárias (veja abaixo)

---

## 🔒 CONFIGURAR RESTRIÇÕES DA API KEY

**IMPORTANTE:** Configure restrições para proteger sua chave!

1. **Acesse Google Cloud Console:**
   - https://console.cloud.google.com/apis/credentials
   - Selecione o projeto: `task-go-ee85f`

2. **Clique na chave API:**
   - Encontre a chave: `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`

3. **Configure Application restrictions:**
   - **Android apps:** Adicione os package names:
     - `com.example.taskgoapp`
     - `com.taskgo.taskgo`
     - `com.taskgoapp.taskgo`
   - Adicione os SHA-1 certificates dos seus apps (se necessário)

4. **Configure API restrictions:**
   - **Restrict key:** Selecione esta opção
   - Adicione apenas as APIs que você usa:
     - **Firebase Installations API**
     - **Firebase App Check API**
     - **Identity Toolkit API** (Firebase Auth)
     - **Cloud Firestore API**
     - **Cloud Storage API**
     - **Cloud Functions API**
     - **Firebase Cloud Messaging API** (se usar notificações)
     - **Gemini API** (se utilizar recursos do Gemini Developer)

---

## 📱 SE FOR UMA CHAVE PARA GOOGLE MAPS/PLACES

Se essa chave é específica para Google Maps ou Places API, você também precisa:

### 1. Adicionar ao AndroidManifest.xml

Se ainda não estiver configurado, adicione:

```xml
<application>
    <!-- ... outras configurações ... -->
    
    <!-- Google Maps API Key -->
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw" />
</application>
```

### 2. Adicionar BuildConfig (Se necessário)

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw\"")
    }
}
```

---

## ✅ VERIFICAÇÃO

Após atualizar:

1. **Verifique no Firebase Console:**
   - Project Settings > Your apps > API Keys
   - Confirme que a chave está correta

2. **Teste o app:**
   - Faça um build e teste
   - Verifique se os serviços do Firebase funcionam
   - Se usar Maps/Places, teste essas funcionalidades

3. **Verifique logs:**
   - Monitore os logs do Firebase
   - Verifique se há erros relacionados à API key

---

## 🆘 TROUBLESHOOTING

### Problema: "API key not valid"

**Soluções:**
1. Verifique se a chave foi copiada corretamente (sem espaços)
2. Verifique se a chave está habilitada no Google Cloud Console
3. Verifique se as APIs necessárias estão habilitadas
4. Verifique se as restrições de aplicativo estão corretas

### Problema: "API key has not been used"

**Solução:**
- Isso é normal se você acabou de criar a chave
- A mensagem desaparecerá após usar a chave

### Problema: "Quota exceeded"

**Solução:**
- Verifique os limites de quota no Google Cloud Console
- Considere habilitar billing para aumentar os limites

---

## 📝 CHECKLIST

- [ ] Chave atualizada no arquivo `google-services.json` local ✅ (já feito)
- [ ] Chave atualizada no Firebase Console
- [ ] Novo `google-services.json` baixado e substituído
- [ ] Restrições de aplicativo configuradas
- [ ] Restrições de API configuradas
- [ ] APIs necessárias habilitadas no Google Cloud
- [ ] App testado e funcionando
- [ ] Se usar Maps/Places: meta-data adicionado ao AndroidManifest.xml

---

**Última atualização:** 2024

