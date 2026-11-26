package br.com.sabios.api.reviews.controller;

import br.com.sabios.api.reviews.dto.CreateReviewRequest;
import br.com.sabios.api.reviews.dto.ReviewResponse;
import br.com.sabios.api.reviews.service.ReviewService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Avaliações", description = "Endpoints para avaliações e comentários de mentorias")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/specialists/{specialistId}")
    @Operation(summary = "Lista pública de avaliações de um especialista")
    public ResponseEntity<List<ReviewResponse>> getSpecialistReviews(@PathVariable UUID specialistId) {
        return ResponseEntity.ok(reviewService.getSpecialistReviews(specialistId));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cliente avalia uma mentoria (1 a 5 estrelas) com recálculo automático da média")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal User client,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(client, request));
    }
}
