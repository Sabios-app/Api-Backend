package br.com.sabios.api.agenda.service;

import br.com.sabios.api.agenda.domain.Availability;
import br.com.sabios.api.agenda.dto.AvailabilityResponse;
import br.com.sabios.api.agenda.dto.CreateAvailabilityRequest;
import br.com.sabios.api.agenda.repository.AvailabilityRepository;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import br.com.sabios.api.shared.exception.BadRequestException;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AgendaService {

    private static final int MAX_SLOT_RANGE_DAYS = 31;

    private final AvailabilityRepository availabilityRepository;
    private final SpecialistRepository specialistRepository;
    private final MentoringRepository mentoringRepository;
    private final SchedulingPolicy schedulingPolicy;

    public AgendaService(
            AvailabilityRepository availabilityRepository,
            SpecialistRepository specialistRepository,
            MentoringRepository mentoringRepository,
            SchedulingPolicy schedulingPolicy
    ) {
        this.availabilityRepository = availabilityRepository;
        this.specialistRepository = specialistRepository;
        this.mentoringRepository = mentoringRepository;
        this.schedulingPolicy = schedulingPolicy;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getSpecialistAvailability(UUID specialistId) {
        return availabilityRepository.findBySpecialistIdOrderByStartTimeAsc(specialistId)
                .stream()
                .sorted(Comparator.comparing(Availability::getDayOfWeek).thenComparing(Availability::getStartTime))
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

        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("O horário de término deve ser após o horário de início.");
        }

        boolean overlaps = availabilityRepository
                .findBySpecialistIdAndDayOfWeek(specialist.getId(), request.dayOfWeek())
                .stream()
                .anyMatch(existing -> request.startTime().isBefore(existing.getEndTime())
                        && existing.getStartTime().isBefore(request.endTime()));
        if (overlaps) {
            throw new IllegalArgumentException("Já existe um horário cadastrado que conflita com o período informado.");
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

    /**
     * Horários livres para agendamento: início de cada sessão dentro das faixas semanais do conselheiro,
     * descontando mentorias já confirmadas e horários que já passaram.
     */
    @Transactional(readOnly = true)
    public List<OffsetDateTime> getFreeSlots(UUID specialistId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("A data final deve ser igual ou posterior à data inicial.");
        }
        if (from.plusDays(MAX_SLOT_RANGE_DAYS).isBefore(to)) {
            throw new BadRequestException("O período máximo de consulta é de " + MAX_SLOT_RANGE_DAYS + " dias.");
        }
        if (!specialistRepository.existsById(specialistId)) {
            throw new NoSuchElementException("Especialista não encontrado com o ID informado.");
        }

        Duration duration = schedulingPolicy.sessionDuration();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime rangeStart = from.atStartOfDay(schedulingPolicy.zone()).toOffsetDateTime();
        OffsetDateTime rangeEnd = to.plusDays(1).atStartOfDay(schedulingPolicy.zone()).toOffsetDateTime();

        List<OffsetDateTime> booked = mentoringRepository.findBookedStarts(
                specialistId,
                List.of(MentoringStatus.CONFIRMED),
                rangeStart.minus(duration),
                rangeEnd.plus(duration)
        );

        List<OffsetDateTime> slots = new ArrayList<>();
        boolean hasAnyAvailability = !availabilityRepository.findBySpecialistIdOrderByStartTimeAsc(specialistId).isEmpty();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<Availability> windows = hasAnyAvailability
                    ? availabilityRepository.findBySpecialistIdAndDayOfWeek(specialistId, day.getDayOfWeek())
                    .stream()
                    .sorted(Comparator.comparing(Availability::getStartTime))
                    .toList()
                    : switch (day.getDayOfWeek()) {
                case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> List.of(
                        Availability.builder()
                                .dayOfWeek(day.getDayOfWeek())
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(18, 0))
                                .build()
                );
                default -> List.of();
            };

            for (Availability window : windows) {
                LocalTime start = window.getStartTime();
                while (!start.plus(duration).isAfter(window.getEndTime()) && start.plus(duration).isAfter(start)) {
                    OffsetDateTime slot = day.atTime(start).atZone(schedulingPolicy.zone()).toOffsetDateTime();
                    boolean taken = booked.stream().anyMatch(b -> Math.abs(Duration.between(b, slot).toSeconds()) < duration.toSeconds());
                    if (slot.isAfter(now) && !taken) {
                        slots.add(slot);
                    }
                    start = start.plus(duration);
                }
            }
        }
        return slots;
    }
}
