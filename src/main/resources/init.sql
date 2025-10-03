-- Script d'initialisation de la base de données PostgreSQL pour order_service

-- Créer la base de données (à exécuter en tant que superuser)
-- CREATE DATABASE brokerx_orders;
-- GRANT ALL PRIVILEGES ON DATABASE brokerx_orders TO postgres;

-- Tables créées automatiquement par Hibernate, mais voici la structure de référence:

/*
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    client_order_id VARCHAR(50),
    user_id BIGINT NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    type VARCHAR(10) NOT NULL CHECK (type IN ('MARKET', 'LIMIT')),
    time_in_force VARCHAR(10) NOT NULL CHECK (time_in_force IN ('DAY', 'IOC', 'FOK', 'GTC')),
    quantity NUMERIC(19,6) NOT NULL,
    price NUMERIC(19,6),
    filled_quantity NUMERIC(19,6) DEFAULT 0,
    avg_fill_price NUMERIC(19,6),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'EXPIRED')),
    reject_reason VARCHAR(500),
    reserved_amount NUMERIC(19,6) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_order_id ON orders(order_id);
CREATE UNIQUE INDEX idx_client_order_user ON orders(client_order_id, user_id) WHERE client_order_id IS NOT NULL;
CREATE INDEX idx_user_id ON orders(user_id);
CREATE INDEX idx_symbol ON orders(symbol);
CREATE INDEX idx_status ON orders(status);
CREATE INDEX idx_created_at ON orders(created_at);

-- Contraintes additionnelles
ALTER TABLE orders ADD CONSTRAINT chk_quantity_positive CHECK (quantity > 0);
ALTER TABLE orders ADD CONSTRAINT chk_price_positive CHECK (price IS NULL OR price > 0);
ALTER TABLE orders ADD CONSTRAINT chk_filled_quantity_valid CHECK (filled_quantity >= 0 AND filled_quantity <= quantity);
ALTER TABLE orders ADD CONSTRAINT chk_limit_order_has_price CHECK (type != 'LIMIT' OR price IS NOT NULL);
*/

-- Données de test (optionnel)
/*
INSERT INTO orders (order_id, user_id, symbol, side, type, time_in_force, quantity, price, status, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 1, 'AAPL', 'BUY', 'LIMIT', 'DAY', 100, 150.00, 'ACCEPTED', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', 1, 'GOOGL', 'SELL', 'MARKET', 'IOC', 50, NULL, 'FILLED', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440003', 2, 'TSLA', 'BUY', 'LIMIT', 'GTC', 25, 800.00, 'PENDING', NOW(), NOW());
*/