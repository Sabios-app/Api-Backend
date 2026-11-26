-- V4__password_reset_and_scheduling.sql
-- Recuperação de senha e suporte às regras de agendamento (agenda x mentorias)

-- 1. Tokens de redefinição de senha (somente o hash SHA-256 do token é armazenado)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user ON password_reset_tokens(user_id);

-- 2. Padroniza availabilities.day_of_week para os nomes do enum java.time.DayOfWeek
UPDATE availabilities SET day_of_week = CASE
    WHEN lower(day_of_week) LIKE 'seg%' THEN 'MONDAY'
    WHEN lower(day_of_week) LIKE 'ter%' THEN 'TUESDAY'
    WHEN lower(day_of_week) LIKE 'qua%' THEN 'WEDNESDAY'
    WHEN lower(day_of_week) LIKE 'qui%' THEN 'THURSDAY'
    WHEN lower(day_of_week) LIKE 'sex%' THEN 'FRIDAY'
    WHEN lower(day_of_week) LIKE 's_b%' THEN 'SATURDAY'
    WHEN lower(day_of_week) LIKE 'dom%' THEN 'SUNDAY'
    ELSE upper(day_of_week)
END;

-- Horários com dia irreconhecível não podem ser lidos pelo enum; o conselheiro deve recadastrá-los.
DELETE FROM availabilities
WHERE day_of_week NOT IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY');

-- 3. Índice para consultas de conflito de horário
CREATE INDEX IF NOT EXISTS idx_mentorings_specialist_scheduled ON mentorings(specialist_id, scheduled_at);
