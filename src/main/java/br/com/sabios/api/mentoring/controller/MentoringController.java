package br.com.sabios.api.mentoring.controller;

import br.com.sabios.api.mentoring.domain.MentoringScope;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.mentoring.dto.AcceptMentoringRequest;
import br.com.sabios.api.mentoring.dto.CreateMentoringRequest;
import br.com.sabios.api.mentoring.dto.MeetingLinkRequest;
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
    @Operation(summary = "Lista as mentorias do usuário logado (Cliente ou Conselheiro). "
            + "Filtros opcionais: status e scope (UPCOMING = próximas/pendentes, HISTORY = histórico)")
    public ResponseEntity<List<MentoringResponse>> listMyMentorings(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) MentoringStatus status,
            @RequestParam(required = false) MentoringScope scope
    ) {
        return ResponseEntity.ok(mentoringService.listUserMentorings(user, status, scope));
    }

    @GetMapping("/next")
    @Operation(summary = "Retorna a próxima mentoria confirmada mais próxima com link de chamada")
    public ResponseEntity<MentoringResponse> getNextMentoring(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mentoringService.getNextMentoring(user));
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Conselheiro aceita uma mentoria solicitada (altera para CONFIRMED). "
            + "O corpo é opcional e permite informar o link da videochamada")
    public ResponseEntity<MentoringResponse> acceptMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AcceptMentoringRequest request
    ) {
        String meetingLink = request != null ? request.meetingLink() : null;
        return ResponseEntity.ok(mentoringService.acceptMentoring(user, id, meetingLink));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Conselheiro recusa uma mentoria solicitada (altera para REJECTED)")
    public ResponseEntity<MentoringResponse> rejectMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mentoringService.rejectMentoring(user, id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cliente ou conselheiro cancela uma mentoria REQUESTED ou CONFIRMED futura (altera para CANCELED)")
    public ResponseEntity<MentoringResponse> cancelMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mentoringService.cancelMentoring(user, id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Conselheiro marca uma mentoria CONFIRMED, já iniciada, como concluída (altera para COMPLETED)")
    public ResponseEntity<MentoringResponse> completeMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mentoringService.completeMentoring(user, id));
    }

    @PatchMapping("/{id}/meeting-link")
    @Operation(summary = "Conselheiro informa ou altera o link da videochamada de uma mentoria CONFIRMED")
    public ResponseEntity<MentoringResponse> updateMeetingLink(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody MeetingLinkRequest request
    ) {
        return ResponseEntity.ok(mentoringService.updateMeetingLink(user, id, request.meetingLink()));
    }
}
