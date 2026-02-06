import { Router, Request, Response } from 'express';
import rateLimit from 'express-rate-limit';
import { AuthService } from '../services/AuthService';
import { GoogleAuthService } from '../services/GoogleAuthService';
import { TwoFactorService } from '../services/TwoFactorService';
import { EmailService } from '../services/EmailService';
import { authenticate } from '../middleware/auth';
import { query, transaction } from '../database/connection';
import {
  RegisterRequest,
  LoginRequest,
  GoogleLoginRequest,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  VerifyEmailRequest,
  Enable2FARequest,
  Verify2FARequest,
  AuthResponse,
  TwoFactorSetupResponse,
} from '../models/Auth';
import { User } from '../models/User';

const router = Router();

// Rate limiting para login
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 5, // 5 tentativas por IP
  message: 'Muitas tentativas de login. Tente novamente em 15 minutos.',
  standardHeaders: true,
  legacyHeaders: false,
});

// Rate limiting para registro
const registerLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hora
  max: 3, // 3 registros por IP por hora
  message: 'Muitas tentativas de registro. Tente novamente em 1 hora.',
});

// Rate limiting para password reset
const passwordResetLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hora
  max: 3, // 3 tentativas por IP por hora
  message: 'Muitas tentativas. Tente novamente em 1 hora.',
});

/**
 * POST /api/auth/register
 * Registrar novo usuário
 */
router.post('/register', registerLimiter, async (req: Request, res: Response) => {
  try {
    const { email, password, display_name, phone, role }: RegisterRequest = req.body;

    // Validações
    if (!email || !password) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Email e senha são obrigatórios',
      });
    }

    if (password.length < 8) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Senha deve ter no mínimo 8 caracteres',
      });
    }

    if (role && !['client', 'partner'].includes(role)) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Role inválido',
      });
    }

    // Verificar se email já existe
    const existingUser = await query(
      `SELECT id FROM users WHERE email = $1`,
      [email.toLowerCase()]
    );

    if (existingUser.rows.length > 0) {
      return res.status(409).json({
        status: 'error',
        code: 409,
        message: 'Email já cadastrado',
      });
    }

    // Criar usuário
    const passwordHash = await AuthService.hashPassword(password);
    const userRole = role || 'client';

    const result = await transaction(async (client) => {
      // Inserir usuário
      const userResult = await client.query(
        `INSERT INTO users (email, password_hash, display_name, phone, role, email_verified)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING id, email, role, display_name, phone, photo_url, email_verified`,
        [email.toLowerCase(), passwordHash, display_name, phone, userRole, false]
      );

      const user = userResult.rows[0];

      // Criar configurações padrão
      await client.query(
        `INSERT INTO user_settings (user_id) VALUES ($1)`,
        [user.id]
      );

      // Gerar token de verificação de email
      const verificationToken = await AuthService.createEmailVerificationToken(user.id);

      // Enviar email de verificação
      await EmailService.sendVerificationEmail(user.email, verificationToken);

      return user;
    });

    res.status(201).json({
      status: 'success',
      message: 'Usuário criado com sucesso. Verifique seu email para ativar a conta.',
      data: {
        user_id: result.id,
        email: result.email,
      },
    });
  } catch (error) {
    console.error('Erro no registro:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao criar usuário',
    });
  }
});

/**
 * POST /api/auth/login
 * Login com email e senha
 */
