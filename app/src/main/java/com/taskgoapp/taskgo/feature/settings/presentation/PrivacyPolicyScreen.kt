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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Política de Privacidade",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        
        val policySections = remember {
            listOf(
            PolicySectionData(
                title = "📘 1. INFORMAÇÕES QUE COLETAMOS",
                paragraphs = listOf(
                    "Durante o uso do TaskGo, podemos coletar os seguintes tipos de dados:"
                )
            ),
            PolicySectionData(
                title = "🧍 1.1. Informações pessoais",
                paragraphs = listOf(
                    "• Nome completo",
                    "• Endereço de e-mail",
                    "• Número de telefone",
                    "• Documento de identificação (ex: RG, CPF ou CNPJ)",
                    "• Foto de perfil",
                    "",
                    "Essas informações são utilizadas para criar e verificar sua conta, permitindo que você contrate ou ofereça serviços e produtos com segurança."
                )
            ),
            PolicySectionData(
                title = "📍 1.2. Dados de localização",
                paragraphs = listOf(
                    "O TaskGo pode coletar e processar informações sobre sua localização geográfica aproximada ou precisa, com o objetivo de:",
                    "",
                    "• Exibir serviços e prestadores próximos.",
                    "• Melhorar a experiência de busca e entrega.",
                    "• Garantir a veracidade das informações de área de atuação.",
                    "",
                    "O compartilhamento da localização é opcional e pode ser controlado nas configurações do seu dispositivo."
                )
            ),
            PolicySectionData(
                title = "💬 1.3. Mensagens e comunicações",
                paragraphs = listOf(
                    "Quando você utiliza o chat integrado do TaskGo:",
                    "",
                    "• As mensagens são armazenadas de forma segura e criptografada no Firebase Firestore.",
                    "• As conversas podem ser revisadas em caso de denúncias, para manter a segurança da comunidade."
                )
            ),
            PolicySectionData(
                title = "🧾 1.4. Dados de uso",
                paragraphs = listOf(
                    "Coletamos automaticamente informações sobre como você interage com o aplicativo, como:",
                    "",
                    "• Versão do app",
                    "• Sistema operacional",
                    "• Tipo de dispositivo",
                    "• Endereço IP e logs de acesso",
                    "• Histórico de navegação interna e preferências",
                    "",
                    "Esses dados são usados apenas para análises técnicas, métricas e melhorias de desempenho."
                )
            ),
            PolicySectionData(
                title = "💳 1.5. Informações financeiras",
                paragraphs = listOf(
                    "Caso realize pagamentos ou vendas dentro do aplicativo:",
                    "",
                    "• Os dados de pagamento (cartão, conta bancária etc.) são processados exclusivamente por gateways de pagamento confiáveis, como Stripe ou Mercado Pago.",
                    "• O TaskGo não armazena dados sensíveis de pagamento em seus servidores."
                )
            ),
            PolicySectionData(
                title = "🧠 2. COMO UTILIZAMOS SEUS DADOS",
                paragraphs = listOf(
                    "Os dados coletados são usados para:",
                    "",
                    "• Criar e gerenciar sua conta.",
                    "• Permitir interações entre clientes e prestadores/vendedores.",
                    "• Processar transações e repasses financeiros.",
                    "• Enviar notificações sobre ordens, mensagens e atualizações.",
                    "• Melhorar a segurança, desempenho e personalização do aplicativo.",
                    "• Cumprir obrigações legais e regulatórias."
                )
            ),
            PolicySectionData(
                title = "🔗 3. COMPARTILHAMENTO DE DADOS",
                paragraphs = listOf(
                    "O TaskGo não vende, aluga ou comercializa seus dados pessoais.",
                    "",
                    "No entanto, podemos compartilhar suas informações com:",
                    "",
                    "• Firebase (Google LLC) — para autenticação, banco de dados e armazenamento.",
                    "• Stripe / Mercado Pago — para processamento de pagamentos.",
                    "• OpenAI ou provedores de IA — para o chat automatizado de suporte.",
                    "• Autoridades legais — em caso de investigações, mediante solicitação formal.",
                    "",
                    "Todos os parceiros são escolhidos por sua confiabilidade, criptografia e conformidade legal (LGPD e GDPR)."
                )
            ),
            PolicySectionData(
                title = "🧱 4. ARMAZENAMENTO E SEGURANÇA",
                paragraphs = listOf(
                    "Seus dados são armazenados em servidores seguros do Firebase (Google Cloud Platform), com:",
                    "",
                    "• Criptografia AES-256 para dados em repouso.",
                    "• Criptografia TLS 1.3 para dados em trânsito.",
                    "• Controle de acesso baseado em autenticação (request.auth.uid).",
                    "• Backups automáticos e logs de auditoria."
                )
            ),
            PolicySectionData(
                title = "🧹 5. RETENÇÃO E EXCLUSÃO DE DADOS",
                paragraphs = listOf(
                    "Seus dados pessoais são mantidos enquanto sua conta estiver ativa.",
                    "",
                    "Você pode solicitar a exclusão definitiva da sua conta e de todos os dados associados a qualquer momento.",
                    "",
                    "Após a exclusão, apenas registros necessários por lei (ex: transações financeiras) são mantidos temporariamente.",
                    "",
                    "Para solicitar exclusão, envie um e-mail para: suporte@taskgo.app"
                )
            ),
            PolicySectionData(
                title = "🧭 6. SEUS DIREITOS",
                paragraphs = listOf(
                    "De acordo com a LGPD (Lei nº 13.709/2018) e o GDPR, você tem direito a:",
                    "",
                    "• Acessar seus dados pessoais.",
                    "• Corrigir informações incorretas.",
                    "• Solicitar a exclusão de dados.",
                    "• Revogar o consentimento a qualquer momento.",
                    "• Saber como seus dados são tratados.",
                    "",
                    "Esses direitos podem ser exercidos dentro do app ou por contato direto com nosso suporte."
                )
            ),
            PolicySectionData(
                title = "👁 7. COOKIES E TECNOLOGIAS SIMILARES",
                paragraphs = listOf(
                    "O TaskGo utiliza cookies e identificadores locais apenas para:",
                    "",
                    "• Manter sua sessão autenticada.",
                    "• Lembrar preferências do usuário.",
                    "• Coletar métricas de uso para melhorias.",
                    "",
                    "Nenhum cookie é usado para rastreamento publicitário fora do aplicativo."
                )
            ),
            PolicySectionData(
                title = "🌎 8. TRANSFERÊNCIA INTERNACIONAL DE DADOS",
                paragraphs = listOf(
                    "Por utilizar serviços do Google Firebase, alguns dados podem ser processados em servidores localizados fora do Brasil.",
                    "",
                    "Todos os dados transferidos seguem padrões internacionais de segurança e privacidade (GDPR e LGPD)."
                )
            ),
            PolicySectionData(
                title = "🧩 9. ATUALIZAÇÕES DESTA POLÍTICA",
                paragraphs = listOf(
                    "Podemos atualizar esta Política de Privacidade periodicamente.",
                    "",
                    "A versão mais recente estará sempre disponível no aplicativo e em nosso site oficial.",
                    "",
                    "Notificaremos você em caso de alterações relevantes."
                )
            ),
            PolicySectionData(
                title = "📬 10. CONTATO",
                paragraphs = listOf(
                    "Se você tiver dúvidas, solicitações ou denúncias relacionadas a esta Política de Privacidade, entre em contato conosco:",
                    "",
                    "📧 E-mail: suporte@taskgo.app",
                    "🌐 Site: https://taskgo.app",
                    "📍 Empresa: TaskGo Tecnologia Digital LTDA",
                    "📄 CNPJ: (adicione aqui seu número quando registrar a empresa)"
                )
            ),
            PolicySectionData(
                title = "✅ Conformidade",
                paragraphs = listOf(
                    "Esta Política está em conformidade com:",
                    "",
                    "• Lei Geral de Proteção de Dados (LGPD - Lei nº 13.709/2018)",
                    "• Regulamento Geral de Proteção de Dados (GDPR - União Europeia)",
                    "• Políticas de Privacidade do Google Play Developer Program"
                )
            )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Bem-vindo ao TaskGo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Um aplicativo desenvolvido para conectar pessoas, serviços e produtos de forma simples, segura e inteligente.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esta Política de Privacidade explica como coletamos, usamos, armazenamos e protegemos suas informações pessoais quando você utiliza nosso aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ao utilizar o TaskGo, você concorda com os termos descritos nesta política.",
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            policySections.forEach { section ->
                PolicySection(section)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class PolicySectionData(
    val title: String,
    val paragraphs: List<String>
)

@Composable
private fun PolicySection(section: PolicySectionData) {
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
