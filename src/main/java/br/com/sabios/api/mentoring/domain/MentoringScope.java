package br.com.sabios.api.mentoring.domain;

/** Recorte usado na listagem do painel. */
public enum MentoringScope {
    /** Solicitadas ou confirmadas com data futura, da mais próxima para a mais distante. */
    UPCOMING,
    /** Concluídas, recusadas, canceladas ou já passadas, da mais recente para a mais antiga. */
    HISTORY
}