router.post('/login', loginLimiter, async (req: Request, res: Response) => {
  try {
    const { email, password, two_factor_code }: LoginRequest = req.body;

    if (!email || !password) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Email e senha são obrigatórios',
      });
    }

    // Buscar usuário
    const userResult = await query(
      `SELECT * FROM users WHERE email = $1`,
      [email.toLowerCase()]
    );

    if (userResult.rows.length === 0) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Credenciais inválidas',
      });
    }

    const user: User = userResult.rows[0];

    // Verificar se está bloqueado
    if (await AuthService.isUserLocked(user.id)) {
      return res.status(423).json({
        status: 'error',
        code: 423,
        message: 'Conta temporariamente bloqueada. Tente novamente mais tarde.',
      });
    }

    // Verificar senha
    if (!user.password_hash || !(await AuthService.verifyPassword(password, user.password_hash))) {
      await AuthService.incrementFailedLoginAttempts(user.id);
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Credenciais inválidas',
      });
    }

    // Verificar 2FA
    const twoFactorEnabled = await TwoFactorService.is2FAEnabled(user.id);
    if (twoFactorEnabled) {
      if (!two_factor_code) {
        return res.status(200).json({
          status: 'success',
          requires_2fa: true,
          message: 'Código de verificação 2FA necessário',
        });
      }

      const isValid2FA = await TwoFactorService.verify2FA(user.id, two_factor_code);
      if (!isValid2FA) {
        return res.status(401).json({
          status: 'error',
          code: 401,
          message: 'Código 2FA inválido',
        });
      }
    }

    // Login bem-sucedido
    await AuthService.resetFailedLoginAttempts(user.id);

    // Gerar tokens
    const accessToken = AuthService.generateAccessToken(user.id, user.email, user.role);
    const refreshToken = AuthService.generateRefreshToken();
    const expiresAt = AuthService.getRefreshTokenExpiration();

    await AuthService.saveRefreshToken(user.id, refreshToken, expiresAt);

    const response: AuthResponse = {
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        display_name: user.display_name,
        phone: user.phone,
        photo_url: user.photo_url,
        email_verified: user.email_verified,
        two_factor_enabled: twoFactorEnabled,
      },
      access_token: accessToken,
      refresh_token: refreshToken,
      expires_in: 15 * 60, // 15 minutos em segundos
    };

    res.json({
      status: 'success',
      data: response,
    });
  } catch (error) {
    console.error('Erro no login:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao fazer login',
    });
  }
});

/**
 * POST /api/auth/google
 * Login/Registro com Google OAuth
 */
router.post('/google', async (req: Request, res: Response) => {
  try {
    const { id_token }: GoogleLoginRequest = req.body;

    if (!id_token) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'ID token do Google é obrigatório',
      });
    }

    // Verificar token do Google
    const googleUser = await GoogleAuthService.verifyIdToken(id_token);
    if (!googleUser) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Token do Google inválido',
      });
    }

    // Criar ou atualizar usuário
    const user = await GoogleAuthService.createOrUpdateUserWithGoogle(
      googleUser.googleId,
      googleUser.email,
      googleUser.name,
      googleUser.picture
    );

    // Verificar 2FA
    const twoFactorEnabled = await TwoFactorService.is2FAEnabled(user.id);

    // Gerar tokens
    const accessToken = AuthService.generateAccessToken(user.id, user.email, user.role);
    const refreshToken = AuthService.generateRefreshToken();
    const expiresAt = AuthService.getRefreshTokenExpiration();

    await AuthService.saveRefreshToken(user.id, refreshToken, expiresAt);

    const response: AuthResponse = {
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        display_name: user.display_name,
        phone: user.phone,
        photo_url: user.photo_url,
        email_verified: user.email_verified,
        two_factor_enabled: twoFactorEnabled,
      },
      access_token: accessToken,
      refresh_token: refreshToken,
      expires_in: 15 * 60,
    };

    res.json({
      status: 'success',
      data: response,
    });
  } catch (error) {
    console.error('Erro no login Google:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao fazer login com Google',
    });
  }
});

/**
 * POST /api/auth/refresh
 * Renovar access token
 */
router.post('/refresh', async (req: Request, res: Response) => {
  try {
    const { refresh_token }: RefreshTokenRequest = req.body;

    if (!refresh_token) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Refresh token é obrigatório',
      });
    }

    // Validar refresh token
    const tokenData = await AuthService.validateRefreshToken(refresh_token);
    if (!tokenData) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Refresh token inválido ou expirado',
      });
    }

    // Buscar usuário
    const userResult = await query(
      `SELECT id, email, role FROM users WHERE id = $1`,
      [tokenData.user_id]
    );

    if (userResult.rows.length === 0) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Usuário não encontrado',
      });
    }

    const user = userResult.rows[0];

    // Gerar novo access token
    const accessToken = AuthService.generateAccessToken(user.id, user.email, user.role);

    res.json({
      status: 'success',
      data: {
        access_token: accessToken,
        expires_in: 15 * 60,
      },
    });
  } catch (error) {
    console.error('Erro ao renovar token:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao renovar token',
    });
  }
});

/**
 * POST /api/auth/logout
 * Logout (revogar refresh token)
 */
