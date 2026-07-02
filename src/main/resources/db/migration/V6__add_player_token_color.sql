-- V6__add_player_token_color.sql
-- Add token_color column to player table to persist chosen badge/token colors

ALTER TABLE player ADD COLUMN IF NOT EXISTS token_color VARCHAR(30);
