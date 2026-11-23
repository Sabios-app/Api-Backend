package br.com.sabios.api.specialists.dto;

import java.math.BigDecimal;

public record UpdateSpecialistRequest(
        String title,
        String specialty,
        String bio,
        BigDecimal hourlyRate,
        Boolean isAvailable
) {}
