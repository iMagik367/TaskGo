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

// WebSocket Server (inicializar mesmo se banco falhar)
let wsServer: WebSocketServer;
try {
  wsServer = new WebSocketServer(httpServer);
} catch (error) {
  console.warn('⚠️ Aviso: WebSocket server não pôde ser inicializado:', error);
}

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Health check (deve funcionar sempre, mesmo sem banco)
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    database: 'checking...'
  });
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
  try {
    if (wsServer) await wsServer.close();
    await pool.end();
  } catch (error) {
    console.error('Erro ao encerrar:', error);
  }
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('SIGINT recebido, encerrando servidor...');
  try {
    if (wsServer) await wsServer.close();
    await pool.end();
  } catch (error) {
    console.error('Erro ao encerrar:', error);
  }
  process.exit(0);
});

const PORT = process.env.PORT || 3000;

// Iniciar servidor mesmo se o banco não estiver disponível
httpServer.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Servidor rodando na porta ${PORT}`);
  console.log(`📡 WebSocket server ativo`);
  console.log(`🌐 Ambiente: ${process.env.NODE_ENV || 'development'}`);
  console.log(`🔗 Health check: http://0.0.0.0:${PORT}/health`);
  
  // Testar conexão com banco (não bloquear se falhar)
  pool.query('SELECT 1')
    .then(() => console.log('✅ Conexão com PostgreSQL verificada'))
    .catch((err) => console.warn('⚠️ Aviso: Não foi possível conectar ao PostgreSQL:', err.message));
});

export default app;
