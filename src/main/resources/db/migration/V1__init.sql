-- V1__init.sql
-- Initial Schema Migration for Vyapar Game with IF NOT EXISTS checks

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS game_room (
    id UUID PRIMARY KEY,
    room_code VARCHAR(6) NOT NULL UNIQUE,
    host_id UUID NOT NULL,
    max_players INTEGER NOT NULL,
    current_players INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS game (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    current_turn_player_id UUID,
    pending_action VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    winner_id UUID,
    has_rolled BOOLEAN,
    version BIGINT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP
);


CREATE TABLE IF NOT EXISTS player (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    username VARCHAR(30) NOT NULL,
    balance INTEGER NOT NULL,
    position INTEGER NOT NULL,
    number_of_properties INTEGER NOT NULL,
    consecutive_doubles INTEGER NOT NULL,
    jail_turns INTEGER NOT NULL,
    skipped_turns INTEGER NOT NULL,
    connected BOOLEAN NOT NULL,
    ready BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    has_built_house_this_turn BOOLEAN NOT NULL,
    version BIGINT
);


CREATE TABLE IF NOT EXISTS owned_property (
    game_id UUID NOT NULL,
    property_id INTEGER NOT NULL,
    owner_id UUID NOT NULL,
    development_level INTEGER NOT NULL,
    mortgaged BOOLEAN NOT NULL,
    version BIGINT,
    PRIMARY KEY (game_id, property_id)
);


CREATE TABLE IF NOT EXISTS transaction_history (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    player_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount INTEGER NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
