-- Migration: 003_seed_categories
-- Description: Popular categorias de serviços e produtos
-- Created: 2024-01-01

-- Categorias de Serviços
INSERT INTO categories (name, type) VALUES
('Pintura', 'service'),
('Elétrica', 'service'),
('Encanamento', 'service'),
('Montagem', 'service'),
('Limpeza', 'service'),
('Jardinagem', 'service'),
('Marcenaria', 'service'),
('Alvenaria', 'service'),
('Reforma', 'service'),
('Instalação', 'service'),
('Manutenção', 'service'),
('Design', 'service'),
('Fotografia', 'service'),
('Vídeo', 'service'),
('Edição', 'service'),
('Consultoria', 'service'),
('Aulas', 'service'),
('Transporte', 'service'),
('Entrega', 'service'),
('Outros', 'service')
ON CONFLICT (name) DO NOTHING;

-- Categorias de Produtos
INSERT INTO categories (name, type) VALUES
('Eletrônicos', 'product'),
('Roupas', 'product'),
('Casa e Decoração', 'product'),
('Beleza e Cuidados', 'product'),
('Esportes', 'product'),
('Livros', 'product'),
('Brinquedos', 'product'),
('Alimentos', 'product'),
('Bebidas', 'product'),
('Ferramentas', 'product'),
('Móveis', 'product'),
('Automotivo', 'product'),
('Outros', 'product')
ON CONFLICT (name) DO NOTHING;
