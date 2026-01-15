# 🔐 SHA-256 para Adicionar no Firebase

## ✅ SHA-256 do App Signing Key (OBRIGATÓRIO)

Este é o SHA-256 que você **DEVE ADICIONAR** no Firebase App Check:

```
8E:F5:30:BE:12:7D:76:54:BA:FF:EE:88:98:F6:EF:61:73:FA:D6:FF:C8:75:49:5C:C1:FE:B2:77:48:AD:3E:2F
```

---

## 📋 Onde Adicionar

### 1. Firebase App Check

**Link:** https://console.firebase.google.com/project/task-go-ee85f/appcheck

**Passos:**
1. Acesse o link acima
2. Selecione: **Task Go** (`com.taskgoapp.taskgo`)
3. Clique em: **Play Integrity**
4. Na seção **"Impressão digital do certificado SHA-256"**:
   - Clique em: **"Adicionar outra impressão digital"**
   - Cole: `8E:F5:30:BE:12:7D:76:54:BA:FF:EE:88:98:F6:EF:61:73:FA:D6:FF:C8:75:49:5C:C1:FE:B2:77:48:AD:3E:2F`
   - Clique em: **Salvar**

**Importante:** Não remova o SHA-256 atual (`95:AF:63:3A:8F:CD:20:49:...`). Adicione este novo além do existente.

---

### 2. Firebase Console - Android App

**Link:** https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo

**Passos:**
1. Acesse o link acima
2. Role até: **"SHA certificate fingerprints"**
3. Clique em: **"Add fingerprint"**
4. Cole: `8E:F5:30:BE:12:7D:76:54:BA:FF:EE:88:98:F6:EF:61:73:FA:D6:FF:C8:75:49:5C:C1:FE:B2:77:48:AD:3E:2F`
5. Clique em: **Save**

---

## 📊 Resumo das Chaves

| Tipo de Chave | SHA-256 | Status no Firebase |
|---------------|---------|-------------------|
| **Upload Key** | `95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18` | ✅ Já cadastrado |
| **App Signing Key** | `8E:F5:30:BE:12:7D:76:54:BA:FF:EE:88:98:F6:EF:61:73:FA:D6:FF:C8:75:49:5C:C1:FE:B2:77:48:AD:3E:2F` | ❌ **ADICIONAR AGORA** |

---

## ✅ Checklist

- [ ] Adicionar SHA-256 do App Signing Key no Firebase App Check
- [ ] Adicionar SHA-256 do App Signing Key no Firebase Console (Android App)
- [ ] Aguardar 5-10 minutos para propagação
- [ ] Testar o app (deve estar instalado via Play Store)

---

## 🎯 Resultado Esperado

Após adicionar o SHA-256 correto, os logs do app devem mostrar:

```
✅ App Check token obtido com sucesso (Play Integrity)
Token (primeiros 20 chars): ...
```

O erro **"App attestation failed (403)"** deve desaparecer.





















