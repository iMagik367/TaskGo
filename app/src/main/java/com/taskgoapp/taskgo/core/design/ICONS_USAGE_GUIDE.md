# 🎯 Guia de Uso dos Ícones WebP/PNG - TaskGoApp

> **Nota**: Os ícones estão sendo migrados de PNG para WebP para melhor qualidade e menor tamanho de arquivo. O código funciona igual para ambos os formatos.

## 📱 **Ícones Disponíveis**

### **1. Ícones de Navegação**
- `TGIcons.Home` - Ícone da tela inicial
- `TGIcons.Services` - Ícone de serviços
- `TGIcons.Products` - Ícone de produtos
- `TGIcons.Messages` - Ícone de mensagens
- `TGIcons.Profile` - Ícone de perfil

### **2. Ícones de Ações**
- `TGIcons.Search` - Ícone de busca
- `TGIcons.Cart` - Ícone do carrinho
- `TGIcons.Add` - Ícone de adicionar
- `TGIcons.Edit` - Ícone de editar
- `TGIcons.Delete` - Ícone de deletar
- `TGIcons.Check` - Ícone de confirmação
- `TGIcons.Back` - Ícone de voltar
- `TGIcons.Close` - Ícone de fechar

### **3. Ícones de Sistema**
- `TGIcons.Settings` - Ícone de configurações
- `TGIcons.Bell` - Ícone de notificações
- `TGIcons.Update` - Ícone de atualização
- `TGIcons.Help` - Ícone de ajuda
- `TGIcons.Support` - Ícone de suporte

### **4. Ícones de Pagamento**
- `TGIcons.Pix` - Ícone PIX
- `TGIcons.CreditCard` - Ícone de cartão de crédito
- `TGIcons.DebitCard` - Ícone de cartão de débito

### **5. Logos**
- `TGIcons.TaskGoLogoHorizontal` - Logo horizontal
- `TGIcons.TaskGoLogoVertical` - Logo vertical

## 🚀 **Como Usar os Ícones**

### **1. Em Composables (Recomendado)**
```kotlin
import com.taskgoapp.taskgo.core.design.TGIcons

@Composable
fun MyScreen() {
    Icon(
        painter = painterResource(TGIcons.Home),
        contentDescription = "Tela inicial",
        tint = MaterialTheme.colorScheme.primary
    )
}
```

### **2. Em Botões**
```kotlin
Button(
    onClick = { /* ação */ }
) {
    Icon(
        painter = painterResource(TGIcons.Add),
        contentDescription = "Adicionar"
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text("Adicionar Item")
}
```

### **3. Em TopBars**
```kotlin
AppTopBar(
    title = "Título",
    actions = {
        IconButton(onClick = { /* ação */ }) {
            Icon(
                painter = painterResource(TGIcons.Search),
                contentDescription = "Buscar"
            )
        }
    }
)
```

### **4. Em Cards**
```kotlin
Card(
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(TGIcons.Products),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text("Nome do Produto")
    }
}
```

## 🎨 **Personalização dos Ícones**

### **1. Tamanho**
```kotlin
Icon(
    painter = painterResource(TGIcons.Home),
    contentDescription = null,
    modifier = Modifier.size(24.dp) // Tamanho personalizado
)
```

### **2. Cor**
```kotlin
Icon(
    painter = painterResource(TGIcons.Home),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.primary // Cor personalizada
)
```

### **3. Combinação de Modificadores**
```kotlin
Icon(
    painter = painterResource(TGIcons.Home),
    contentDescription = null,
    modifier = Modifier
        .size(32.dp)
        .padding(8.dp),
    tint = Color.Red
)
```

## 📱 **Exemplos Práticos**

### **1. Bottom Navigation**
```kotlin
NavigationBarItem(
    selected = currentRoute == "home",
    onClick = { onTabSelected("home") },
    icon = {
        Icon(
            painter = painterResource(TGIcons.Home),
            contentDescription = "Início"
        )
    },
    label = { Text("Início") }
)
```

### **2. Lista com Ícones**
```kotlin
LazyColumn {
    items(items) { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    when (item.type) {
                        "product" -> TGIcons.Products
                        "service" -> TGIcons.Services
                        else -> TGIcons.Info
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(item.name)
        }
    }
}
```

### **3. Botões de Ação Flutuante**
```kotlin
FloatingActionButton(
    onClick = { /* ação */ }
) {
    Icon(
        painter = painterResource(TGIcons.Add),
        contentDescription = "Adicionar"
    )
}
```

## ⚠️ **Importante**

1. **Sempre use `painterResource()`** para ícones WebP/PNG
2. **Nunca use `vectorResource()`** para recursos WebP/PNG
3. **Use `TGIcons`** para referenciar os ícones
4. **Sempre forneça `contentDescription`** para acessibilidade
5. **Use `MaterialTheme.colorScheme`** para cores consistentes
6. **WebP é suportado nativamente** - não precisa mudar o código

## 🔧 **Solução de Problemas**

### **Erro: "Resource not found"**
- Verifique se o ícone existe na pasta `drawable-xxxhdpi` (WebP) ou `drawable-mdpi` (PNG antigo)
- Confirme se o nome está correto em `TGIcons`
- Certifique-se de que está usando `painterResource()`
- WebP e PNG são detectados automaticamente pelo Android

### **Ícone não aparece**
- Verifique se o `contentDescription` está definido
- Confirme se o `tint` não está transparente
- Verifique se o `modifier.size()` está definido

### **Ícone muito pequeno/grande**
- Ajuste o `modifier.size()` conforme necessário
- Use valores como `16.dp`, `24.dp`, `32.dp`, `48.dp`

## 📚 **Recursos Adicionais**

- [Material Design Icons](https://material.io/design/iconography/system-icons.html)
- [Jetpack Compose Icons](https://developer.android.com/jetpack/compose/graphics/images)
- [Android Resource Types](https://developer.android.com/guide/topics/resources/providing-resources)
- [Guia de Conversão WebP](../GUIA_CONVERSAO_WEBP.md) - Veja como converter PNG para WebP
