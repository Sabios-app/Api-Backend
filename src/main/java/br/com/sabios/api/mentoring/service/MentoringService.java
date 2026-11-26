package br.com.sabios.api.mentoring.service;

import br.com.sabios.api.agenda.service.SchedulingPolicy;
import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringScope;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.mentoring.dto.CreateMentoringRequest;
import br.com.sabios.api.mentoring.dto.MentoringResponse;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import br.com.sabios.api.notifications.domain.NotificationType;
import br.com.sabios.api.notifications.service.NotificationService;
import br.com.sabios.api.shared.exception.BadRequestException;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class MentoringService {

    private static final List<MentoringStatus> ACTIVE_STATUSES = List.of(MentoringStatus.REQUESTED, MentoringStatus.CONFIRMED);
    private static final List<MentoringStatus> CONFIRMED_ONLY = List.of(MentoringStatus.CONFIRMED);
    private static final UUID NO_EXCLUSION = new UUID(0L, 0L);

    private final MentoringRepository mentoringRepository;
    private final SpecialistRepository specialistRepository;
    private final SchedulingPolicy schedulingPolicy;
    private final NotificationService notificationService;

    public MentoringService(
            MentoringRepository mentoringRepository,
            SpecialistRepository specialistRepository,
            SchedulingPolicy schedulingPolicy,
            NotificationService notificationService
    ) {
        this.mentoringRepository = mentoringRepository;
        this.specialistRepository = specialistRepository;
        this.schedulingPolicy = schedulingPolicy;
        this.notificationService = notificationService;
    }

    @Transactional
    public MentoringResponse requestMentoring(User client, CreateMentoringRequest request) {
        if (client.getRole() != Role.CLIENT && client.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas clientes podem solicitar uma mentoria.");
        }

        // Bloqueia o conselheiro até o fim da transação: solicitações simultâneas para ele são serializadas.
        Specialist specialist = specialistRepository.findByIdForUpdate(request.specialistId())
                .orElseThrow(() -> new NoSuchElementException("Especialista não encontrado para agendamento."));

        if (specialist.getUser().getId().equals(client.getId())) {
            throw new BadRequestException("Você não pode solicitar uma mentoria para si mesmo.");
        }

        if (!Boolean.TRUE.equals(specialist.getIsAvailable())) {
            throw new IllegalArgumentException("O especialista selecionado não está disponível no momento.");
        }

        if (!schedulingPolicy.fitsAvailability(specialist.getId(), request.scheduledAt())) {
            throw new BadRequestException("O horário escolhido está fora da disponibilidade do especialista.");
        }

        assertSpecialistFree(specialist.getId(), request.scheduledAt(), NO_EXCLUSION);
        assertClientFree(client.getId(), request.scheduledAt());

        Mentoring mentoring = Mentoring.builder()
                .client(client)
                .specialist(specialist)
                .title(request.title())
                .description(request.description())
                .doubts(request.doubts())
                .status(MentoringStatus.REQUESTED)
                .scheduledAt(request.scheduledAt())
                .build();

        Mentoring saved = mentoringRepository.save(mentoring);
        notificationService.notify(specialist.getUser(), NotificationType.MENTORING_REQUESTED,
                "Nova solicitação de mentoria",
                client.getName() + " solicitou a mentoria \"" + saved.getTitle() + "\" para " + formatWhen(saved) + ".",
                saved.getId());
        return MentoringResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<MentoringResponse> listUserMentorings(User user, MentoringStatus status, MentoringScope scope) {
        List<Mentoring> mentorings;
        if (user.getRole() == Role.COUNSELOR) {
            Specialist specialist = specialistRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NoSuchElementException("Perfil de especialista não encontrado."));
            mentorings = mentoringRepository.findBySpecialistIdOrderByScheduledAtDesc(specialist.getId());
        } else {
            mentorings = mentoringRepository.findByClientIdOrderByScheduledAtDesc(user.getId());
        }

        OffsetDateTime now = OffsetDateTime.now();
        Stream<Mentoring> stream = mentorings.stream();
        if (status != null) {
            stream = stream.filter(m -> m.getStatus() == status);
        }
        if (scope == MentoringScope.UPCOMING) {
            stream = stream.filter(m -> isUpcoming(m, now))
                    .sorted(Comparator.comparing(Mentoring::getScheduledAt));
        } else if (scope == MentoringScope.HISTORY) {
            stream = stream.filter(m -> !isUpcoming(m, now))
                    .sorted(Comparator.comparing(Mentoring::getScheduledAt).reversed());
        }
        return stream.map(MentoringResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public MentoringResponse getNextMentoring(User user) {
        return mentoringRepository.findNextConfirmedMentoring(user.getId(), MentoringStatus.CONFIRMED, OffsetDateTime.now())
                .map(MentoringResponse::fromEntity)
                .orElseThrow(() -> new NoSuchElementException("Nenhuma mentoria futura confirmada encontrada."));
    }

    @Transactional
    public MentoringResponse acceptMentoring(User user, UUID mentoringId, String meetingLink) {
        Mentoring mentoring = findOwnedByCounselor(user, mentoringId);
        lockSpecialist(mentoring);

        if (mentoring.getStatus() != MentoringStatus.REQUESTED) {
            throw new IllegalArgumentException("Apenas mentorias com status REQUESTED podem ser aceitas.");
        }
        if (!mentoring.getScheduledAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Não é possível aceitar uma mentoria cujo horário já passou.");
        }

        // Entre a solicitação e o aceite, outra mentoria pode ter sido confirmada no mesmo horário.
        assertSpecialistFree(mentoring.getSpecialist().getId(), mentoring.getScheduledAt(), mentoring.getId());

        mentoring.setStatus(MentoringStatus.CONFIRMED);
        if (meetingLink != null && !meetingLink.isBlank()) {
            mentoring.setMeetingLink(meetingLink);
        }
        notifyClient(mentoring, NotificationType.MENTORING_ACCEPTED, "Mentoria confirmada",
                " confirmou a mentoria \"" + mentoring.getTitle() + "\" para " + formatWhen(mentoring) + ".");
        return MentoringResponse.fromEntity(mentoring);
    }

    @Transactional
    public MentoringResponse rejectMentoring(User user, UUID mentoringId) {
        Mentoring mentoring = findOwnedByCounselor(user, mentoringId);

        if (mentoring.getStatus() != MentoringStatus.REQUESTED) {
            throw new IllegalArgumentException("Apenas mentorias com status REQUESTED podem ser rejeitadas.");
        }

        mentoring.setStatus(MentoringStatus.REJECTED);
        notifyClient(mentoring, NotificationType.MENTORING_REJECTED, "Solicitação recusada",
                " recusou a mentoria \"" + mentoring.getTitle() + "\" de " + formatWhen(mentoring) + ".");
        return MentoringResponse.fromEntity(mentoring);
    }

    /** Cliente ou conselheiro da mentoria cancelam uma solicitação pendente ou uma sessão confirmada futura. */
    @Transactional
    public MentoringResponse cancelMentoring(User user, UUID mentoringId) {
        Mentoring mentoring = findById(mentoringId);

        boolean isClient = mentoring.getClient().getId().equals(user.getId());
        boolean isCounselor = mentoring.getSpecialist().getUser().getId().equals(user.getId());
        if (!isClient && !isCounselor && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Você não tem autorização para cancelar esta mentoria.");
        }

        if (!ACTIVE_STATUSES.contains(mentoring.getStatus())) {
            throw new IllegalArgumentException("Apenas mentorias REQUESTED ou CONFIRMED podem ser canceladas.");
        }
        if (mentoring.getStatus() == MentoringStatus.CONFIRMED && !mentoring.getScheduledAt().isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Não é possível cancelar uma mentoria que já começou.");
        }

        mentoring.setStatus(MentoringStatus.CANCELED);
        String message = "A mentoria \"" + mentoring.getTitle() + "\" de " + formatWhen(mentoring) + " foi cancelada por "
                + user.getName() + ".";
        if (!isClient) {
            notificationService.notify(mentoring.getClient(), NotificationType.MENTORING_CANCELED,
                    "Mentoria cancelada", message, mentoring.getId());
        }
        if (!isCounselor) {
            notificationService.notify(mentoring.getSpecialist().getUser(), NotificationType.MENTORING_CANCELED,
                    "Mentoria cancelada", message, mentoring.getId());
        }
        return MentoringResponse.fromEntity(mentoring);
    }

    /** O conselheiro marca como concluída uma sessão confirmada cujo horário já começou. */
    @Transactional
    public MentoringResponse completeMentoring(User user, UUID mentoringId) {
        Mentoring mentoring = findOwnedByCounselor(user, mentoringId);

        if (mentoring.getStatus() != MentoringStatus.CONFIRMED) {
            throw new IllegalArgumentException("Apenas mentorias com status CONFIRMED podem ser concluídas.");
        }
        if (mentoring.getScheduledAt().isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("A mentoria só pode ser concluída depois do horário agendado.");
        }

        mentoring.setStatus(MentoringStatus.COMPLETED);
        return MentoringResponse.fromEntity(mentoring);
    }

    @Transactional
    public MentoringResponse updateMeetingLink(User user, UUID mentoringId, String meetingLink) {
        Mentoring mentoring = findOwnedByCounselor(user, mentoringId);

        if (mentoring.getStatus() != MentoringStatus.CONFIRMED) {
            throw new IllegalArgumentException("O link só pode ser alterado em mentorias CONFIRMED.");
        }

        mentoring.setMeetingLink(meetingLink);
        return MentoringResponse.fromEntity(mentoring);
    }

    /** Marca como EXPIRED as solicitações pendentes cujo horário já passou. Retorna quantas foram alteradas. */
    @Transactional
    public int expireOverdueRequests() {
        return mentoringRepository.expireOverdue(MentoringStatus.REQUESTED, MentoringStatus.EXPIRED, OffsetDateTime.now());
    }

    private void notifyClient(Mentoring mentoring, NotificationType type, String title, String messageAfterCounselorName) {
        notificationService.notify(mentoring.getClient(), type, title,
                mentoring.getSpecialist().getUser().getName() + messageAfterCounselorName, mentoring.getId());
    }

    private String formatWhen(Mentoring mentoring) {
        return mentoring.getScheduledAt().atZoneSameInstant(schedulingPolicy.zone())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
    }

    private boolean isUpcoming(Mentoring mentoring, OffsetDateTime now) {
        return ACTIVE_STATUSES.contains(mentoring.getStatus()) && !mentoring.getScheduledAt().isBefore(now);
    }

    /** Falha se outra mentoria CONFIRMED do especialista colide com o horário (janela de uma sessão). */
    private void assertSpecialistFree(UUID specialistId, OffsetDateTime start, UUID excludeId) {
        Duration duration = schedulingPolicy.sessionDuration();
        long conflicts = mentoringRepository.countSpecialistConflicts(
                specialistId, CONFIRMED_ONLY, excludeId, start.minus(duration), start.plus(duration));
        if (conflicts > 0) {
            throw new IllegalArgumentException("Este horário já está reservado para o especialista.");
        }
    }

    /** Impede o cliente de manter duas solicitações/sessões ativas sobrepostas (inclui pedidos duplicados). */
    private void assertClientFree(UUID clientId, OffsetDateTime start) {
        Duration duration = schedulingPolicy.sessionDuration();
        long conflicts = mentoringRepository.countClientConflicts(
                clientId, ACTIVE_STATUSES, NO_EXCLUSION, start.minus(duration), start.plus(duration));
        if (conflicts > 0) {
            throw new IllegalArgumentException("Você já possui uma mentoria solicitada ou confirmada neste horário.");
        }
    }

    private void lockSpecialist(Mentoring mentoring) {
        specialistRepository.findByIdForUpdate(mentoring.getSpecialist().getId());
    }

    private Mentoring findById(UUID mentoringId) {
        return mentoringRepository.findById(mentoringId)
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));
    }

    private Mentoring findOwnedByCounselor(User user, UUID mentoringId) {
        Mentoring mentoring = findById(mentoringId);
        UUID counselorUserId = mentoring.getSpecialist().getUser().getId();
        if (!counselorUserId.equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Você não tem autorização para responder a esta mentoria.");
        }
        return mentoring;
    }
}
