package br.com.sabios.api.reviews.dto;

import br.com.sabios.api.reviews.domain.Review;
import br.com.sabios.api.users.dto.UserResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID mentoringId,
        UserResponse client,
        Integer rating,
        String comment,
        OffsetDateTime createdAt
) {
    public static ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getMentoring().getId(),
                UserResponse.fromEntity(review.getClient()),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
