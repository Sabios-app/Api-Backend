package br.com.sabios.api.specialists.dto;

import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.users.dto.UserResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SpecialistResponse(
        UUID id,
        UserResponse user,
        String title,
        String specialty,
        String bio,
        BigDecimal rating,
        BigDecimal hourlyRate,
        Boolean isAvailable,
        OffsetDateTime createdAt
) {
    public static SpecialistResponse fromEntity(Specialist specialist) {
        return new SpecialistResponse(
                specialist.getId(),
                UserResponse.fromEntity(specialist.getUser()),
                specialist.getTitle(),
                specialist.getSpecialty(),
                specialist.getBio(),
                specialist.getRating(),
                specialist.getHourlyRate(),
                specialist.getIsAvailable(),
                specialist.getCreatedAt()
        );
    }
}
