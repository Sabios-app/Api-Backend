package br.com.sabios.api.reviews.service;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import br.com.sabios.api.reviews.domain.Review;
import br.com.sabios.api.reviews.dto.CreateReviewRequest;
import br.com.sabios.api.reviews.dto.ReviewResponse;
import br.com.sabios.api.reviews.repository.ReviewRepository;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MentoringRepository mentoringRepository;
    private final SpecialistRepository specialistRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         MentoringRepository mentoringRepository,
                         SpecialistRepository specialistRepository) {
        this.reviewRepository = reviewRepository;
        this.mentoringRepository = mentoringRepository;
        this.specialistRepository = specialistRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getSpecialistReviews(UUID specialistId) {
        return reviewRepository.findBySpecialistIdOrderByCreatedAtDesc(specialistId)
                .stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(User client, CreateReviewRequest request) {
        Mentoring mentoring = mentoringRepository.findById(request.mentoringId())
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));

        if (!mentoring.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("Apenas o cliente participante pode avaliar esta mentoria.");
        }

        if (reviewRepository.existsByMentoringId(mentoring.getId())) {
            throw new IllegalArgumentException("Esta mentoria já foi avaliada.");
        }

        Review review = Review.builder()
                .mentoring(mentoring)
                .client(client)
                .specialist(mentoring.getSpecialist())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        Review saved = reviewRepository.save(review);

        // Recalcular média do especialista automaticamente
        Double avg = reviewRepository.calculateAverageRatingBySpecialistId(mentoring.getSpecialist().getId());
        if (avg != null) {
            Specialist specialist = mentoring.getSpecialist();
            specialist.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            specialistRepository.save(specialist);
        }

        return ReviewResponse.fromEntity(saved);
    }
}
