package br.com.sabios.api.mentoring.service;

import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.domain.MentoringStatus;
import br.com.sabios.api.mentoring.dto.CreateMentoringRequest;
import br.com.sabios.api.mentoring.dto.MentoringResponse;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MentoringService {

    private final MentoringRepository mentoringRepository;
    private final SpecialistRepository specialistRepository;

    public MentoringService(MentoringRepository mentoringRepository, SpecialistRepository specialistRepository) {
        this.mentoringRepository = mentoringRepository;
        this.specialistRepository = specialistRepository;
    }

    @Transactional
    public MentoringResponse requestMentoring(User client, CreateMentoringRequest request) {
        if (client.getRole() != Role.CLIENT && client.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas clientes podem solicitar uma mentoria.");
        }

        Specialist specialist = specialistRepository.findById(request.specialistId())
                .orElseThrow(() -> new NoSuchElementException("Especialista não encontrado para agendamento."));

        if (!Boolean.TRUE.equals(specialist.getIsAvailable())) {
            throw new IllegalArgumentException("O especialista selecionado não está disponível no momento.");
        }

        // Gera um link de conferência amigável
        String meetingLink = "https://meet.google.com/sabios-" + UUID.randomUUID().toString().substring(0, 8);

        Mentoring mentoring = Mentoring.builder()
                .client(client)
                .specialist(specialist)
                .title(request.title())
                .description(request.description())
                .doubts(request.doubts())
                .status(MentoringStatus.REQUESTED)
                .scheduledAt(request.scheduledAt())
                .meetingLink(meetingLink)
                .build();

        Mentoring saved = mentoringRepository.save(mentoring);
        return MentoringResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<MentoringResponse> listUserMentorings(User user) {
        if (user.getRole() == Role.COUNSELOR) {
            Specialist specialist = specialistRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NoSuchElementException("Perfil de especialista não encontrado."));
            return mentoringRepository.findBySpecialistIdOrderByScheduledAtDesc(specialist.getId())
                    .stream()
                    .map(MentoringResponse::fromEntity)
                    .toList();
        }

        return mentoringRepository.findByClientIdOrderByScheduledAtDesc(user.getId())
                .stream()
                .map(MentoringResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MentoringResponse getNextMentoring(User user) {
        return mentoringRepository.findNextConfirmedMentoring(user.getId(), MentoringStatus.CONFIRMED, OffsetDateTime.now())
                .map(MentoringResponse::fromEntity)
                .orElseThrow(() -> new NoSuchElementException("Nenhuma mentoria futura confirmada encontrada."));
    }

    @Transactional
    public MentoringResponse acceptMentoring(User user, UUID mentoringId) {
        Mentoring mentoring = mentoringRepository.findById(mentoringId)
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));

        validateCounselorOwnership(user, mentoring);

        if (mentoring.getStatus() != MentoringStatus.REQUESTED) {
            throw new IllegalArgumentException("Apenas mentorias com status REQUESTED podem ser aceitas.");
        }

        mentoring.setStatus(MentoringStatus.CONFIRMED);
        return MentoringResponse.fromEntity(mentoring);
    }

    @Transactional
    public MentoringResponse rejectMentoring(User user, UUID mentoringId) {
        Mentoring mentoring = mentoringRepository.findById(mentoringId)
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));

        validateCounselorOwnership(user, mentoring);

        if (mentoring.getStatus() != MentoringStatus.REQUESTED) {
            throw new IllegalArgumentException("Apenas mentorias com status REQUESTED podem ser rejeitadas.");
        }

        mentoring.setStatus(MentoringStatus.REJECTED);
        return MentoringResponse.fromEntity(mentoring);
    }

    private void validateCounselorOwnership(User user, Mentoring mentoring) {
        UUID counselorUserId = mentoring.getSpecialist().getUser().getId();
        if (!counselorUserId.equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Você não tem autorização para responder a esta mentoria.");
        }
    }
}
