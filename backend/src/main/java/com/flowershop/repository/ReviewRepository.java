package com.flowershop.repository;

import com.flowershop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByFlowerId(Long flowerId, Pageable pageable);
    boolean existsByCustomerIdAndFlowerId(Long customerId, Long flowerId);
    long countByFlowerId(Long flowerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.flower.id = :flowerId")
    Double findAverageRatingByFlowerId(@Param("flowerId") Long flowerId);
}
