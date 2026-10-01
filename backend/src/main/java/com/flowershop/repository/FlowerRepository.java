package com.flowershop.repository;

import com.flowershop.entity.Flower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FlowerRepository extends JpaRepository<Flower, Long>, JpaSpecificationExecutor<Flower> {
    long countByCategoryId(Long categoryId);
}
