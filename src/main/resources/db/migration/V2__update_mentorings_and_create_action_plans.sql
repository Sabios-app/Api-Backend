-- V2__update_mentorings_and_create_action_plans.sql
-- Ajuste da tabela mentorings e criação da tabela action_plans (Sem dados fictícios)

-- 1. Adicionar colunas necessárias na tabela mentorings
ALTER TABLE mentorings
    ADD COLUMN IF NOT EXISTS doubts TEXT,
    ADD COLUMN IF NOT EXISTS meeting_link VARCHAR(500);

-- 2. Tabela de Planos de Ação (Action Plans / Tasks)
CREATE TABLE IF NOT EXISTS action_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conselheiro_nome VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    deadline VARCHAR(100),
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_action_plans_client ON action_plans(client_id);
