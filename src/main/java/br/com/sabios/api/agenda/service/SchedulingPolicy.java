package br.com.sabios.api.agenda.service;

import br.com.sabios.api.agenda.domain.Availability;
import br.com.sabios.api.agenda.repository.AvailabilityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Regras compartilhadas entre agenda e mentorias: fuso horário, duração padrão da sessão e
 * verificação se um horário cabe na disponibilidade semanal do conselheiro.
 *
 * <p>A mentoria guarda apenas o início ({@code scheduledAt}); a duração é única e configurável
 * ({@code app.scheduling.session-minutes}).
 */
@Component
public class SchedulingPolicy {

    private final AvailabilityRepository availabilityRepository;
    private final ZoneId zone;
    private final Duration sessionDuration;

    public SchedulingPolicy(
            AvailabilityRepository availabilityRepository,
            @Value("${app.scheduling.zone}") String zone,
            @Value("${app.scheduling.session-minutes}") long sessionMinutes
    ) {
        this.availabilityRepository = availabilityRepository;
        this.zone = ZoneId.of(zone);
        this.sessionDuration = Duration.ofMinutes(sessionMinutes);
    }

    public ZoneId zone() {
        return zone;
    }

    public Duration sessionDuration() {
        return sessionDuration;
    }

    /** O início e o fim da sessão precisam estar dentro de uma mesma faixa cadastrada para o dia. */
    public boolean fitsAvailability(UUID specialistId, OffsetDateTime start) {
        ZonedDateTime local = start.atZoneSameInstant(zone);
        LocalTime sessionStart = local.toLocalTime();
        LocalTime sessionEnd = sessionStart.plus(sessionDuration);

        // Sessão que atravessa a meia-noite nunca cabe em uma faixa diária.
        if (!sessionEnd.isAfter(sessionStart)) {
            return false;
        }

        for (Availability window : availabilityRepository.findBySpecialistIdAndDayOfWeek(specialistId, local.getDayOfWeek())) {
            if (!sessionStart.isBefore(window.getStartTime()) && !sessionEnd.isAfter(window.getEndTime())) {
                return true;
            }
        }
        return false;
    }
}
