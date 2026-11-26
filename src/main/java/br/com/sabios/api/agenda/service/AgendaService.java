package br.com.sabios.api.agenda.service;

import br.com.sabios.api.agenda.domain.Availability;
import br.com.sabios.api.agenda.dto.AvailabilityResponse;
import br.com.sabios.api.agenda.dto.CreateAvailabilityRequest;
import br.com.sabios.api.agenda.repository.AvailabilityRepository;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AgendaService {

    private final AvailabilityRepository availabilityRepository;
    private final SpecialistRepository specialistRepository;

    public AgendaService(AvailabilityRepository availabilityRepository, SpecialistRepository specialistRepository) {
        this.availabilityRepository = availabilityRepository;
        this.specialistRepository = specialistRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getSpecialistAvailability(UUID specialistId) {
        return availabilityRepository.findBySpecialistIdOrderByStartTimeAsc(specialistId)
                .stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getMyAvailability(User user) {
        Specialist specialist = specialistRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("Perfil de especialista não encontrado."));
        return getSpecialistAvailability(specialist.getId());
    }

    @Transactional
    public AvailabilityResponse createAvailability(User user, CreateAvailabilityRequest request) {
        Specialist specialist = specialistRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Apenas conselheiros podem definir horários na agenda."));

        if (request.endTime().isBefore(request.startTime()) || request.endTime().equals(request.startTime())) {
            throw new IllegalArgumentException("O horário de término deve ser após o horário de início.");
        }

        Availability availability = Availability.builder()
                .specialist(specialist)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();

        Availability saved = availabilityRepository.save(availability);
        return AvailabilityResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteAvailability(User user, UUID id) {
        Availability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Horário não encontrado."));

        if (!availability.getSpecialist().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Você não tem autorização para remover este horário.");
        }

        availabilityRepository.delete(availability);
    }
}
