package br.com.sabios.api.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "Status da Aplicação", description = "Endpoint de verificação de integridade (Health Check)")
public class StatusController {

    @GetMapping
    @Operation(summary = "Verifica se a API está online e respondendo")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "Sábios App Backend",
                "version", "v1.0.0",
                "timestamp", OffsetDateTime.now()
        ));
    }
}
