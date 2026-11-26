package br.com.sabios.api.specialists.controller;

import br.com.sabios.api.specialists.dto.CreateSpecialistRequest;
import br.com.sabios.api.specialists.dto.SpecialistFilter;
import br.com.sabios.api.specialists.dto.SpecialistResponse;
import br.com.sabios.api.specialists.dto.UpdateSpecialistRequest;
import br.com.sabios.api.specialists.service.SpecialistService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/specialists")
@Tag(name = "Especialistas", description = "Endpoints para consulta e gestão de perfis de especialistas")
public class SpecialistController {

    private final SpecialistService specialistService;

    public SpecialistController(SpecialistService specialistService) {
        this.specialistService = specialistService;
    }

    @GetMapping
    @Operation(summary = "Lista especialistas disponíveis com busca, filtros (especialidade, nota mínima, faixa de preço, dia da semana atendido) e paginação. Ordenação: sort=rating,desc | hourlyRate,asc | user.name,asc")
    public ResponseEntity<Page<SpecialistResponse>> list(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) DayOfWeek availableDay,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(specialistService.listSpecialists(
                new SpecialistFilter(specialty, search, minRating, minPrice, maxPrice, availableDay), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de um especialista pelo ID")
    public ResponseEntity<SpecialistResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(specialistService.getById(id));
    }

    @PostMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria o perfil profissional de especialista para o usuário logado (Conselheiro)")
    public ResponseEntity<SpecialistResponse> createProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSpecialistRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialistService.createProfile(user, request));
    }

    @PatchMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza o perfil profissional de especialista do conselheiro logado")
    public ResponseEntity<SpecialistResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateSpecialistRequest request
    ) {
        return ResponseEntity.ok(specialistService.updateMyProfile(user, request));
    }
}
