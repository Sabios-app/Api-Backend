package br.com.sabios.api.mentoring.dto;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.specialists.dto.SpecialistResponse;
import br.com.sabios.api.users.dto.UserResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MentoringResponse(
        UUID id,
        UserResponse client,
        SpecialistResponse specialist,
        String title,
        String description,
        String doubts,
        MentoringStatus status,
        OffsetDateTime scheduledAt,
        String meetingLink,
        OffsetDateTime createdAt
) {
    public static MentoringResponse fromEntity(Mentoring mentoring) {
        return new MentoringResponse(
                mentoring.getId(),
                UserResponse.fromEntity(mentoring.getClient()),
                SpecialistResponse.fromEntity(mentoring.getSpecialist()),
                mentoring.getTitle(),
                mentoring.getDescription(),
                mentoring.getDoubts(),
                mentoring.getStatus(),
                mentoring.getScheduledAt(),
                mentoring.getMeetingLink(),
                mentoring.getCreatedAt()
        );
    }
}
