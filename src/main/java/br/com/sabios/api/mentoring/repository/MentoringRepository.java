package br.com.sabios.api.mentoring.repository;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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

    @Override
    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    Optional<Mentoring> findById(UUID id);
}
