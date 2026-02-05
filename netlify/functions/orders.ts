import { Handler, HandlerEvent, HandlerContext } from '@netlify/functions';
import { ServiceOrderRepository, PurchaseOrderRepository } from '../../backend/src/repositories/OrderRepository';

const serviceOrderRepo = new ServiceOrderRepository();
const purchaseOrderRepo = new PurchaseOrderRepository();

export const handler: Handler = async (event: HandlerEvent, context: HandlerContext) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
  };

  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 200, headers, body: '' };
  }

  try {
    const { httpMethod, body, path, queryStringParameters } = event;

    // Service Orders
    if (path.includes('/service')) {
      if (httpMethod === 'POST') {
        const order = await serviceOrderRepo.create(JSON.parse(body || '{}'));
        return { statusCode: 201, headers, body: JSON.stringify(order) };
      }

      if (httpMethod === 'GET' && path.includes('/city/') && path.includes('/category/')) {
        const cityId = parseInt(path.split('/city/')[1]?.split('/')[0] || '0');
        const category = path.split('/category/')[1] || '';
        const limit = parseInt(queryStringParameters?.limit || '50');

        const orders = await serviceOrderRepo.findByCityAndCategory(cityId, category, limit);
        return { statusCode: 200, headers, body: JSON.stringify(orders) };
      }

      if (httpMethod === 'POST' && path.includes('/accept')) {
        const orderId = path.split('/accept')[0].split('/').pop() || '';
        const { partnerId } = JSON.parse(body || '{}');
        const order = await serviceOrderRepo.acceptOrder(orderId, partnerId);
        return { statusCode: 200, headers, body: JSON.stringify(order) };
      }
    }

    // Purchase Orders
    if (path.includes('/purchase')) {
      if (httpMethod === 'POST') {
        const order = await purchaseOrderRepo.create(JSON.parse(body || '{}'));
        return { statusCode: 201, headers, body: JSON.stringify(order) };
      }

      if (httpMethod === 'GET') {
        const orderId = path.split('/purchase/')[1] || '';
        if (orderId) {
          const order = await purchaseOrderRepo.findById(orderId);
          if (!order) {
            return { statusCode: 404, headers, body: JSON.stringify({ error: 'Pedido não encontrado' }) };
          }
          return { statusCode: 200, headers, body: JSON.stringify(order) };
        }
      }
    }

    return { statusCode: 404, headers, body: JSON.stringify({ error: 'Rota não encontrada' }) };
  } catch (error: any) {
    console.error('Erro:', error);
    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({ error: error.message || 'Erro interno do servidor' }),
    };
  }
};
