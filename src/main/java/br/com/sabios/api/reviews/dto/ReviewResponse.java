package br.com.sabios.api.reviews.dto;

import br.com.sabios.api.reviews.domain.Review;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID mentoringId,
        /** Apenas dados públicos de quem avaliou (sem e-mail, telefone ou perfil). */
        Reviewer client,
        Integer rating,
        String comment,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record Reviewer(String name, String avatarUrl) {}

    public static ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getMentoring().getId(),
                new Reviewer(review.getClient().getName(), review.getClient().getAvatarUrl()),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
