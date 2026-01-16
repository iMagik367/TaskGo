import * as functions from 'firebase-functions';
import {AppError} from '../utils/errors';

/**
 * Roles válidos no sistema
 */
export const VALID_ROLES = ['user', 'admin', 'moderator', 'partner', 'seller', 'provider', 'client'] as const;

export type UserRole = typeof VALID_ROLES[number];

/**
 * Verifica se um role é válido
 */
export const isValidRole = (role: string): role is UserRole => {
  return VALID_ROLES.includes(role as UserRole);
};

/**
 * Obtém o role do usuário através de Custom Claims
 * Custom Claims são a autoridade única para permissões
 */
export const getUserRole = (context: functions.https.CallableContext): UserRole => {
  if (!context.auth) {
    throw new AppError('unauthenticated', 'User must be authenticated', 401);
  }

  // Custom Claims estão em context.auth.token
  const role = context.auth.token.role as string | undefined;

  // Se não houver role em Custom Claims, verificar no documento do usuário
  // (apenas para migração - em produção, sempre deve ter Custom Claim)
  if (!role) {
    functions.logger.warn(`User ${context.auth.uid} has no role in Custom Claims`, {
      uid: context.auth.uid,
      timestamp: new Date().toISOString(),
    });

    // Fallback temporário para migração - retornar 'user' como padrão
    // Em produção, isso nunca deve acontecer
    return 'user';
  }

  if (!isValidRole(role)) {
    throw new AppError(
      'permission-denied',
      `Invalid role: ${role}. Must be one of: ${VALID_ROLES.join(', ')}`,
      403,
    );
  }

  return role;
};

/**
 * Verifica se o usuário tem um role específico
 */
export const hasRole = (
  context: functions.https.CallableContext,
  requiredRole: UserRole,
): boolean => {
  try {
    const userRole = getUserRole(context);
    return userRole === requiredRole;
  } catch {
    return false;
  }
};

/**
 * Verifica se o usuário é admin
 */
export const isAdmin = (context: functions.https.CallableContext): boolean => {
  return hasRole(context, 'admin');
};

/**
 * Verifica se o usuário é moderador ou admin
 */
export const isModeratorOrAdmin = (
  context: functions.https.CallableContext,
): boolean => {
  const role = getUserRole(context);
  return role === 'admin' || role === 'moderator';
};

/**
 * Asserta que o usuário tem um role específico
 */
export const assertRole = (
  context: functions.https.CallableContext,
  requiredRole: UserRole,
): void => {
  const userRole = getUserRole(context);

  if (userRole !== requiredRole) {
    throw new AppError(
      'permission-denied',
      `Required role: ${requiredRole}. Current role: ${userRole}`,
      403,
    );
  }
};

/**
 * Asserta que o usuário é admin
 */
export const assertAdmin = (context: functions.https.CallableContext): void => {
  assertRole(context, 'admin');
};

/**
 * Asserta que o usuário é moderador ou admin
 */
export const assertModeratorOrAdmin = (
  context: functions.https.CallableContext,
): void => {
  const role = getUserRole(context);

  if (role !== 'admin' && role !== 'moderator') {
    throw new AppError(
      'permission-denied',
      'Moderator or admin access required',
      403,
    );
  }
};
