import { Request, Response, NextFunction } from 'express';
import { AuthService } from '../services/AuthService';
import { UUID } from '../types';

// Estender interface Request do Express para incluir userId
declare global {
  namespace Express {
    interface Request {
      userId?: UUID;
      userEmail?: string;
      userRole?: string;
    }
  }
}

/**
 * Middleware para autenticar requisições usando JWT
 */
export async function authenticate(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Token de autenticação não fornecido',
      });
      return;
    }

    const token = authHeader.substring(7); // Remover "Bearer "
    const decoded = AuthService.verifyToken(token);

    if (!decoded) {
      res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Token inválido ou expirado',
      });
      return;
    }

    // Adicionar informações do usuário à requisição
    req.userId = decoded.userId;
    req.userEmail = decoded.email;
    req.userRole = decoded.role;

    next();
  } catch (error) {
    console.error('Erro na autenticação:', error);
    res.status(401).json({
      status: 'error',
      code: 401,
      message: 'Erro ao autenticar requisição',
    });
  }
}

/**
 * Middleware opcional - não retorna erro se não houver token
 */
export async function optionalAuthenticate(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      const decoded = AuthService.verifyToken(token);

      if (decoded) {
        req.userId = decoded.userId;
        req.userEmail = decoded.email;
        req.userRole = decoded.role;
      }
    }

    next();
  } catch (error) {
    // Continuar mesmo com erro
    next();
  }
}

/**
 * Middleware para verificar se usuário tem role específica
 */
export function requireRole(...roles: string[]) {
  return (req: Request, res: Response, next: NextFunction): void => {
    if (!req.userRole) {
      res.status(401).json({
        status: 'error',
        code: 401,
        message: 'Autenticação necessária',
      });
      return;
    }

    if (!roles.includes(req.userRole)) {
      res.status(403).json({
        status: 'error',
        code: 403,
        message: 'Acesso negado. Permissão insuficiente.',
      });
      return;
    }

    next();
  };
}
