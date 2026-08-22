package com.taskmaster.task.adapter.out;

import com.taskmaster.task.application.dto.TaskFilterCriteria;
import com.taskmaster.task.domain.model.Task;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic JPA Criteria specification builder for filtering and searching tasks.
 */
public final class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> withFilter(TaskFilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Soft deletion filter (exclude deleted by default)
            if (criteria == null || criteria.includeDeleted() == null || !criteria.includeDeleted()) {
                predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            }

            if (criteria == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Status filter
            if (criteria.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.status()));
            }

            // 3. Priority filter
            if (criteria.priority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), criteria.priority()));
            }

            // 4. Assignee filter
            if (criteria.assigneeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignee").get("id"), criteria.assigneeId()));
            }

            // 5. Creator filter
            if (criteria.creatorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("creator").get("id"), criteria.creatorId()));
            }

            // 6. Team filter
            if (criteria.teamId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("teamId"), criteria.teamId()));
            }

            // 7. Due date range filters
            if (criteria.dueDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), criteria.dueDateFrom()));
            }
            if (criteria.dueDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), criteria.dueDateTo()));
            }

            // 8. Label filter
            if (criteria.label() != null && !criteria.label().isBlank()) {
                Join<Task, String> labelsJoin = root.join("labels");
                predicates.add(criteriaBuilder.equal(labelsJoin, criteria.label().trim()));
            }

            // 9. Full text / keyword search
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String searchPattern = "%" + criteria.search().trim().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    searchPattern
                );
                Predicate descriptionMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")),
                    searchPattern
                );
                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
