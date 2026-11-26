package br.com.sabios.api.reviews.repository;

import br.com.sabios.api.reviews.domain.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"client", "mentoring"})
    List<Review> findBySpecialistIdOrderByCreatedAtDesc(UUID specialistId);

    boolean existsByMentoringId(UUID mentoringId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.specialist.id = :specialistId")
    Double calculateAverageRatingBySpecialistId(@Param("specialistId") UUID specialistId);
}
