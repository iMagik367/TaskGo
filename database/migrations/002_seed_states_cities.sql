-- Migration: 002_seed_states_cities
-- Description: Popular estados e cidades brasileiras (IBGE)
-- Created: 2024-01-01

-- Estados brasileiros
INSERT INTO states (code, name) VALUES
('AC', 'Acre'),
('AL', 'Alagoas'),
('AP', 'Amapá'),
('AM', 'Amazonas'),
('BA', 'Bahia'),
('CE', 'Ceará'),
('DF', 'Distrito Federal'),
('ES', 'Espírito Santo'),
('GO', 'Goiás'),
('MA', 'Maranhão'),
('MT', 'Mato Grosso'),
('MS', 'Mato Grosso do Sul'),
('MG', 'Minas Gerais'),
('PA', 'Pará'),
('PB', 'Paraíba'),
('PR', 'Paraná'),
('PE', 'Pernambuco'),
('PI', 'Piauí'),
('RJ', 'Rio de Janeiro'),
('RN', 'Rio Grande do Norte'),
('RS', 'Rio Grande do Sul'),
('RO', 'Rondônia'),
('RR', 'Roraima'),
('SC', 'Santa Catarina'),
('SP', 'São Paulo'),
('SE', 'Sergipe'),
('TO', 'Tocantins')
ON CONFLICT (code) DO NOTHING;

-- Nota: Para popular cidades, usar dados do IBGE
-- Exemplo de algumas cidades principais (completar com dados completos do IBGE)
-- Em produção, usar script Python/Node.js para popular todas as cidades

-- Exemplo para São Paulo
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'São Paulo', 'sao paulo', -23.5505, -46.6333
FROM states s WHERE s.code = 'SP'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Rio de Janeiro
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Rio de Janeiro', 'rio de janeiro', -22.9068, -43.1729
FROM states s WHERE s.code = 'RJ'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Belo Horizonte
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Belo Horizonte', 'belo horizonte', -19.9167, -43.9345
FROM states s WHERE s.code = 'MG'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Curitiba
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Curitiba', 'curitiba', -25.4284, -49.2733
FROM states s WHERE s.code = 'PR'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Porto Alegre
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Porto Alegre', 'porto alegre', -30.0346, -51.2177
FROM states s WHERE s.code = 'RS'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Brasília
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Brasília', 'brasilia', -15.7942, -47.8822
FROM states s WHERE s.code = 'DF'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Salvador
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Salvador', 'salvador', -12.9714, -38.5014
FROM states s WHERE s.code = 'BA'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Fortaleza
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Fortaleza', 'fortaleza', -3.7172, -38.5433
FROM states s WHERE s.code = 'CE'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Recife
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Recife', 'recife', -8.0476, -34.8770
FROM states s WHERE s.code = 'PE'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- Exemplo para Osasco (SP)
INSERT INTO cities (state_id, name, normalized_name, latitude, longitude)
SELECT s.id, 'Osasco', 'osasco', -23.5329, -46.7915
FROM states s WHERE s.code = 'SP'
ON CONFLICT (state_id, normalized_name) DO NOTHING;

-- TODO: Popular todas as cidades do IBGE usando script separado
-- Script recomendado: scripts/populate_cities_from_ibge.js
