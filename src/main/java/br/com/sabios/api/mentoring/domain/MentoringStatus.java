package br.com.sabios.api.mentoring.domain;

public enum MentoringStatus {
    CONFIRMED,
    REQUESTED,
    COMPLETED,
    REJECTED,
    CANCELED,
    /** Solicitação que ninguém respondeu até o horário agendado (definido automaticamente). */
    EXPIRED
}
