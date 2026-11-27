-- V8__seed_conselheiros_ficticios.sql
-- Restauração dos conselheiros reais do protótipo (Helena Martins, Carlos Almeida, Roberto Salles)
-- e cliente Marcos Ribeiro com dados completos (agenda, mentorias, planos de ação e avaliações)

-- Senha padrão para todos os usuários de teste: "senha123"
-- Hash BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq

-- 1. Usuários Conselheiros e Cliente
INSERT INTO users (id, name, email, password_hash, role, phone, avatar_url, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Helena Martins', 'helena.martins@sabios.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'COUNSELOR', '79999990001', 'https://images.unsplash.com/photo-1758691737605-69a0e78bd193?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000002', 'Carlos Almeida', 'carlos.almeida@sabios.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'COUNSELOR', '79999990002', 'https://images.unsplash.com/photo-1750853752220-0facda334bd7?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000003', 'Roberto Salles', 'roberto.salles@sabios.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'COUNSELOR', '79999990003', 'https://images.unsplash.com/photo-1624245532111-8f4aba352ba9?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000001', 'Marcos Ribeiro', 'marcos.ribeiro@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAf270Cq', 'CLIENT', '79988881111', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO UPDATE SET
    name = EXCLUDED.name,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    avatar_url = EXCLUDED.avatar_url;

-- 2. Perfis de Especialistas
INSERT INTO specialists (id, user_id, title, specialty, bio, rating, hourly_rate, is_available, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Ex-diretora financeira com 32 anos de experiência', 'Finanças & Gestão', 'Fui diretora financeira por mais de três décadas. Hoje ajudo pequenos negócios a organizar o caixa, definir preços e crescer sem sustos.', 4.90, 180.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Liderou times comerciais por mais de 28 anos', 'Vendas & Estratégia', 'Passei a vida montando e treinando equipes de vendas. Gosto de ajudar quem está começando a vender mais, sem fórmulas mágicas.', 4.80, 160.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Conselheiro e ex-VP de pessoas em empresas nacionais', 'Liderança & Pessoas', 'Cuidei de pessoas e de cultura em empresas grandes. Agora ajudo donos de negócio a contratar bem, liderar e manter a equipe motivada.', 5.00, 200.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO UPDATE SET
    title = EXCLUDED.title,
    specialty = EXCLUDED.specialty,
    bio = EXCLUDED.bio,
    rating = EXCLUDED.rating,
    hourly_rate = EXCLUDED.hourly_rate,
    is_available = EXCLUDED.is_available;

-- 3. Grade Semanal de Disponibilidade (Agenda)
DELETE FROM availabilities WHERE specialist_id IN (
    'c0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000003'
);

INSERT INTO availabilities (specialist_id, day_of_week, start_time, end_time)
VALUES
    -- Helena Martins (Segunda, Terça e Quinta)
    ('c0000000-0000-0000-0000-000000000001', 'MONDAY', '09:00:00', '12:00:00'),
    ('c0000000-0000-0000-0000-000000000001', 'MONDAY', '14:00:00', '18:00:00'),
    ('c0000000-0000-0000-0000-000000000001', 'TUESDAY', '09:00:00', '12:00:00'),
    ('c0000000-0000-0000-0000-000000000001', 'TUESDAY', '14:00:00', '18:00:00'),
    ('c0000000-0000-0000-0000-000000000001', 'THURSDAY', '09:00:00', '12:00:00'),
    ('c0000000-0000-0000-0000-000000000001', 'THURSDAY', '14:00:00', '18:00:00'),
    -- Carlos Almeida (Segunda, Quarta e Sexta)
    ('c0000000-0000-0000-0000-000000000002', 'MONDAY', '14:00:00', '18:00:00'),
    ('c0000000-0000-0000-0000-000000000002', 'WEDNESDAY', '09:00:00', '12:00:00'),
    ('c0000000-0000-0000-0000-000000000002', 'WEDNESDAY', '14:00:00', '18:00:00'),
    ('c0000000-0000-0000-0000-000000000002', 'FRIDAY', '09:00:00', '13:00:00'),
    -- Roberto Salles (Terça, Quinta e Sexta)
    ('c0000000-0000-0000-0000-000000000003', 'TUESDAY', '09:00:00', '12:00:00'),
    ('c0000000-0000-0000-0000-000000000003', 'TUESDAY', '14:00:00', '17:00:00'),
    ('c0000000-0000-0000-0000-000000000003', 'THURSDAY', '09:00:00', '12:00:00'),
    ('c0000000-0000-0000-0000-000000000003', 'THURSDAY', '14:00:00', '17:00:00'),
    ('c0000000-0000-0000-0000-000000000003', 'FRIDAY', '09:00:00', '12:00:00');

-- 4. Planos de Ação do Cliente Marcos Ribeiro
INSERT INTO action_plans (id, client_id, specialist_id, created_by_id, conselheiro_nome, title, deadline, due_date, completed, created_at, updated_at)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Helena Martins', 'Separar conta bancária da pessoa física da jurídica', 'Concluído', CURRENT_DATE - 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Helena Martins', 'Planilhar custo unitário dos 5 pães com maior saída', 'Até 15 dias', CURRENT_DATE + 15, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Carlos Almeida', 'Criar roteiro simples de vendas para atendentes do balcão', 'Até 22 dias', CURRENT_DATE + 22, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 5. Mentoria Confirmada com Link de Chamada
INSERT INTO mentorings (id, client_id, specialist_id, title, description, doubts, status, scheduled_at, meeting_link, created_at, updated_at)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Fluxo de caixa e planejamento de compras para o fim de ano', 'Mentoria para organizar a compra de insumos e separar finanças pessoais da padaria.', 'Qual percentual máximo devo gastar com farinha e insumos?', 'CONFIRMED', CURRENT_TIMESTAMP + INTERVAL '2 days', 'https://meet.google.com/sabios-mentoria-demo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