router.post('/logout', authenticate, async (req: Request, res: Response) => {
  try {
    const { refresh_token } = req.body;

    if (refresh_token) {
      await AuthService.revokeRefreshToken(refresh_token);
    } else {
      // Revogar todos os tokens do usuário
      if (req.userId) {
        await AuthService.revokeAllUserTokens(req.userId);
      }
    }

    res.json({
      status: 'success',
      message: 'Logout realizado com sucesso',
    });
  } catch (error) {
    console.error('Erro no logout:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao fazer logout',
    });
  }
});

/**
 * POST /api/auth/verify-email
 * Verificar email com token
 */
router.post('/verify-email', async (req: Request, res: Response) => {
  try {
    const { token }: VerifyEmailRequest = req.body;

    if (!token) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Token é obrigatório',
      });
    }

    const verified = await AuthService.verifyEmail(token);
    if (!verified) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Token inválido ou expirado',
      });
    }

    res.json({
      status: 'success',
      message: 'Email verificado com sucesso',
    });
  } catch (error) {
    console.error('Erro ao verificar email:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao verificar email',
    });
  }
});

/**
 * POST /api/auth/resend-verification
 * Reenviar email de verificação
 */
router.post('/resend-verification', async (req: Request, res: Response) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Email é obrigatório',
      });
    }

    const userResult = await query(
      `SELECT id, email, email_verified FROM users WHERE email = $1`,
      [email.toLowerCase()]
    );

    if (userResult.rows.length === 0) {
      // Não revelar se email existe ou não (segurança)
      return res.json({
        status: 'success',
        message: 'Se o email existir, um link de verificação foi enviado',
      });
    }

    const user = userResult.rows[0];

    if (user.email_verified) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Email já verificado',
      });
    }

    const token = await AuthService.createEmailVerificationToken(user.id);
    await EmailService.sendVerificationEmail(user.email, token);

    res.json({
      status: 'success',
      message: 'Email de verificação reenviado',
    });
  } catch (error) {
    console.error('Erro ao reenviar verificação:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao reenviar email de verificação',
    });
  }
});

/**
 * POST /api/auth/forgot-password
 * Solicitar reset de senha
 */
router.post('/forgot-password', passwordResetLimiter, async (req: Request, res: Response) => {
  try {
    const { email }: ForgotPasswordRequest = req.body;

    if (!email) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Email é obrigatório',
      });
    }

    const userResult = await query(
      `SELECT id, email FROM users WHERE email = $1`,
      [email.toLowerCase()]
    );

    if (userResult.rows.length === 0) {
      // Não revelar se email existe ou não (segurança)
      return res.json({
        status: 'success',
        message: 'Se o email existir, um link de reset foi enviado',
      });
    }

    const user = userResult.rows[0];
    const token = await AuthService.createPasswordResetToken(user.id);
    await EmailService.sendPasswordResetEmail(user.email, token);

    res.json({
      status: 'success',
      message: 'Se o email existir, um link de reset foi enviado',
    });
  } catch (error) {
    console.error('Erro ao solicitar reset de senha:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao solicitar reset de senha',
    });
  }
});

/**
 * POST /api/auth/reset-password
 * Redefinir senha com token
 */
router.post('/reset-password', async (req: Request, res: Response) => {
  try {
    const { token, new_password }: ResetPasswordRequest = req.body;

    if (!token || !new_password) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Token e nova senha são obrigatórios',
      });
    }

    if (new_password.length < 8) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Senha deve ter no mínimo 8 caracteres',
      });
    }

    const resetToken = await AuthService.validatePasswordResetToken(token);
    if (!resetToken) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Token inválido ou expirado',
      });
    }

    const passwordHash = await AuthService.hashPassword(new_password);

    await transaction(async (client) => {
      // Atualizar senha
      await client.query(
        `UPDATE users SET password_hash = $1, failed_login_attempts = 0, locked_until = NULL WHERE id = $2`,
        [passwordHash, resetToken.user_id]
      );

      // Marcar token como usado
      await AuthService.markPasswordResetTokenAsUsed(token);

      // Revogar todos os refresh tokens (forçar logout de todos os dispositivos)
      await AuthService.revokeAllUserTokens(resetToken.user_id);
    });

    res.json({
      status: 'success',
      message: 'Senha redefinida com sucesso',
    });
  } catch (error) {
    console.error('Erro ao redefinir senha:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao redefinir senha',
    });
  }
});

