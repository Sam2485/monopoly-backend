-- V4__remove_transaction_type_check.sql
-- Migration to drop the transaction_type check constraint to allow new types like TRADE

ALTER TABLE transaction_history DROP CONSTRAINT IF EXISTS transaction_history_transaction_type_check;
