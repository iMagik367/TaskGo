package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Termos de Uso",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val lastUpdated = remember {
            "Novembro de 2025"
        }
        
        val termsSections = listOf(
            TermsSectionData(
                title = "📘 1. DEFINIÇÕES",
                paragraphs = listOf(
                    "Para os fins deste Termo, considera-se:",
                    "",
                    "• Aplicativo / Plataforma / TaskGo — o aplicativo móvel e seus serviços associados.",
                    "• Usuário — toda pessoa física ou jurídica que acessa, utiliza ou se cadastra no TaskGo.",
                    "• Prestador / Vendedor — usuário que oferece serviços ou produtos por meio do aplicativo.",
                    "• Cliente / Comprador — usuário que contrata serviços ou compra produtos através da plataforma.",
                    "• Conta — o perfil cadastrado pelo usuário para utilização dos recursos do TaskGo.",
                    "• Transação — qualquer interação comercial, financeira ou contratual realizada no app."
                )
            ),
            TermsSectionData(
                title = "🧾 2. ACEITAÇÃO DOS TERMOS",
                paragraphs = listOf(
                    "O uso do aplicativo implica aceitação integral e irrevogável destes Termos de Uso e da Política de Privacidade.",
                    "",
                    "Caso o usuário não concorde com qualquer cláusula, deverá abster-se de utilizar o TaskGo e desinstalar o aplicativo."
                )
            ),
            TermsSectionData(
                title = "👤 3. CADASTRO E CONTA DE USUÁRIO",
                paragraphs = listOf(
                    "3.1. Requisitos",
                    "",
                    "Para criar uma conta no TaskGo, o usuário deve:",
                    "",
                    "• Ter 18 anos ou mais (ou estar assistido por responsável legal).",
                    "• Fornecer informações verdadeiras, completas e atualizadas.",
                    "• Manter a confidencialidade de suas credenciais de acesso.",
                    "",
                    "3.2. Responsabilidade",
                    "",
                    "O usuário é inteiramente responsável por todas as atividades realizadas em sua conta, incluindo:",
                    "",
                    "• Interações, publicações e negociações.",
                    "• Cadastramento de produtos, serviços e informações.",
                    "• Cumprimento de compromissos assumidos com outros usuários.",
                    "",
                    "A TaskGo não se responsabiliza por danos ou prejuízos decorrentes do uso indevido da conta por terceiros."
                )
            ),
            TermsSectionData(
                title = "🧩 4. FUNCIONALIDADES DA PLATAFORMA",
                paragraphs = listOf(
                    "O TaskGo oferece as seguintes funcionalidades:",
                    "",
                    "• Cadastro e autenticação de usuários.",
                    "• Busca e oferta de serviços e produtos.",
                    "• Criação de ordens de serviço e pedidos de compra.",
                    "• Chat em tempo real entre usuários.",
                    "• Sistema de pagamentos seguro via parceiros externos.",
                    "• Avaliações e feedbacks entre clientes e prestadores.",
                    "• Verificação de documentos e identidade.",
                    "• Notificações push e alertas de status.",
                    "",
                    "O aplicativo pode ser atualizado periodicamente para incluir novas funcionalidades ou remover aquelas que se tornem obsoletas."
                )
            ),
            TermsSectionData(
                title = "💳 5. TRANSAÇÕES, PAGAMENTOS E TAXAS",
                paragraphs = listOf(
                    "5.1. Pagamentos",
                    "",
                    "Todos os pagamentos são processados de forma segura por gateways de pagamento (como Stripe, Mercado Pago ou equivalentes).",
                    "",
                    "O TaskGo não armazena dados financeiros sensíveis (cartões, senhas, chaves PIX etc).",
                    "",
                    "5.2. Taxas",
                    "",
                    "A TaskGo poderá cobrar taxas de serviço ou comissões sobre transações realizadas na plataforma.",
                    "",
                    "Essas taxas serão sempre informadas de forma clara antes da conclusão da transação.",
                    "",
                    "5.3. Disputas",
                    "",
                    "Eventuais conflitos ou reembolsos entre usuários deverão ser resolvidos inicialmente entre as partes por meio do chat.",
                    "",
                    "Caso não haja acordo, a moderação da TaskGo poderá intervir e propor uma solução imparcial."
                )
            ),
            TermsSectionData(
                title = "📦 6. PRODUTOS E SERVIÇOS OFERECIDOS",
                paragraphs = listOf(
                    "Os prestadores e vendedores são exclusivamente responsáveis por:",
                    "",
                    "• Cumprir as condições acordadas com os clientes.",
                    "• Garantir a qualidade, legalidade e entrega do serviço/produto.",
                    "• Fornecer informações corretas, imagens reais e descrições verdadeiras.",
                    "",
                    "A TaskGo atua apenas como intermediadora tecnológica entre usuários, não sendo parte direta nas transações."
                )
            ),
            TermsSectionData(
                title = "🚫 7. CONDUTAS PROIBIDAS",
                paragraphs = listOf(
                    "É expressamente proibido:",
                    "",
                    "• Fornecer informações falsas ou enganosas.",
                    "• Publicar conteúdos ofensivos, discriminatórios ou ilegais.",
                    "• Utilizar o TaskGo para atividades ilícitas, fraudulentas ou imorais.",
                    "• Cadastrar produtos ou serviços proibidos por lei.",
                    "• Copiar, modificar ou redistribuir o aplicativo sem autorização.",
                    "• Violar direitos de propriedade intelectual da TaskGo ou de terceiros.",
                    "• Tentar obter acesso não autorizado a dados, contas ou servidores.",
                    "",
                    "O descumprimento de qualquer dessas regras poderá resultar em suspensão, exclusão da conta e reporte às autoridades competentes."
                )
            ),
            TermsSectionData(
                title = "🔐 8. PRIVACIDADE E SEGURANÇA",
                paragraphs = listOf(
                    "O tratamento dos dados pessoais dos usuários é regido pela Política de Privacidade do TaskGo, disponível em:",
                    "",
                    "👉 https://taskgo.app/politica-de-privacidade",
                    "",
                    "Em resumo:",
                    "",
                    "• Os dados são coletados apenas para funcionamento e segurança do app.",
                    "• Todas as informações são armazenadas em servidores seguros do Firebase (Google Cloud).",
                    "• O usuário pode solicitar exclusão definitiva de sua conta a qualquer momento."
                )
            ),
            TermsSectionData(
                title = "⚙ 9. LICENÇA DE USO",
                paragraphs = listOf(
                    "A TaskGo concede ao usuário uma licença limitada, não exclusiva e intransferível para utilizar o aplicativo.",
                    "",
                    "O usuário não adquire propriedade intelectual sobre o software, interface ou código.",
                    "",
                    "É proibido:",
                    "",
                    "• Copiar, descompilar ou modificar partes do app.",
                    "• Comercializar ou redistribuir o TaskGo sem autorização formal."
                )
            ),
            TermsSectionData(
                title = "🧠 10. PROPRIEDADE INTELECTUAL",
                paragraphs = listOf(
                    "Todo o conteúdo, layout, logotipo, código, design e banco de dados do TaskGo pertencem exclusivamente à TaskGo Tecnologia Digital LTDA, sendo protegidos pelas leis de direitos autorais e propriedade industrial.",
                    "",
                    "O uso indevido desses elementos poderá gerar responsabilidade civil e criminal."
                )
            ),
            TermsSectionData(
                title = "🧾 11. SUPORTE E COMUNICAÇÃO",
                paragraphs = listOf(
                    "O usuário pode entrar em contato com a equipe de suporte pelos canais oficiais:",
                    "",
                    "📧 E-mail: suporte@taskgo.app",
                    "🌐 Site: https://taskgo.app",
                    "",
                    "O suporte atende a dúvidas técnicas, denúncias, problemas com pagamentos e solicitações de exclusão de conta."
                )
            ),
            TermsSectionData(
                title = "🧭 12. RESPONSABILIDADES",
                paragraphs = listOf(
                    "Do Usuário:",
                    "",
                    "• Fornecer informações verídicas e manter o comportamento ético.",
                    "• Cumprir compromissos financeiros e contratuais.",
                    "• Respeitar outros usuários e as normas legais vigentes.",
                    "",
                    "Da TaskGo:",
                    "",
                    "• Garantir a disponibilidade técnica da plataforma.",
                    "• Adotar medidas de segurança e privacidade adequadas.",
                    "• Intervir em casos de denúncia, fraude ou violação dos termos.",
                    "",
                    "A TaskGo não se responsabiliza por:",
                    "",
                    "• Erros cometidos pelos usuários.",
                    "• Perdas financeiras decorrentes de má conduta de terceiros.",
                    "• Indisponibilidade temporária do serviço por manutenção ou força maior."
                )
            ),
            TermsSectionData(
                title = "🔄 13. ATUALIZAÇÕES DOS TERMOS",
                paragraphs = listOf(
                    "A TaskGo poderá atualizar este Termo de Uso a qualquer momento.",
                    "",
                    "Alterações relevantes serão comunicadas no aplicativo e/ou por e-mail.",
                    "",
                    "O uso continuado do TaskGo após alterações constitui aceitação das novas condições."
                )
            ),
            TermsSectionData(
                title = "⚖ 14. LEGISLAÇÃO E FORO",
                paragraphs = listOf(
                    "Este Termo é regido pelas leis da República Federativa do Brasil.",
                    "",
                    "Qualquer controvérsia será dirimida no foro da comarca de [sua cidade e estado], com exclusão de qualquer outro, por mais privilegiado que seja."
                )
            ),
            TermsSectionData(
                title = "✅ 15. CONTATO E INFORMAÇÕES DA EMPRESA",
                paragraphs = listOf(
                    "TaskGo Tecnologia Digital LTDA",
                    "",
                    "📧 E-mail: suporte@taskgo.app",
                    "🌐 Site: https://taskgo.app",
                    "📍 Localização: [insira cidade/estado]",
                    "📄 CNPJ: [insira número quando disponível]"
                )
            ),
            TermsSectionData(
                title = "📌 Resumo",
                paragraphs = listOf(
                    "O TaskGo é uma plataforma que conecta pessoas, serviços e produtos de forma segura e colaborativa.",
                    "",
                    "Ao utilizar o app, você concorda em agir com responsabilidade, respeitar outros usuários e cumprir as leis aplicáveis."
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "📜 Termos de Uso — TaskGo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Última atualização: $lastUpdated",
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bem-vindo ao TaskGo, um aplicativo desenvolvido por TaskGo Tecnologia Digital LTDA, criado para conectar pessoas que oferecem e buscam serviços e produtos em um ambiente seguro, prático e inteligente.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Estes Termos de Uso regulam o acesso e utilização do aplicativo, do site e de todos os serviços relacionados oferecidos pela TaskGo.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ao utilizar o aplicativo, você declara que leu, compreendeu e concorda integralmente com estes termos.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            termsSections.forEach { section ->
                TermsSection(section)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class TermsSectionData(
    val title: String,
    val paragraphs: List<String>
)

@Composable
private fun TermsSection(section: TermsSectionData) {
    Text(
        text = section.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TaskGoTextBlack
    )
    Spacer(modifier = Modifier.height(8.dp))
    section.paragraphs.forEachIndexed { index, paragraph ->
        if (paragraph.isNotBlank()) {
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextBlack
            )
            if (index != section.paragraphs.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
