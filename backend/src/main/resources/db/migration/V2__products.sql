CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    category TEXT NOT NULL,
    banner_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed inicial de produtos de exemplo
INSERT INTO products (name, description, price, category, banner_url)
VALUES
('Guarda Roupa 6 Portas', 'Guarda roupa com espelho, MDF.', 899.90, 'Móveis', NULL),
('Furadeira sem fio 18V', 'Com 2 baterias.', 299.90, 'Ferramentas', NULL),
('Forno de Embutir 30L', 'Elétrico 30L, grill.', 599.90, 'Eletrodomésticos', NULL),
('Martelo 500g', 'Cabo de madeira.', 45.90, 'Ferramentas', NULL)
ON CONFLICT DO NOTHING;


