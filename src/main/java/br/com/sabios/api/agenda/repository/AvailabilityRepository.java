package br.com.sabios.api.agenda.repository;

import br.com.sabios.api.agenda.domain.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {
    List<Availability> findBySpecialistIdOrderByStartTimeAsc(UUID specialistId);
}
