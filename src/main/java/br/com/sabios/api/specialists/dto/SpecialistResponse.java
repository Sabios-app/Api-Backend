package br.com.sabios.api.specialists.dto;

import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.users.dto.PublicUserResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SpecialistResponse(
        UUID id,
        /** Apenas dados públicos do conselheiro; e-mail e telefone não são expostos. */
        PublicUserResponse user,
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
                PublicUserResponse.fromEntity(specialist.getUser()),
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
