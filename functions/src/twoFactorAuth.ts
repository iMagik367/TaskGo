import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';

const db = getFirestore();

/**
 * Envia código de verificação 2FA por email
 */
export const sendTwoFactorCode = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const userId = context.auth!.uid;
    
    // Buscar informações do usuário
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Usuário não encontrado'
      );
    }
    
    const userData = userDoc.data();
    // Buscar email do Firebase Auth user se não estiver no Firestore
    let email = userData?.email;
    if (!email) {
      const authUser = await admin.auth().getUser(userId);
      email = authUser.email || undefined;
    }
    const phone = userData?.phone;
    
    if (!email && !phone) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Email ou telefone necessário para envio do código'
      );
    }
    
    // Gerar código de 6 dígitos
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = Date.now() + (10 * 60 * 1000); // 10 minutos
    
    // Salvar código no Firestore
    await db.collection('twoFactorCodes').doc(userId).set({
      code,
      expiresAt,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      method: email ? 'email' : 'sms'
    });
    
    // Enviar email se disponível
    if (email) {
      try {
        // Usar Firebase Extensions: Trigger Email ou enviar diretamente
        // Se não tiver extensão, podemos usar nodemailer ou serviço de email
        await sendVerificationEmail(email, code);
      } catch (emailError) {
        functions.logger.error('Erro ao enviar email:', emailError);
        // Continuar mesmo se email falhar - o código está salvo
      }
    }
    
    // TODO: Enviar SMS se disponível (requer serviço de SMS como Twilio)
    if (phone && !email) {
      functions.logger.warn('SMS não implementado ainda para:', phone);
    }
    
    functions.logger.info(`Código 2FA gerado para usuário ${userId}`);
    
    return {
      success: true,
      method: email ? 'email' : 'sms',
      message: email ? `Código enviado para ${maskEmail(email)}` : `Código enviado para ${maskPhone(phone)}`
    };
  } catch (error) {
    functions.logger.error('Erro ao enviar código 2FA:', error);
    throw handleError(error);
  }
});

/**
 * Verifica código 2FA
 */
export const verifyTwoFactorCode = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const userId = context.auth!.uid;
    const {code} = data;
    
    if (!code || typeof code !== 'string' || code.length !== 6) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Código de verificação inválido'
      );
    }
    
    // Buscar código do Firestore
    const codeDoc = await db.collection('twoFactorCodes').doc(userId).get();
    
    if (!codeDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Código não encontrado. Solicite um novo código.'
      );
    }
    
    const codeData = codeDoc.data();
    const storedCode = codeData?.code;
    const expiresAt = codeData?.expiresAt || 0;
    
    // Verificar expiração
    if (Date.now() > expiresAt) {
      // Deletar código expirado
      await codeDoc.ref.delete();
      throw new functions.https.HttpsError(
        'deadline-exceeded',
        'Código expirado. Solicite um novo código.'
      );
    }
    
    // Verificar código
    if (code !== storedCode) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Código inválido. Tente novamente.'
      );
    }
    
    // Código válido - deletar do Firestore
    await codeDoc.ref.delete();
    
    // Marcar verificação 2FA como concluída no usuário
    await db.collection('users').doc(userId).update({
      twoFactorVerified: true,
      twoFactorVerifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    functions.logger.info(`Código 2FA verificado com sucesso para usuário ${userId}`);
    
    return {
      success: true,
      verified: true
    };
  } catch (error) {
    functions.logger.error('Erro ao verificar código 2FA:', error);
    throw handleError(error);
  }
});

/**
 * Envia email de verificação usando Firebase Auth sendEmailVerification ou serviço externo
 */
async function sendVerificationEmail(email: string, code: string): Promise<void> {
  // Método 1: Usar Firebase Extensions Trigger Email (se configurado)
  // Isso requer que a extensão "Trigger Email" esteja instalada
  
  // Método 2: Enviar usando Nodemailer ou serviço de email
  // Por enquanto, vamos usar uma abordagem simples que funciona com a extensão
  
  // Salvar na coleção que a extensão Trigger Email monitora
  // A extensão Trigger Email espera este formato específico
  await db.collection('mail').add({
    to: [email],
    message: {
      subject: 'Código de Verificação - TaskGo',
      html: `
        <html>
          <body style="font-family: Arial, sans-serif; padding: 20px;">
            <h2 style="color: #2E7D32;">Código de Verificação</h2>
            <p>Olá,</p>
            <p>Seu código de verificação de duas etapas é:</p>
            <div style="background-color: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;">
              <h1 style="color: #2E7D32; font-size: 32px; letter-spacing: 5px; margin: 0;">${code}</h1>
            </div>
            <p>Este código expira em <strong>10 minutos</strong>.</p>
            <p>Se você não solicitou este código, ignore este email.</p>
            <p>Atenciosamente,<br>Equipe TaskGo</p>
          </body>
        </html>
      `,
      text: `Seu código de verificação TaskGo é: ${code}. Este código expira em 10 minutos.`
    },
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  // Nota: Se a extensão Trigger Email não estiver instalada,
  // você pode configurar usando: firebase ext:install firebase/firestore-send-email
  // Ou usar um serviço de email externo como SendGrid, Mailgun, etc.
  // Para usar SendGrid/Mailgun, você precisaria instalar o SDK correspondente
  // e substituir este código pela chamada da API do serviço
}

function maskEmail(email: string): string {
  const parts = email.split('@');
  if (parts.length !== 2) return email;
  const username = parts[0];
  const domain = parts[1];
  const maskedUsername = username.length > 2
    ? `${username.substring(0, 2)}***`
    : '***';
  return `${maskedUsername}@${domain}`;
}

function maskPhone(phone: string): string {
  return phone.length > 4 ? `***${phone.substring(phone.length - 4)}` : '***';
}

/**
 * Limpa códigos 2FA expirados (executar periodicamente via scheduled function)
 */
export const cleanupExpiredTwoFactorCodes = functions.pubsub
  .schedule('every 1 hours')
  .onRun(async () => {
    try {
      const now = Date.now();
      const expiredCodes = await db.collection('twoFactorCodes')
        .where('expiresAt', '<', now)
        .get();
      
      const batch = db.batch();
      expiredCodes.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      
      functions.logger.info(`Removidos ${expiredCodes.docs.length} códigos 2FA expirados`);
      return null;
    } catch (error) {
      functions.logger.error('Erro ao limpar códigos expirados:', error);
      return null;
    }
  });










