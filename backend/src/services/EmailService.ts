import nodemailer from 'nodemailer';

const SMTP_HOST = process.env.SMTP_HOST || 'smtp.gmail.com';
const SMTP_PORT = parseInt(process.env.SMTP_PORT || '587');
const SMTP_USER = process.env.SMTP_USER || '';
const SMTP_PASS = process.env.SMTP_PASS || '';
const SMTP_FROM = process.env.SMTP_FROM || SMTP_USER;
const APP_URL = process.env.APP_URL || 'https://taskgo-production.up.railway.app';

let transporter: nodemailer.Transporter | null = null;

function getTransporter(): nodemailer.Transporter {
  if (!transporter) {
    transporter = nodemailer.createTransport({
      host: SMTP_HOST,
      port: SMTP_PORT,
      secure: SMTP_PORT === 465, // true para 465, false para outras portas
      auth: {
        user: SMTP_USER,
        pass: SMTP_PASS,
      },
    });
  }
  return transporter;
}

export class EmailService {
  /**
   * Enviar email de verificação
   */
  static async sendVerificationEmail(email: string, token: string): Promise<void> {
    const verificationUrl = `${APP_URL}/verify-email?token=${token}`;

    const html = `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="utf-8">
          <title>Verificar Email - TaskGo</title>
        </head>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
          <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
            <h1 style="color: #4CAF50;">Bem-vindo ao TaskGo!</h1>
            <p>Obrigado por se cadastrar. Por favor, verifique seu endereço de email clicando no botão abaixo:</p>
            <div style="text-align: center; margin: 30px 0;">
              <a href="${verificationUrl}" style="background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">
                Verificar Email
              </a>
            </div>
            <p>Ou copie e cole este link no seu navegador:</p>
            <p style="word-break: break-all; color: #666;">${verificationUrl}</p>
            <p style="color: #999; font-size: 12px; margin-top: 30px;">
              Este link expira em 24 horas. Se você não criou uma conta no TaskGo, ignore este email.
            </p>
          </div>
        </body>
      </html>
    `;

    await getTransporter().sendMail({
      from: `TaskGo <${SMTP_FROM}>`,
      to: email,
      subject: 'Verifique seu email - TaskGo',
      html,
    });
  }

  /**
   * Enviar email de reset de senha
   */
  static async sendPasswordResetEmail(email: string, token: string): Promise<void> {
    const resetUrl = `${APP_URL}/reset-password?token=${token}`;

    const html = `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="utf-8">
          <title>Redefinir Senha - TaskGo</title>
        </head>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
          <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
            <h1 style="color: #4CAF50;">Redefinir Senha</h1>
            <p>Recebemos uma solicitação para redefinir a senha da sua conta TaskGo.</p>
            <p>Clique no botão abaixo para criar uma nova senha:</p>
            <div style="text-align: center; margin: 30px 0;">
              <a href="${resetUrl}" style="background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">
                Redefinir Senha
              </a>
            </div>
            <p>Ou copie e cole este link no seu navegador:</p>
            <p style="word-break: break-all; color: #666;">${resetUrl}</p>
            <p style="color: #999; font-size: 12px; margin-top: 30px;">
              Este link expira em 1 hora. Se você não solicitou a redefinição de senha, ignore este email.
            </p>
          </div>
        </body>
      </html>
    `;

    await getTransporter().sendMail({
      from: `TaskGo <${SMTP_FROM}>`,
      to: email,
      subject: 'Redefinir Senha - TaskGo',
      html,
    });
  }

  /**
   * Enviar código 2FA por email
   */
  static async send2FACode(email: string, code: string): Promise<void> {
    const html = `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="utf-8">
          <title>Código de Verificação - TaskGo</title>
        </head>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
          <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
            <h1 style="color: #4CAF50;">Código de Verificação</h1>
            <p>Seu código de verificação de duas etapas é:</p>
            <div style="text-align: center; margin: 30px 0;">
              <div style="background-color: #f5f5f5; padding: 20px; border-radius: 5px; display: inline-block;">
                <h2 style="margin: 0; color: #4CAF50; font-size: 32px; letter-spacing: 5px;">${code}</h2>
              </div>
            </div>
            <p style="color: #999; font-size: 12px;">
              Este código expira em 10 minutos. Se você não solicitou este código, ignore este email.
            </p>
          </div>
        </body>
      </html>
    `;

    await getTransporter().sendMail({
      from: `TaskGo <${SMTP_FROM}>`,
      to: email,
      subject: 'Código de Verificação - TaskGo',
      html,
    });
  }

  /**
   * Testar conexão SMTP
   */
  static async testConnection(): Promise<boolean> {
    try {
      await getTransporter().verify();
      return true;
    } catch (error) {
      console.error('Erro ao verificar conexão SMTP:', error);
      return false;
    }
  }
}
