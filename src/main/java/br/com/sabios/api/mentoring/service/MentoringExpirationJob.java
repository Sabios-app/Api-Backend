package br.com.sabios.api.mentoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodicamente marca como EXPIRED as solicitações que ninguém respondeu antes do horário. */
@Component
public class MentoringExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(MentoringExpirationJob.class);

    private final MentoringService mentoringService;

    public MentoringExpirationJob(MentoringService mentoringService) {
        this.mentoringService = mentoringService;
    }

    @Scheduled(fixedDelayString = "${app.mentoring.expiration-check-ms}",
            initialDelayString = "${app.mentoring.expiration-check-ms}")
    public void run() {
        int expired = mentoringService.expireOverdueRequests();
        if (expired > 0) {
            log.info("{} solicitação(ões) de mentoria expirada(s) por falta de resposta.", expired);
        }
    }
}
