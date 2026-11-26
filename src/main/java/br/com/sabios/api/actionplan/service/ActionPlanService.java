package br.com.sabios.api.actionplan.service;

import br.com.sabios.api.actionplan.domain.ActionPlan;
import br.com.sabios.api.actionplan.dto.ActionPlanResponse;
import br.com.sabios.api.actionplan.dto.AssignActionPlanRequest;
import br.com.sabios.api.actionplan.dto.CreateActionPlanRequest;
import br.com.sabios.api.actionplan.dto.UpdateActionPlanRequest;
import br.com.sabios.api.actionplan.repository.ActionPlanRepository;
import br.com.sabios.api.agenda.service.SchedulingPolicy;
import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ActionPlanService {

    private static final String NO_DEADLINE = "Sem prazo";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Só mentorias confirmadas ou concluídas podem originar tarefas. */
    private static final List<MentoringStatus> LINKABLE = List.of(MentoringStatus.CONFIRMED, MentoringStatus.COMPLETED);

    private final ActionPlanRepository actionPlanRepository;
    private final MentoringRepository mentoringRepository;
    private final SpecialistRepository specialistRepository;
    private final NotificationService notificationService;
    private final SchedulingPolicy schedulingPolicy;

    public ActionPlanService(
            ActionPlanRepository actionPlanRepository,
            MentoringRepository mentoringRepository,
            SpecialistRepository specialistRepository,
            NotificationService notificationService,
            SchedulingPolicy schedulingPolicy
    ) {
        this.actionPlanRepository = actionPlanRepository;
        this.mentoringRepository = mentoringRepository;
        this.specialistRepository = specialistRepository;
        this.notificationService = notificationService;
        this.schedulingPolicy = schedulingPolicy;
    }

    /**
     * Cliente: as próprias tarefas. Conselheiro: as tarefas que ele atribuiu.
     * Filtros opcionais: {@code completed}, {@code overdue} e {@code mentoringId}.
     */
    @Transactional(readOnly = true)
    public List<ActionPlanResponse> list(User user, Boolean completed, Boolean overdue, UUID mentoringId) {
        List<ActionPlan> plans;
        if (user.getRole() == Role.COUNSELOR) {
            Specialist specialist = specialistRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NoSuchElementException("Perfil de especialista não encontrado."));
            plans = actionPlanRepository.findBySpecialistIdAndCreatedByIdOrderByCreatedAtDesc(specialist.getId(), user.getId());
        } else {
            plans = actionPlanRepository.findByClientIdOrderByCreatedAtDesc(user.getId());
        }

        LocalDate today = today();
        return plans.stream()
                .map(plan -> ActionPlanResponse.fromEntity(plan, today))
                .filter(plan -> completed == null || completed.equals(plan.completed()))
                .filter(plan -> overdue == null || overdue == plan.overdue())
                .filter(plan -> mentoringId == null || mentoringId.equals(plan.mentoringId()))
                .toList();
    }

    /** Tarefas de uma mentoria, visíveis para o cliente e para o conselheiro dela. */
    @Transactional(readOnly = true)
    public List<ActionPlanResponse> listByMentoring(User user, UUID mentoringId) {
        Mentoring mentoring = findMentoring(mentoringId);
        if (!isParticipant(user, mentoring)) {
            throw new AccessDeniedException("Você não participa desta mentoria.");
        }
        LocalDate today = today();
        return actionPlanRepository.findByMentoringIdOrderByCreatedAtDesc(mentoringId).stream()
                .map(plan -> ActionPlanResponse.fromEntity(plan, today))
                .toList();
    }

    @Transactional
    public ActionPlanResponse createActionPlan(User client, CreateActionPlanRequest request) {
        if (client.getRole() != Role.CLIENT && client.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas clientes podem criar metas no plano de ação.");
        }
        validateDueDate(request.dueDate());

        Mentoring mentoring = null;
        if (request.mentoringId() != null) {
            mentoring = findMentoring(request.mentoringId());
            if (!mentoring.getClient().getId().equals(client.getId())) {
                throw new AccessDeniedException("Esta mentoria não pertence a você.");
            }
            assertLinkable(mentoring);
        }

        ActionPlan plan = ActionPlan.builder()
                .client(client)
                .createdBy(client)
                .mentoring(mentoring)
                .specialist(mentoring != null ? mentoring.getSpecialist() : null)
                .title(request.title().trim())
                .deadline(resolveDeadlineText(request.deadline(), request.dueDate()))
                .dueDate(request.dueDate())
                .conselheiroNome(mentoring == null ? request.conselheiroNome() : null)
                .completed(false)
                .build();

        return ActionPlanResponse.fromEntity(actionPlanRepository.save(plan), today());
    }

    /** O conselheiro da mentoria atribui uma tarefa ao cliente dela e o cliente é notificado. */
    @Transactional
    public ActionPlanResponse assignToClient(User counselor, UUID mentoringId, AssignActionPlanRequest request) {
        Mentoring mentoring = findMentoring(mentoringId);
        if (!mentoring.getSpecialist().getUser().getId().equals(counselor.getId())) {
            throw new AccessDeniedException("Apenas o conselheiro da mentoria pode atribuir tarefas.");
        }
        assertLinkable(mentoring);
        validateDueDate(request.dueDate());

        ActionPlan plan = ActionPlan.builder()
                .client(mentoring.getClient())
                .createdBy(counselor)
                .mentoring(mentoring)
                .specialist(mentoring.getSpecialist())
                .title(request.title().trim())
                .deadline(resolveDeadlineText(request.deadline(), request.dueDate()))
                .dueDate(request.dueDate())
                .completed(false)
                .build();
        ActionPlan saved = actionPlanRepository.save(plan);

        notificationService.notify(mentoring.getClient(), NotificationType.ACTION_PLAN_ASSIGNED,
                "Nova tarefa no seu plano de ação",
                counselor.getName() + " adicionou a tarefa \"" + saved.getTitle() + "\" ("
                        + saved.getDeadline() + ").",
                mentoring.getId());

        return ActionPlanResponse.fromEntity(saved, today());
    }

    /** Edita título e prazo. Só quem criou a tarefa (cliente ou conselheiro) pode editá-la. */
    @Transactional
    public ActionPlanResponse update(User user, UUID id, UpdateActionPlanRequest request) {
        ActionPlan plan = findPlan(id);
        assertCanManage(user, plan);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("O título não pode ficar em branco.");
            }
            plan.setTitle(request.title().trim());
        }
        if (request.dueDate() != null) {
            validateDueDate(request.dueDate());
            plan.setDueDate(request.dueDate());
            // sem texto explícito, mantém o prazo em texto coerente com a nova data
            if (request.deadline() == null) {
                plan.setDeadline(DATE_FORMAT.format(request.dueDate()));
            }
        }
        if (request.deadline() != null) {
            plan.setDeadline(request.deadline().isBlank() ? NO_DEADLINE : request.deadline());
        }
        return ActionPlanResponse.fromEntity(plan, today());
    }

    @Transactional
    public void delete(User user, UUID id) {
        ActionPlan plan = findPlan(id);
        assertCanManage(user, plan);
        actionPlanRepository.delete(plan);
    }

    /** Quem executa a tarefa (o cliente dono) marca como concluída ou volta para pendente. */
    @Transactional
    public ActionPlanResponse toggleStatus(User user, UUID id) {
        ActionPlan plan = findPlan(id);

        if (!plan.getClient().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Você não tem permissão para alterar este plano de ação.");
        }

        boolean nowCompleted = !Boolean.TRUE.equals(plan.getCompleted());
        plan.setCompleted(nowCompleted);
        plan.setCompletedAt(nowCompleted ? OffsetDateTime.now() : null);
        return ActionPlanResponse.fromEntity(plan, today());
    }

    private void assertCanManage(User user, ActionPlan plan) {
        UUID ownerId = plan.getCreatedBy() != null ? plan.getCreatedBy().getId() : plan.getClient().getId();
        if (!ownerId.equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas quem criou esta tarefa pode alterá-la ou removê-la.");
        }
    }

    private void assertLinkable(Mentoring mentoring) {
        if (!LINKABLE.contains(mentoring.getStatus())) {
            throw new BadRequestException("Só é possível usar mentorias confirmadas ou concluídas no plano de ação.");
        }
    }

    private void validateDueDate(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(today())) {
            throw new BadRequestException("O prazo não pode estar no passado.");
        }
    }

    private String resolveDeadlineText(String text, LocalDate dueDate) {
        if (text != null && !text.isBlank()) {
            return text;
        }
        return dueDate != null ? DATE_FORMAT.format(dueDate) : NO_DEADLINE;
    }

    private boolean isParticipant(User user, Mentoring mentoring) {
        return mentoring.getClient().getId().equals(user.getId())
                || mentoring.getSpecialist().getUser().getId().equals(user.getId())
                || user.getRole() == Role.ADMIN;
    }

    private LocalDate today() {
        return LocalDate.now(schedulingPolicy.zone());
    }

    private ActionPlan findPlan(UUID id) {
        return actionPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item do plano de ação não encontrado."));
    }

    private Mentoring findMentoring(UUID id) {
        return mentoringRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));
    }
}
