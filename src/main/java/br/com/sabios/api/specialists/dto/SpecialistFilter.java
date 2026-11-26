package br.com.sabios.api.specialists.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/** Critérios opcionais da busca de especialistas. Campos nulos ou vazios são ignorados. */
public record SpecialistFilter(
        String specialty,
        String search,
        BigDecimal minRating,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        DayOfWeek availableDay
) {}
