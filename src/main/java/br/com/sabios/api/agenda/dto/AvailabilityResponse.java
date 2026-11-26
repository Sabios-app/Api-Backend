package br.com.sabios.api.agenda.dto;

import br.com.sabios.api.agenda.domain.Availability;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static AvailabilityResponse fromEntity(Availability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime()
        );
    }
}
