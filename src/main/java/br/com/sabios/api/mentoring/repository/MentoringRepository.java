package br.com.sabios.api.mentoring.repository;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentoringRepository extends JpaRepository<Mentoring, UUID> {

    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    List<Mentoring> findByClientIdOrderByScheduledAtDesc(UUID clientId);

    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    List<Mentoring> findBySpecialistIdOrderByScheduledAtDesc(UUID specialistId);

    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    @Query("""
        SELECT m FROM Mentoring m
        WHERE (m.client.id = :userId OR m.specialist.user.id = :userId)
          AND m.status = :status
          AND m.scheduledAt >= :now
        ORDER BY m.scheduledAt ASC
        LIMIT 1
    """)
    Optional<Mentoring> findNextConfirmedMentoring(
            @Param("userId") UUID userId,
            @Param("status") MentoringStatus status,
            @Param("now") OffsetDateTime now
    );

    /**
     * Conta mentorias do especialista cujo início cai dentro da janela (exclusiva) informada.
     * Com sessões de duração fixa, dois horários colidem quando a distância entre inícios é menor que a duração.
     */
    @Query("""
        SELECT COUNT(m) FROM Mentoring m
        WHERE m.specialist.id = :specialistId
          AND m.status IN :statuses
          AND m.id <> :excludeId
          AND m.scheduledAt > :windowStart
          AND m.scheduledAt < :windowEnd
    """)
    long countSpecialistConflicts(
            @Param("specialistId") UUID specialistId,
            @Param("statuses") Collection<MentoringStatus> statuses,
            @Param("excludeId") UUID excludeId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );

    /** Mesma lógica de {@link #countSpecialistConflicts}, mas para o cliente (qualquer especialista). */
    @Query("""
        SELECT COUNT(m) FROM Mentoring m
        WHERE m.client.id = :clientId
          AND m.status IN :statuses
          AND m.id <> :excludeId
          AND m.scheduledAt > :windowStart
          AND m.scheduledAt < :windowEnd
    """)
    long countClientConflicts(
            @Param("clientId") UUID clientId,
            @Param("statuses") Collection<MentoringStatus> statuses,
            @Param("excludeId") UUID excludeId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );

    /** Inícios das mentorias já reservadas de um especialista em um período (usado para calcular horários livres). */
    @Query("""
        SELECT m.scheduledAt FROM Mentoring m
        WHERE m.specialist.id = :specialistId
          AND m.status IN :statuses
          AND m.scheduledAt >= :from
          AND m.scheduledAt < :to
    """)
    List<OffsetDateTime> findBookedStarts(
            @Param("specialistId") UUID specialistId,
            @Param("statuses") Collection<MentoringStatus> statuses,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    /** Marca em lote como EXPIRED as solicitações pendentes cujo horário já passou. */
    @Modifying
    @Query("""
        UPDATE Mentoring m
        SET m.status = :expired, m.updatedAt = :now
        WHERE m.status = :requested AND m.scheduledAt < :now
    """)
    int expireOverdue(
            @Param("requested") MentoringStatus requested,
            @Param("expired") MentoringStatus expired,
            @Param("now") OffsetDateTime now
    );

    @Override
    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    Optional<Mentoring> findById(UUID id);
}
