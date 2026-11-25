package br.com.sabios.api.specialists.repository;

import br.com.sabios.api.specialists.domain.Specialist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Optional<Specialist> findByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Optional<Specialist> findById(UUID id);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT s FROM Specialist s
        WHERE (:specialty IS NULL OR LOWER(s.specialty) LIKE LOWER(CONCAT('%', :specialty, '%')))
          AND (:search IS NULL OR (
                LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(s.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(s.specialty) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
          AND s.isAvailable = true
    """)
    Page<Specialist> findAvailableSpecialists(
            @Param("specialty") String specialty,
            @Param("search") String search,
            Pageable pageable
    );
}
