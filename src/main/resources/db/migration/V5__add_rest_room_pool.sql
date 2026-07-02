-- V5__add_rest_room_pool.sql
-- Add rest_room_pool column to game table to accumulate taxes

ALTER TABLE game ADD COLUMN IF NOT EXISTS rest_room_pool INTEGER NOT NULL DEFAULT 0;
