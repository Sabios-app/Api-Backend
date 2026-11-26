package br.com.sabios.api.mentoring.controller;

import br.com.sabios.api.mentoring.dto.CreateMentoringRequest;
import br.com.sabios.api.mentoring.dto.MentoringResponse;
import br.com.sabios.api.mentoring.service.MentoringService;
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
@RequestMapping("/api/v1/mentorings")
@Tag(name = "Mentorias", description = "Endpoints para agendamento, consulta e gestão de mentorias")
@SecurityRequirement(name = "bearerAuth")
public class MentoringController {

    private final MentoringService mentoringService;

    public MentoringController(MentoringService mentoringService) {
        this.mentoringService = mentoringService;
    }

    @PostMapping
    @Operation(summary = "Cliente solicita uma nova mentoria (status inicial REQUESTED)")
    public ResponseEntity<MentoringResponse> requestMentoring(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateMentoringRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mentoringService.requestMentoring(user, request));
    }

    @GetMapping
    @Operation(summary = "Retorna a lista de mentorias do usuário logado (Cliente ou Conselheiro)")
    public ResponseEntity<List<MentoringResponse>> listMyMentorings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mentoringService.listUserMentorings(user));
    }

    @GetMapping("/next")
    @Operation(summary = "Retorna a próxima mentoria confirmada mais próxima com link de chamada")
    public ResponseEntity<MentoringResponse> getNextMentoring(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mentoringService.getNextMentoring(user));
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Conselheiro aceita uma mentoria solicitada (altera para CONFIRMED)")
    public ResponseEntity<MentoringResponse> acceptMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mentoringService.acceptMentoring(user, id));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Conselheiro recusa uma mentoria solicitada (altera para REJECTED)")
    public ResponseEntity<MentoringResponse> rejectMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mentoringService.rejectMentoring(user, id));
    }
}
