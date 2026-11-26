package br.com.sabios.api.reviews.service;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.mentoring.dto.MentoringResponse;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import br.com.sabios.api.notifications.domain.NotificationType;
import br.com.sabios.api.notifications.service.NotificationService;
import br.com.sabios.api.reviews.domain.Review;
import br.com.sabios.api.reviews.dto.CreateReviewRequest;
import br.com.sabios.api.reviews.dto.ReviewResponse;
import br.com.sabios.api.reviews.dto.ReviewSummaryResponse;
import br.com.sabios.api.reviews.dto.UpdateReviewRequest;
import br.com.sabios.api.reviews.repository.ReviewRepository;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReviewService {

    /** Nota exibida para um especialista que ainda não recebeu avaliações (valor padrão do cadastro). */
    private static final BigDecimal DEFAULT_RATING = new BigDecimal("5.00");

    private final ReviewRepository reviewRepository;
    private final MentoringRepository mentoringRepository;
    private final SpecialistRepository specialistRepository;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         MentoringRepository mentoringRepository,
                         SpecialistRepository specialistRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.mentoringRepository = mentoringRepository;
        this.specialistRepository = specialistRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getSpecialistReviews(UUID specialistId, Pageable pageable) {
        assertSpecialistExists(specialistId);
        return reviewRepository.findBySpecialistIdOrderByCreatedAtDesc(specialistId, pageable)
                .stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSpecialistSummary(UUID specialistId) {
        assertSpecialistExists(specialistId);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(stars, 0L);
        }
        long total = 0;
        for (Object[] row : reviewRepository.countByRating(specialistId)) {
            long count = ((Number) row[1]).longValue();
            distribution.put(((Number) row[0]).intValue(), count);
            total += count;
        }

        Double avg = reviewRepository.calculateAverageRatingBySpecialistId(specialistId);
        BigDecimal average = avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        return new ReviewSummaryResponse(specialistId, average, total, distribution);
    }

    /** Cliente: avaliações que fez. Conselheiro: avaliações que recebeu. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> listMine(User user, Pageable pageable) {
        if (user.getRole() == Role.COUNSELOR) {
            Specialist specialist = specialistRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NoSuchElementException("Perfil de especialista não encontrado."));
            return getSpecialistReviews(specialist.getId(), pageable);
        }
        return reviewRepository.findByClientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    /** Mentorias concluídas que o cliente ainda não avaliou. */
    @Transactional(readOnly = true)
    public List<MentoringResponse> listPending(User client) {
        return reviewRepository.findPendingReviews(client.getId())
                .stream()
                .map(MentoringResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(User client, CreateReviewRequest request) {
        Mentoring mentoring = mentoringRepository.findById(request.mentoringId())
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));

        if (!mentoring.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("Apenas o cliente participante pode avaliar esta mentoria.");
        }

        if (mentoring.getStatus() != MentoringStatus.COMPLETED) {
            throw new IllegalArgumentException("Apenas mentorias concluídas podem ser avaliadas.");
        }

        if (reviewRepository.existsByMentoringId(mentoring.getId())) {
            throw new IllegalArgumentException("Esta mentoria já foi avaliada.");
        }

        // Bloqueia o especialista: avaliações simultâneas recalculam a média uma de cada vez.
        Specialist specialist = lockSpecialist(mentoring.getSpecialist().getId());

        Review saved = reviewRepository.save(Review.builder()
                .mentoring(mentoring)
                .client(client)
                .specialist(specialist)
                .rating(request.rating())
                .comment(normalize(request.comment()))
                .build());

        recalculateRating(specialist);

        notificationService.notify(specialist.getUser(), NotificationType.REVIEW_RECEIVED,
                "Nova avaliação recebida",
                client.getName() + " avaliou a mentoria \"" + mentoring.getTitle() + "\" com "
                        + saved.getRating() + (saved.getRating() == 1 ? " estrela." : " estrelas."),
                mentoring.getId());

        return ReviewResponse.fromEntity(saved);
    }

    /** O autor ajusta a nota e/ou o comentário da própria avaliação. */
    @Transactional
    public ReviewResponse updateReview(User user, UUID reviewId, UpdateReviewRequest request) {
        Review review = findReview(reviewId);
        if (!review.getClient().getId().equals(user.getId())) {
            throw new AccessDeniedException("Apenas o autor pode editar esta avaliação.");
        }

        Specialist specialist = lockSpecialist(review.getSpecialist().getId());

        if (request.rating() != null) {
            review.setRating(request.rating());
        }
        if (request.comment() != null) {
            review.setComment(normalize(request.comment()));
        }

        recalculateRating(specialist);
        return ReviewResponse.fromEntity(review);
    }

    /** O autor (ou um administrador) remove a avaliação e a média do especialista é recalculada. */
    @Transactional
    public void deleteReview(User user, UUID reviewId) {
        Review review = findReview(reviewId);
        if (!review.getClient().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas o autor pode remover esta avaliação.");
        }

        Specialist specialist = lockSpecialist(review.getSpecialist().getId());
        reviewRepository.delete(review);
        recalculateRating(specialist);
    }

    private void recalculateRating(Specialist specialist) {
        // a consulta da média faz flush das alterações pendentes antes de executar
        Double avg = reviewRepository.calculateAverageRatingBySpecialistId(specialist.getId());
        specialist.setRating(avg == null ? DEFAULT_RATING : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
    }

    private Specialist lockSpecialist(UUID specialistId) {
        return specialistRepository.findByIdForUpdate(specialistId)
                .orElseThrow(() -> new NoSuchElementException("Especialista não encontrado."));
    }

    private void assertSpecialistExists(UUID specialistId) {
        if (!specialistRepository.existsById(specialistId)) {
            throw new NoSuchElementException("Especialista não encontrado com o ID informado.");
        }
    }

    private Review findReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Avaliação não encontrada."));
    }

    private String normalize(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }
}
