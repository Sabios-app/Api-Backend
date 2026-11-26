package br.com.sabios.api.agenda.controller;

import br.com.sabios.api.agenda.dto.AvailabilityResponse;
import br.com.sabios.api.agenda.dto.CreateAvailabilityRequest;
import br.com.sabios.api.agenda.service.AgendaService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agenda")
@Tag(name = "Agenda", description = "Endpoints de disponibilidade e horários de especialistas")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping("/specialists/{specialistId}")
    @Operation(summary = "Lista pública de horários disponíveis de um especialista")
    public ResponseEntity<List<AvailabilityResponse>> getSpecialistAvailability(@PathVariable UUID specialistId) {
        return ResponseEntity.ok(agendaService.getSpecialistAvailability(specialistId));
    }

    @GetMapping("/specialists/{specialistId}/slots")
    @Operation(summary = "Lista pública dos horários livres (datas e horas) de um especialista em um período de até 31 dias")
    public ResponseEntity<List<OffsetDateTime>> getFreeSlots(
            @PathVariable UUID specialistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(agendaService.getFreeSlots(specialistId, from, to));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lista os horários da agenda do conselheiro autenticado")
    public ResponseEntity<List<AvailabilityResponse>> getMyAvailability(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(agendaService.getMyAvailability(user));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Conselheiro adiciona um novo horário disponível na sua agenda")
    public ResponseEntity<AvailabilityResponse> addAvailability(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateAvailabilityRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendaService.createAvailability(user, request));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Conselheiro remove um horário da sua agenda")
    public ResponseEntity<Void> deleteAvailability(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        agendaService.deleteAvailability(user, id);
        return ResponseEntity.noContent().build();
    }
}
