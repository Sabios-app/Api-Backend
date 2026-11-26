package br.com.sabios.api.reviews.repository;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.reviews.domain.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"client", "mentoring"})
    List<Review> findBySpecialistIdOrderByCreatedAtDesc(UUID specialistId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "mentoring"})
    List<Review> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    @Override
    @EntityGraph(attributePaths = {"client", "mentoring", "specialist", "specialist.user"})
    Optional<Review> findById(UUID id);

    boolean existsByMentoringId(UUID mentoringId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.specialist.id = :specialistId")
    Double calculateAverageRatingBySpecialistId(@Param("specialistId") UUID specialistId);

    /** Pares (nota, quantidade) das avaliações de um especialista. */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.specialist.id = :specialistId GROUP BY r.rating")
    List<Object[]> countByRating(@Param("specialistId") UUID specialistId);

    /** Mentorias concluídas do cliente que ainda não foram avaliadas, da mais recente para a mais antiga. */
    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    @Query("""
        SELECT m FROM Mentoring m
        WHERE m.client.id = :clientId
          AND m.status = br.com.sabios.api.mentoring.domain.MentoringStatus.COMPLETED
          AND NOT EXISTS (SELECT r.id FROM Review r WHERE r.mentoring = m)
        ORDER BY m.scheduledAt DESC
    """)
    List<Mentoring> findPendingReviews(@Param("clientId") UUID clientId);
}
