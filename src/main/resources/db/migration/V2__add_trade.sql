-- V2__add_trade.sql
-- Migration to support Trading between Players

CREATE TABLE IF NOT EXISTS trade_offer (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    proposer_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    offered_cash INTEGER NOT NULL DEFAULT 0,
    requested_cash INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS trade_offer_property (
    trade_offer_id UUID NOT NULL,
    property_id INTEGER NOT NULL,
    is_offered BOOLEAN NOT NULL,
    PRIMARY KEY (trade_offer_id, property_id)
);
