package com.taskgoapp.taskgo.core.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.R

object TGIcons {
    // Mapeamento usando os ícones PNG criados
    @DrawableRes val Home = R.drawable.ic_home
    @DrawableRes val Services = R.drawable.ic_servicos // Corrigido para usar ic_servicos
    @DrawableRes val Products = R.drawable.ic_produtos
    @DrawableRes val Messages = R.drawable.ic_mensagens
    @DrawableRes val Profile = R.drawable.ic_perfil
    @DrawableRes val Search = R.drawable.ic_search
    @DrawableRes val Cart = R.drawable.ic_carrinho
    @DrawableRes val Bell = R.drawable.ic_notification // Agora usando o ícone correto de notificação
    @DrawableRes val Back = R.drawable.ic_back
    @DrawableRes val Edit = R.drawable.ic_edit // Agora usando o ícone correto de editar
    @DrawableRes val Delete = R.drawable.ic_delete // Usando ícone correto de deletar
    @DrawableRes val Add = R.drawable.ic_add // Agora usando o ícone correto de adicionar
    @DrawableRes val Send = R.drawable.ic_add // Usando ícone de adicionar como enviar
    @DrawableRes val Star = R.drawable.ic_star // Agora usando o ícone correto de estrela
    @DrawableRes val Check = R.drawable.ic_check
    @DrawableRes val Close = R.drawable.ic_delete // Usando ícone de deletar como close
    @DrawableRes val Language = R.drawable.idioma
    @DrawableRes val Privacy = R.drawable.ic_privacidade
    @DrawableRes val Support = R.drawable.ic_suporte
    @DrawableRes val Info = R.drawable.ic_ajuda
    @DrawableRes val Phone = R.drawable.ic_telefone
    @DrawableRes val Pix = R.drawable.ic_pix
    @DrawableRes val PropostaAceita = R.drawable.ic_proposta_aceita
    @DrawableRes val Anuncios = R.drawable.ic_anuncios
    
    // Novos ícones adicionados
    @DrawableRes val Time = R.drawable.ic_time // Ícone de tempo para "Tempo no TaskGo"
    @DrawableRes val ManageProposals = R.drawable.ic_gerenciar_proposta // Ícone para "Gerenciar Propostas"
    @DrawableRes val MyOrders = R.drawable.ic_meus_pedidos // Ícone para "Meus Pedidos"
    @DrawableRes val Arrow = R.drawable.ic_arrow // Ícone de seta para "Menu do Perfil"
    
    // Logos principais - já existem!
    @DrawableRes val TaskGoLogoVertical = R.drawable.ic_taskgo_logo_vertical
    @DrawableRes val TaskGoLogoHorizontal = R.drawable.ic_taskgo_logo_horizontal
    
    // Ícones de sistema e configurações
    @DrawableRes val Settings = R.drawable.ic_configuracoes
    @DrawableRes val Update = R.drawable.ic_atualizacao
    @DrawableRes val Account = R.drawable.ic_conta
    @DrawableRes val MyData = R.drawable.ic_meus_dados
    @DrawableRes val CreditCard = R.drawable.ic_cartao_de_credito
    @DrawableRes val DebitCard = R.drawable.ic_cartao_de_debito
    @DrawableRes val AlterarSenha = R.drawable.alterar_senha
    @DrawableRes val Package = R.drawable.ic_produtos // Using products icon for package
    @DrawableRes val Google = R.drawable.ic_google_logo // Google logo para login
    
    // Banners promocionais
    @DrawableRes val BannerLocalProviders = R.drawable.banner_prestadores_locais // Banner de prestadores locais
    @DrawableRes val BannerDiscountedProducts = R.drawable.banner_produtos_descontos // Banner de produtos com descontos
    @DrawableRes val BannerServiceOrders = R.drawable.banner_ordens_servico // Banner de ordens de serviço
    
    // Tamanhos padrão para ícones
    object Sizes {
        val Small = 18.dp      // Para ícones pequenos em listas
        val Medium = 24.dp     // Tamanho padrão (mais usado)
        val Large = 28.dp      // Para ícones em top bars
        val ExtraLarge = 32.dp // Para ícones grandes
        val Navigation = 24.dp // Para bottom navigation
    }
}

/**
 * Componente helper para ícones padronizados com tamanho consistente
 * Garante que todos os ícones tenham o mesmo tamanho visual independente do tamanho do arquivo WebP
 */
@Composable
fun TGIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = TGIcons.Sizes.Medium,
    tint: Color = Color.Unspecified
) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}
