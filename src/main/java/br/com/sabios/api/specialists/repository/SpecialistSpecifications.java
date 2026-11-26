package br.com.sabios.api.specialists.repository;

import br.com.sabios.api.agenda.domain.Availability;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.dto.SpecialistFilter;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class SpecialistSpecifications {

    private SpecialistSpecifications() {
    }

    /** Somente especialistas com perfil disponível que atendem a todos os filtros informados. */
    public static Specification<Specialist> matching(SpecialistFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isAvailable")));

            if (hasText(filter.specialty())) {
                predicates.add(cb.like(cb.lower(root.get("specialty")), contains(filter.specialty()), '\\'));
            }

            if (hasText(filter.search())) {
                String pattern = contains(filter.search());
                Expression<String> name = cb.lower(root.join("user", JoinType.INNER).get("name"));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(name, pattern, '\\'),
                        cb.like(cb.lower(root.get("specialty")), pattern, '\\')
                ));
            }

            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.minRating()));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("hourlyRate"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("hourlyRate"), filter.maxPrice()));
            }

            if (filter.availableDay() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Availability> availability = sub.from(Availability.class);
                sub.select(cb.literal(1L)).where(
                        cb.equal(availability.get("specialist"), root),
                        cb.equal(availability.get("dayOfWeek"), filter.availableDay())
                );
                predicates.add(cb.exists(sub));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Padrão LIKE "contém", escapando os curingas digitados pelo usuário. */
    private static String contains(String value) {
        String escaped = value.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
