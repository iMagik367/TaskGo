package com.taskgoapp.taskgo.core.design

import androidx.compose.ui.unit.dp

/**
 * Constantes de Design Padronizadas
 * 
 * Este arquivo centraliza todas as constantes de design para garantir
 * consistência visual em todo o aplicativo.
 * 
 * LEI DO DESIGN: Todas as telas DEVEM usar estas constantes.
 * NUNCA usar valores hardcoded de espaçamento, tamanho ou cor.
 */

object DesignConstants {
    
    // ========== ESPAÇAMENTOS ==========
    object Spacing {
        // Espaçamentos horizontais/verticais padrão
        val xs = 4.dp      // Espaçamento extra pequeno
        val sm = 8.dp      // Espaçamento pequeno
        val md = 16.dp     // Espaçamento médio (padrão)
        val lg = 24.dp     // Espaçamento grande
        val xl = 32.dp     // Espaçamento extra grande
        val xxl = 48.dp    // Espaçamento extra extra grande
        
        // Espaçamentos específicos
        val screenPadding = 16.dp      // Padding padrão das telas
        val cardPadding = 16.dp        // Padding interno dos cards
        val cardSpacing = 16.dp        // Espaçamento entre cards
        val sectionSpacing = 24.dp     // Espaçamento entre seções
        val itemSpacing = 8.dp          // Espaçamento entre itens em listas
        val buttonSpacing = 12.dp       // Espaçamento entre botões
        val formFieldSpacing = 16.dp    // Espaçamento entre campos de formulário
        val topBarHeight = 56.dp        // Altura padrão da top bar
        val bottomBarHeight = 64.dp     // Altura padrão da bottom bar
    }
    
    // ========== TAMANHOS DE ELEMENTOS ==========
    object Sizes {
        // Botões
        val buttonHeight = 52.dp        // Altura padrão dos botões
        val buttonHeightLarge = 56.dp   // Altura de botões grandes
        val buttonHeightSmall = 40.dp   // Altura de botões pequenos
        val buttonMinWidth = 120.dp     // Largura mínima dos botões
        
        // Ícones
        val iconSmall = 16.dp           // Ícones pequenos
        val iconMedium = 24.dp           // Ícones médios (padrão)
        val iconLarge = 32.dp            // Ícones grandes
        val iconXLarge = 48.dp           // Ícones extra grandes
        
        // Avatares
        val avatarSmall = 32.dp          // Avatar pequeno
        val avatarMedium = 48.dp         // Avatar médio
        val avatarLarge = 64.dp          // Avatar grande
        val avatarXLarge = 96.dp         // Avatar extra grande
        
        // Cards
        val cardElevation = 2.dp         // Elevação padrão dos cards
        val cardElevationHover = 4.dp    // Elevação ao passar o mouse
        val cardCornerRadius = 12.dp     // Raio de borda dos cards
        
        // Inputs
        val inputHeight = 56.dp          // Altura padrão dos inputs
        val inputCornerRadius = 8.dp     // Raio de borda dos inputs
        val inputPadding = 16.dp         // Padding interno dos inputs
        
        // Chips
        val chipHeight = 32.dp           // Altura padrão dos chips
        val chipPadding = 12.dp          // Padding interno dos chips
        
        // Dividers
        val dividerThickness = 1.dp      // Espessura dos dividers
    }
    
    // ========== TIPOGRAFIA ==========
    object Typography {
        // Tamanhos de fonte (usar estilos do MaterialTheme quando possível)
        val fontSizeSmall = 12.dp
        val fontSizeMedium = 14.dp
        val fontSizeLarge = 16.dp
        val fontSizeXLarge = 18.dp
        val fontSizeXXLarge = 20.dp
        val fontSizeTitle = 24.dp
    }
    
    // ========== BORDAS E FORMAS ==========
    object Shapes {
        val cornerRadiusSmall = 4.dp
        val cornerRadiusMedium = 8.dp
        val cornerRadiusLarge = 12.dp
        val cornerRadiusXLarge = 16.dp
        val cornerRadiusRound = 50.dp    // Para elementos circulares
    }
    
    // ========== ANIMAÇÕES ==========
    object Animation {
        val durationShort = 200          // Duração curta em ms
        val durationMedium = 300         // Duração média em ms
        val durationLong = 500           // Duração longa em ms
    }
    
    // ========== LIMITES ==========
    object Limits {
        val maxTextLength = 500          // Comprimento máximo de texto
        val maxImageCount = 5             // Número máximo de imagens
        val maxFileSizeMB = 10            // Tamanho máximo de arquivo em MB
    }
}
