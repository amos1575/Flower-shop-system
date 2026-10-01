package com.flowershop.controller;

import com.flowershop.dto.CreateReviewRequest;
import com.flowershop.dto.ReviewResponse;
import com.flowershop.dto.ReviewSummaryResponse;
import com.flowershop.dto.UpdateReviewRequest;
import com.flowershop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flowers/{flowerId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ReviewSummaryResponse> list(@PathVariable Long flowerId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForFlower(flowerId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long flowerId,
                                                  @Valid @RequestBody CreateReviewRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(authentication.getName(), flowerId, request));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long flowerId,
                                                  @PathVariable Long reviewId,
                                                  @Valid @RequestBody UpdateReviewRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(reviewService.updateReview(authentication.getName(), flowerId, reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> delete(@PathVariable Long flowerId,
                                        @PathVariable Long reviewId,
                                        Authentication authentication) {
        reviewService.deleteReview(authentication.getName(), flowerId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
