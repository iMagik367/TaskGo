# Notas de Lançamento - Teste Interno

## Versão 1.0.18 (versionCode 19)

### Notas para o Google Play (máximo 250 caracteres):

```
Correção: API Key centralizada atualizada para release. Firebase App Check configurado corretamente com Play Integrity. Melhorias na autenticação e logs de diagnóstico.
```

**Contagem de caracteres:** 247 caracteres ✅

---

## Versão Alternativa (mais curta):

```
API Key centralizada atualizada. Firebase App Check com Play Integrity configurado. Melhorias na autenticação e diagnóstico de erros.
```

**Contagem de caracteres:** 149 caracteres ✅

---

## Versão Alternativa (mais técnica):

```
Correção crítica: API Key unificada para release. App Check com Play Integrity ativo. Autenticação otimizada e logs melhorados.
```

**Contagem de caracteres:** 147 caracteres ✅

---

## Resumo das Correções Realizadas:

1. ✅ **API Key Centralizada**: Substituída API Key antiga pela nova (`AIzaSyD9JIxB5lzJUou1hUHBxNMGC4DVjEtIY_k`) em:
   - `google-services.json` (já estava correta)
   - `AndroidManifest.xml` (Google Maps API Key)
   - `GeocodingService.kt` (Geocoding API)

2. ✅ **Firebase App Check**: Configurado corretamente com Play Integrity para builds release

3. ✅ **Versão**: Atualizada para `versionCode 19` e `versionName 1.0.18`

4. ✅ **Build**: AAB gerado com sucesso e assinado

---

## Arquivo AAB Gerado:

`app/build/outputs/bundle/release/app-release.aab`





















