package br.com.sabios.api.specialists.service;

import br.com.sabios.api.shared.exception.BadRequestException;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.dto.CreateSpecialistRequest;
import br.com.sabios.api.specialists.dto.SpecialistFilter;
import br.com.sabios.api.specialists.dto.SpecialistResponse;
import br.com.sabios.api.specialists.dto.UpdateSpecialistRequest;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.specialists.repository.SpecialistSpecifications;
import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SpecialistService {

    private final SpecialistRepository specialistRepository;

    public SpecialistService(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    @Transactional(readOnly = true)
    public Page<SpecialistResponse> listSpecialists(SpecialistFilter filter, Pageable pageable) {
        validateFilter(filter);
        return specialistRepository.findAll(SpecialistSpecifications.matching(filter), pageable)
                .map(SpecialistResponse::fromEntity);
    }

    private void validateFilter(SpecialistFilter filter) {
        if (filter.minRating() != null
                && (filter.minRating().compareTo(BigDecimal.ZERO) < 0 || filter.minRating().compareTo(new BigDecimal("5")) > 0)) {
            throw new BadRequestException("A nota mínima deve estar entre 0 e 5.");
        }
        if ((filter.minPrice() != null && filter.minPrice().signum() < 0)
                || (filter.maxPrice() != null && filter.maxPrice().signum() < 0)) {
            throw new BadRequestException("Os valores de preço não podem ser negativos.");
        }
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new BadRequestException("O preço mínimo não pode ser maior que o preço máximo.");
        }
    }

    @Transactional(readOnly = true)
    public SpecialistResponse getById(UUID id) {
        Specialist specialist = specialistRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Especialista não encontrado com o ID informado."));
        return SpecialistResponse.fromEntity(specialist);
    }

    @Transactional
    public SpecialistResponse createProfile(User user, CreateSpecialistRequest request) {
        if (user.getRole() != Role.COUNSELOR && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Apenas conselheiros podem criar um perfil de especialista.");
        }

        if (specialistRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalArgumentException("O usuário já possui um perfil de especialista cadastrado.");
        }

        Specialist specialist = Specialist.builder()
                .user(user)
                .title(request.title())
                .specialty(request.specialty())
                .bio(request.bio())
                .hourlyRate(request.hourlyRate())
                .isAvailable(true)
                .build();

        Specialist saved = specialistRepository.save(specialist);
        return SpecialistResponse.fromEntity(saved);
    }

    @Transactional
    public SpecialistResponse updateMyProfile(User user, UpdateSpecialistRequest request) {
        Specialist specialist = specialistRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("Perfil de especialista não encontrado para o usuário logado."));

        if (request.title() != null && !request.title().isBlank()) {
            specialist.setTitle(request.title());
        }
        if (request.specialty() != null && !request.specialty().isBlank()) {
            specialist.setSpecialty(request.specialty());
        }
        if (request.bio() != null) {
            specialist.setBio(request.bio());
        }
        if (request.hourlyRate() != null) {
            specialist.setHourlyRate(request.hourlyRate());
        }
        if (request.isAvailable() != null) {
            specialist.setIsAvailable(request.isAvailable());
        }

        return SpecialistResponse.fromEntity(specialist);
    }
}
