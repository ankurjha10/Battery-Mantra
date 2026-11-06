package com.api.batterymantra.repository.specification;

import com.api.batterymantra.entity.BlogPost;
import com.api.batterymantra.entity.enums.BlogStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BlogPostSpecification {

    private BlogPostSpecification() {
        // utility class
    }

    public static Specification<BlogPost> filter(
            String category,
            String keyword,
            String tag,
            Boolean isFeatured,
            BlogStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (isFeatured != null) {
                predicates.add(cb.equal(root.get("isFeatured"), isFeatured));
            }

            if (tag != null && !tag.trim().isEmpty()) {
                String tagPattern = "%" + tag.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("tags")), tagPattern));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate excerptMatch = cb.like(cb.lower(root.get("excerpt")), pattern);
                Predicate tagsMatch = cb.like(cb.lower(root.get("tags")), pattern);
                predicates.add(cb.or(titleMatch, excerptMatch, tagsMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