/**
 * POST /api/auth/change-password
 * Alterar senha (autenticado)
 */
router.post('/change-password', authenticate, async (req: Request, res: Response) => {
  try {
    const { current_password, new_password }: ChangePasswordRequest = req.body;

    if (!current_password || !new_password) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Senha atual e nova senha são obrigatórias',
      });
    }

    if (new_password.length < 8) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Nova senha deve ter no mínimo 8 caracteres',
      });
    }

    if (!req.userId) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Não autenticado',
      });
    }

    // Buscar usuário
    const userResult = await query(
      `SELECT password_hash FROM users WHERE id = $1`,
      [req.userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        status: 'error',
        code: 404,
        message: 'Usuário não encontrado',
      });
    }

    const user = userResult.rows[0];

    // Verificar senha atual
    if (!user.password_hash || !(await AuthService.verifyPassword(current_password, user.password_hash))) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Senha atual incorreta',
      });
    }

    // Atualizar senha
    const passwordHash = await AuthService.hashPassword(new_password);
    await query(
      `UPDATE users SET password_hash = $1 WHERE id = $2`,
      [passwordHash, req.userId]
    );

    // Revogar todos os refresh tokens (forçar logout de todos os dispositivos)
    await AuthService.revokeAllUserTokens(req.userId);

    res.json({
      status: 'success',
      message: 'Senha alterada com sucesso',
    });
  } catch (error) {
    console.error('Erro ao alterar senha:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao alterar senha',
    });
  }
});

/**
 * POST /api/auth/2fa/enable
 * Habilitar 2FA
 */
router.post('/2fa/enable', authenticate, async (req: Request, res: Response) => {
  try {
    const { method, phone }: Enable2FARequest = req.body;

    if (!method || !['sms', 'email', 'authenticator'].includes(method)) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Método 2FA inválido',
      });
    }

    if (method === 'sms' && !phone) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Telefone é obrigatório para SMS',
      });
    }

    if (!req.userId) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Não autenticado',
      });
    }

    // Buscar email do usuário
    const userResult = await query(
      `SELECT email FROM users WHERE id = $1`,
      [req.userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        status: 'error',
        code: 404,
        message: 'Usuário não encontrado',
      });
    }

    const userEmail = userResult.rows[0].email;

    // Habilitar 2FA
    const setup = await TwoFactorService.enable2FA(req.userId, method, undefined, phone);

    // Se for SMS ou Email, enviar código
    if (method === 'sms' && phone) {
      await TwoFactorService.sendSMSCode(req.userId, phone);
    } else if (method === 'email') {
      await TwoFactorService.sendEmailCode(req.userId, userEmail);
    }

    const response: TwoFactorSetupResponse = {
      qr_code_url: setup.qr_code_url,
      backup_codes: setup.backup_codes,
      secret: setup.secret,
    };

    res.json({
      status: 'success',
      data: response,
    });
  } catch (error) {
    console.error('Erro ao habilitar 2FA:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao habilitar 2FA',
    });
  }
});

/**
 * POST /api/auth/2fa/verify
 * Verificar código 2FA
 */
router.post('/2fa/verify', authenticate, async (req: Request, res: Response) => {
  try {
    const { code }: Verify2FARequest = req.body;

    if (!code) {
      return res.status(400).json({
        status: 'error',
        code: 400,
        message: 'Código é obrigatório',
      });
    }

    if (!req.userId) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Não autenticado',
      });
    }

    const isValid = await TwoFactorService.verify2FA(req.userId, code);

    if (!isValid) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Código 2FA inválido',
      });
    }

    res.json({
      status: 'success',
      message: 'Código 2FA verificado com sucesso',
    });
  } catch (error) {
    console.error('Erro ao verificar 2FA:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao verificar código 2FA',
    });
  }
});

/**
 * POST /api/auth/2fa/disable
 * Desabilitar 2FA
 */
router.post('/2fa/disable', authenticate, async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Não autenticado',
      });
    }

    await TwoFactorService.disable2FA(req.userId);

    res.json({
      status: 'success',
      message: '2FA desabilitado com sucesso',
    });
  } catch (error) {
    console.error('Erro ao desabilitar 2FA:', error);
    res.status(500).json({
      status: 'error',
      code: 500,
      message: 'Erro ao desabilitar 2FA',
    });
  }
});

export default router;
