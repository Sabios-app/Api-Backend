package br.com.sabios.api.specialists.repository;

import br.com.sabios.api.specialists.domain.Specialist;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, UUID>, JpaSpecificationExecutor<Specialist> {

    @EntityGraph(attributePaths = {"user"})
    Optional<Specialist> findByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Optional<Specialist> findById(UUID id);

    /**
     * Carrega o especialista bloqueando a linha até o fim da transação. Serializa solicitações e aceites
     * concorrentes para o mesmo conselheiro, evitando reserva dupla do mesmo horário.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Specialist s WHERE s.id = :id")
    Optional<Specialist> findByIdForUpdate(@Param("id") UUID id);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Specialist> findAll(Specification<Specialist> spec, Pageable pageable);
}
