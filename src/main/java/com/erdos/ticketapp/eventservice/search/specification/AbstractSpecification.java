package com.erdos.ticketapp.eventservice.search.specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;

//TODO: A több státusz (IN) szűrés bevezetése

@Slf4j
@NoArgsConstructor
public abstract class AbstractSpecification<C, T> {

    private static final String QUERY = "query";

    public Specification<@NonNull T> build(C criteria) {
        Specification<@NonNull T> spec = Specification.unrestricted();

        for (Field field : FieldUtils.getAllFields(criteria.getClass())) {
            field.setAccessible(true);

            Object value;
            try {
                value = field.get(criteria);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read criteria field: " + field.getName(), e);
            }

            if (value == null || (value instanceof String s && s.isBlank())) {
                continue;
            }

            String criteriaFieldName = field.getName();

//            FieldName annotation = field.getAnnotation(FieldName.class);
//            String entityPath = annotation == null || annotation.name().isBlank() ? criteriaFieldName: annotation.name();

            // skip helper/global fields if needed
            if (QUERY.equals(criteriaFieldName)) {
                spec = spec.and(buildPredicate("event.name", value));
            } else {
                spec = spec.and(buildPredicate(criteriaFieldName, value));
            }
        }

        return spec;
    }

    protected Specification<@NonNull T> buildPredicate(String entityPath, Object value) {
        return (root, query, cb) -> {
            Path<?> path;

            try {
                path = resolvePath(root, entityPath);
            } catch(Exception e) {
                log.error("Could net get the Path for: {}", entityPath);
                return cb.conjunction();
            }

            if (value instanceof String s) {
                return cb.like(cb.lower(path.as(String.class)), "%" + s.toLowerCase() + "%");
            }

            return cb.equal(path, value);
        };
    }

    private Path<?> resolvePath(Root<?> root, String path) {
        Path<?> current = root;
        for (String part : path.split("\\.")) {
            current = current.get(part);
        }
        return current;
    }
}