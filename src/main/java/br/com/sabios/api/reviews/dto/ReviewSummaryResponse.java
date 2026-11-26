package br.com.sabios.api.reviews.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Resumo das avaliações de um especialista: nota média, total e quantas avaliações de cada nota (1 a 5). */
public record ReviewSummaryResponse(
        UUID specialistId,
        /** Média com 2 casas decimais; nula quando ainda não há avaliações. */
        BigDecimal average,
        long total,
        Map<Integer, Long> distribution
) {}
