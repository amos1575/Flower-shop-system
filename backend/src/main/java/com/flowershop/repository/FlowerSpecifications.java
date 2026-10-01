package com.flowershop.repository;

import com.flowershop.entity.Flower;
import org.springframework.data.jpa.domain.Specification;

public final class FlowerSpecifications {

    private FlowerSpecifications() {
    }

    public static Specification<Flower> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Flower> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Flower> nameContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }
}
