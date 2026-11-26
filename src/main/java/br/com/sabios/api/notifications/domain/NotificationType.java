package br.com.sabios.api.notifications.domain;

public enum NotificationType {
    MENTORING_REQUESTED,
    MENTORING_ACCEPTED,
    MENTORING_REJECTED,
    MENTORING_CANCELED,
    /** O conselheiro atribuiu uma tarefa ao cliente no plano de ação. */
    ACTION_PLAN_ASSIGNED,
    /** O cliente avaliou uma mentoria do conselheiro. */
    REVIEW_RECEIVED
}
