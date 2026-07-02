-- V3__add_has_built_house_this_turn.sql
-- Migration to add missing has_built_house_this_turn column to player table

ALTER TABLE player ADD COLUMN IF NOT EXISTS has_built_house_this_turn BOOLEAN NOT NULL DEFAULT FALSE;
