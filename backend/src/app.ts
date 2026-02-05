import express from 'express';
import { createServer } from 'http';
import cors from 'cors';
import { WebSocketServer } from './websocket/server';
import { pool } from './database/connection';

// Importar rotas
import userRoutes from './routes/users';
import locationRoutes from './routes/location';
import productRoutes from './routes/products';
import orderRoutes from './routes/orders';
import notificationRoutes from './routes/notifications';
import stripeRoutes from './routes/stripe';
import trackingRoutes from './routes/tracking';

const app = express();
const httpServer = createServer(app);

// WebSocket Server
const wsServer = new WebSocketServer(httpServer);

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Rotas
app.use('/api/users', userRoutes);
app.use('/api/location', locationRoutes);
app.use('/api/products', productRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/stripe', stripeRoutes);
app.use('/api/tracking', trackingRoutes);

// Error handler
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Erro:', err);
  res.status(err.status || 500).json({
    error: err.message || 'Erro interno do servidor'
  });
});

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('SIGTERM recebido, encerrando servidor...');
  await wsServer.close();
  await pool.end();
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('SIGINT recebido, encerrando servidor...');
  await wsServer.close();
  await pool.end();
  process.exit(0);
});

const PORT = process.env.PORT || 3000;
httpServer.listen(PORT, () => {
  console.log(`🚀 Servidor rodando na porta ${PORT}`);
  console.log(`📡 WebSocket server ativo`);
});

export default app;
