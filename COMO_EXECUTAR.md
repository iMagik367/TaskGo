# 🚀 COMO EXECUTAR OS SCRIPTS

## 📋 Duas formas de executar:

### Opção 1: Usando arquivos .BAT (CMD/Windows Explorer)
✅ **Recomendado para CMD**

Clique duplo ou execute no CMD:
```
deploy-firebase-completo.bat
build-aab-release.bat
deploy-e-build-completo.bat
```

### Opção 2: Usando PowerShell diretamente
✅ **Recomendado para PowerShell**

No PowerShell:
```powershell
.\deploy-firebase-completo.ps1
.\build-aab-release.ps1
.\deploy-e-build-completo.ps1
```

Ou no CMD:
```cmd
powershell -ExecutionPolicy Bypass -File deploy-firebase-completo.ps1
powershell -ExecutionPolicy Bypass -File build-aab-release.ps1
powershell -ExecutionPolicy Bypass -File deploy-e-build-completo.ps1
```

---

## ⚠️ NOTA IMPORTANTE

**Se você executar `.ps1` diretamente no CMD sem usar `.bat` ou `powershell -File`, o Windows vai abrir o arquivo no Bloco de Notas!**

**Solução:** Use os arquivos `.bat` criados ou execute com `powershell -File`.

---

## 📝 Arquivos criados:

- ✅ `deploy-firebase-completo.bat` → Deploy Firebase
- ✅ `build-aab-release.bat` → Build AAB
- ✅ `deploy-e-build-completo.bat` → Deploy + Build

Todos os `.bat` chamam os scripts PowerShell corretamente!
