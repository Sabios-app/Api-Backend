package br.com.sabios.api.actionplan.domain;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.users.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "action_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Cliente dono da tarefa (quem a executa). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    /** Mentoria que originou a tarefa (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoring_id")
    private Mentoring mentoring;

    /** Conselheiro relacionado à tarefa (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id")
    private Specialist specialist;

    /** Quem criou a tarefa: o próprio cliente ou o conselheiro que a atribuiu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /** Nome do conselheiro em texto, usado quando a tarefa não está ligada a um especialista cadastrado. */
    @Column(name = "conselheiro_nome")
    private String conselheiroNome;

    @Column(nullable = false)
    private String title;

    /** Prazo em texto livre (compatibilidade com o front). O prazo estruturado é {@link #dueDate}. */
    @Column(length = 100)
    private String deadline;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
