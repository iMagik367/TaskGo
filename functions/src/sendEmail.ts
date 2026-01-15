import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';
import * as nodemailer from 'nodemailer';

// Configurar transporte SMTP usando variáveis de ambiente
const getTransporter = () => {
  const smtpHost = functions.config().smtp?.host || 'smtp.gmail.com';
  const smtpPort = parseInt(functions.config().smtp?.port || '465');
  const smtpUser = functions.config().smtp?.user;
  const smtpPassword = functions.config().smtp?.password;

  if (!smtpUser || !smtpPassword) {
    throw new Error('SMTP credentials not configured. Set smtp.user and smtp.password');
  }

  return nodemailer.createTransport({
    host: smtpHost,
    port: smtpPort,
    secure: smtpPort === 465, // true para 465, false para outras portas
    auth: {
      user: smtpUser,
      pass: smtpPassword,
    },
  });
};

/**
 * Cloud Function que envia email quando um documento é criado na coleção 'mail'
 * 
 * Formato do documento:
 * {
 *   to: string | string[],      // Destinatário(s)
 *   from?: string,               // Remetente (opcional)
 *   cc?: string | string[],      // Cópia (opcional)
 *   bcc?: string | string[],     // Cópia oculta (opcional)
 *   replyTo?: string,            // Responder para (opcional)
 *   message: {
 *     subject: string,           // Assunto
 *     text?: string,             // Texto plano (opcional)
 *     html?: string,             // HTML (opcional)
 *   },
 *   headers?: Record<string, string> // Headers customizados (opcional)
 * }
 */
export const sendEmail = functions.firestore
  .document('mail/{mailId}')
  .onCreate(async (snap, context) => {
    const mailData = snap.data();
    const mailId = context.params.mailId;

    // Ignorar se já foi processado
    if (mailData.status === 'sent' || mailData.status === 'processing') {
      console.log(`Document ${mailId} already processed, skipping`);
      return null;
    }

    // Marcar como processando
    await snap.ref.update({
      status: 'processing',
      processingAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    try {
      const transporter = getTransporter();

      // Preparar opções do email
      const defaultFrom = functions.config().email?.default_from;
      const defaultReplyTo = functions.config().email?.default_reply_to;

      const mailOptions: nodemailer.SendMailOptions = {
        from: mailData.from || defaultFrom || 'noreply@taskgo.com',
        to: Array.isArray(mailData.to) ? mailData.to.join(', ') : mailData.to,
        cc: mailData.cc
          ? Array.isArray(mailData.cc)
            ? mailData.cc.join(', ')
            : mailData.cc
          : undefined,
        bcc: mailData.bcc
          ? Array.isArray(mailData.bcc)
            ? mailData.bcc.join(', ')
            : mailData.bcc
          : undefined,
        subject: mailData.message?.subject || 'Sem assunto',
        text: mailData.message?.text,
        html: mailData.message?.html,
        replyTo: mailData.replyTo || defaultReplyTo,
        headers: mailData.headers || {},
      };

      // Validar campos obrigatórios
      if (!mailOptions.to) {
        throw new Error('Destination email (to) is required');
      }

      // Enviar email
      const info = await transporter.sendMail(mailOptions);

      // Atualizar documento com status de sucesso
      await snap.ref.update({
        status: 'sent',
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        messageId: info.messageId,
        error: admin.firestore.FieldValue.delete(),
      });

      console.log(`Email sent successfully for document ${mailId}:`, info.messageId);
      return null;
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      console.error(`Error sending email for document ${mailId}:`, error);

      // Atualizar documento com status de erro
      await snap.ref.update({
        status: 'error',
        error: errorMessage,
        failedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Não lançar erro para não reprocessar indefinidamente
      // Log do erro será visível nos logs do Firebase
      return null;
    }
  });












