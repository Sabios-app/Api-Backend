-- V2__seed_initial_data.sql
-- Inserção de dados iniciais reais para o protótipo Sábios App (Web & Mobile)

-- Ajuste de estrutura para mentorings e action_plans caso ainda não existam
ALTER TABLE mentorings
    ADD COLUMN IF NOT EXISTS doubts TEXT,
    ADD COLUMN IF NOT EXISTS meeting_link VARCHAR(500);

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

-- 1. Inserção de Usuários Conselheiros (Senha padrão com hash BCrypt para "senha123": $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq)
INSERT INTO users (id, name, email, password_hash, role, phone, avatar_url)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Helena Martins', 'helena.martins@sabios.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'COUNSELOR', '79999990001', 'https://images.unsplash.com/photo-1758691737605-69a0e78bd193?auto=format&fit=crop&w=800&q=80'),
    ('a0000000-0000-0000-0000-000000000002', 'Carlos Almeida', 'carlos.almeida@sabios.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'COUNSELOR', '79999990002', 'https://images.unsplash.com/photo-1750853752220-0facda334bd7?auto=format&fit=crop&w=800&q=80'),
    ('a0000000-0000-0000-0000-000000000003', 'Roberto Salles', 'roberto.salles@sabios.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'COUNSELOR', '79999990003', 'https://images.unsplash.com/photo-1624245532111-8f4aba352ba9?auto=format&fit=crop&w=800&q=80'),
    ('b0000000-0000-0000-0000-000000000001', 'Marcos Ribeiro', 'marcos.ribeiro@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'CLIENT', '79988881111', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80')
ON CONFLICT (email) DO NOTHING;

-- 2. Inserção de Especialistas
INSERT INTO specialists (id, user_id, title, specialty, bio, rating, hourly_rate, is_available)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Ex-diretora financeira com 32 anos de experiência', 'Finanças & Gestão', 'Fui diretora financeira por mais de três décadas. Hoje ajudo pequenos negócios a organizar o caixa, definir preços e crescer sem sustos.', 4.90, 180.00, true),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Liderou times comerciais por mais de 28 anos', 'Vendas & Estratégia', 'Passei a vida montando e treinando equipes de vendas. Gosto de ajudar quem está começando a vender mais, sem fórmulas mágicas.', 4.80, 160.00, true),
    ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Conselheiro e ex-VP de pessoas em empresas nacionais', 'Liderança & Pessoas', 'Cuidei de pessoas e de cultura em empresas grandes. Agora ajudo donos de negócio a contratar bem, liderar e manter a equipe motivada.', 5.00, 200.00, true)
ON CONFLICT (user_id) DO NOTHING;

-- 3. Inserção de Planos de Ação padrão para o cliente Marcos Ribeiro
INSERT INTO action_plans (client_id, conselheiro_nome, title, deadline, completed)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Helena Martins', 'Separar conta bancária da pessoa física da jurídica', 'Concluído', true),
    ('b0000000-0000-0000-0000-000000000001', 'Helena Martins', 'Planilhar custo unitário dos 5 pães com maior saída', 'Até 15 de Nov', false),
    ('b0000000-0000-0000-0000-000000000001', 'Carlos Almeida', 'Criar roteiro simples de vendas para atendentes do balcão', 'Até 22 de Nov', false);

-- 4. Inserção de Mentoria confirmada (Próxima Sessão para a Home)
INSERT INTO mentorings (id, client_id, specialist_id, title, description, doubts, status, scheduled_at, meeting_link)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Fluxo de caixa e planejamento de compras para o fim de ano', 'Mentoria para organizar a compra de insumos e separar finanças pessoais da padaria.', 'Qual percentual máximo devo gastar com farinha e insumos?', 'CONFIRMED', CURRENT_TIMESTAMP + INTERVAL '2 days', 'https://meet.google.com/sabios-mentoria-demo');
