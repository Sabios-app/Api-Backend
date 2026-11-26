-- V6__action_plan_links_and_review_updates.sql
-- Plano de ação ligado à mentoria e ao conselheiro, prazo como data, histórico de conclusão
-- e controle de edição das avaliações

-- 1. Plano de ação: vínculo com mentoria/conselheiro, autor, prazo em data e momento da conclusão
ALTER TABLE action_plans
    ADD COLUMN IF NOT EXISTS mentoring_id UUID REFERENCES mentorings(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS specialist_id UUID REFERENCES specialists(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS created_by_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS due_date DATE,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;

-- Itens antigos foram criados pelo próprio cliente
UPDATE action_plans SET created_by_id = client_id WHERE created_by_id IS NULL;
UPDATE action_plans SET completed_at = updated_at WHERE completed = TRUE AND completed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_action_plans_mentoring ON action_plans(mentoring_id);
CREATE INDEX IF NOT EXISTS idx_action_plans_specialist_creator ON action_plans(specialist_id, created_by_id);

-- 2. Avaliações: data da última edição
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
UPDATE reviews SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE reviews ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE reviews ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_client ON reviews(client_id);
