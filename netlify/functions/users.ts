import { Handler, HandlerEvent, HandlerContext } from '@netlify/functions';
import { UserRepository } from '../../backend/src/repositories/UserRepository';

const userRepo = new UserRepository();

export const handler: Handler = async (event: HandlerEvent, context: HandlerContext) => {
  // CORS headers
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  };

  // Handle preflight
  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 200, headers, body: '' };
  }

  try {
    const { path, httpMethod, body } = event;
    const userId = path.split('/').pop();

    switch (httpMethod) {
      case 'GET':
        if (userId && userId !== 'users') {
          const user = await userRepo.findById(userId);
          if (!user) {
            return { statusCode: 404, headers, body: JSON.stringify({ error: 'Usuário não encontrado' }) };
          }
          return { statusCode: 200, headers, body: JSON.stringify(user) };
        }
        return { statusCode: 400, headers, body: JSON.stringify({ error: 'ID do usuário é obrigatório' }) };

      case 'POST':
        const newUser = await userRepo.create(JSON.parse(body || '{}'));
        return { statusCode: 201, headers, body: JSON.stringify(newUser) };

      case 'PUT':
        if (!userId || userId === 'users') {
          return { statusCode: 400, headers, body: JSON.stringify({ error: 'ID do usuário é obrigatório' }) };
        }
        const updatedUser = await userRepo.update(userId, JSON.parse(body || '{}'));
        return { statusCode: 200, headers, body: JSON.stringify(updatedUser) };

      default:
        return { statusCode: 405, headers, body: JSON.stringify({ error: 'Método não permitido' }) };
    }
  } catch (error: any) {
    console.error('Erro:', error);
    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({ error: error.message || 'Erro interno do servidor' }),
    };
  }
};
