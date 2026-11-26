package br.com.sabios.api.reviews.controller;

import br.com.sabios.api.mentoring.dto.MentoringResponse;
import br.com.sabios.api.reviews.dto.CreateReviewRequest;
import br.com.sabios.api.reviews.dto.ReviewResponse;
import br.com.sabios.api.reviews.dto.ReviewSummaryResponse;
import br.com.sabios.api.reviews.dto.UpdateReviewRequest;
import br.com.sabios.api.reviews.service.ReviewService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Avaliações", description = "Avaliações que os clientes fazem dos conselheiros após uma mentoria concluída")
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/specialists/{specialistId}")
    @Operation(summary = "Lista pública de avaliações de um especialista, da mais recente para a mais antiga (page/size opcionais, máx. 50)")
    public ResponseEntity<List<ReviewResponse>> getSpecialistReviews(
            @PathVariable UUID specialistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(reviewService.getSpecialistReviews(specialistId, pageOf(page, size)));
    }

    @GetMapping("/specialists/{specialistId}/summary")
    @Operation(summary = "Resumo público: nota média, total e distribuição de 1 a 5 estrelas")
    public ResponseEntity<ReviewSummaryResponse> getSpecialistSummary(@PathVariable UUID specialistId) {
        return ResponseEntity.ok(reviewService.getSpecialistSummary(specialistId));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cliente: avaliações que fez. Conselheiro: avaliações que recebeu")
    public ResponseEntity<List<ReviewResponse>> listMine(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(reviewService.listMine(user, pageOf(page, size)));
    }

    @GetMapping("/pending")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cliente: mentorias concluídas que ainda não foram avaliadas")
    public ResponseEntity<List<MentoringResponse>> listPending(@AuthenticationPrincipal User client) {
        return ResponseEntity.ok(reviewService.listPending(client));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cliente avalia uma mentoria CONCLUÍDA (1 a 5 estrelas). Recalcula a média do especialista e o notifica")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal User client,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(client, request));
    }

    @PatchMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "O autor edita a nota e/ou o comentário da própria avaliação")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReview(user, id, request));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "O autor (ou um administrador) remove a avaliação")
    public ResponseEntity<Void> deleteReview(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        reviewService.deleteReview(user, id);
        return ResponseEntity.noContent().build();
    }

    private PageRequest pageOf(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
