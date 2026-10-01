package com.flowershop.service;

import com.flowershop.dto.CreateReviewRequest;
import com.flowershop.dto.ReviewResponse;
import com.flowershop.dto.ReviewSummaryResponse;
import com.flowershop.dto.UpdateReviewRequest;
import com.flowershop.entity.Flower;
import com.flowershop.entity.Review;
import com.flowershop.entity.User;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.FlowerRepository;
import com.flowershop.repository.OrderDetailRepository;
import com.flowershop.repository.ReviewRepository;
import com.flowershop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Transactional
    public ReviewResponse createReview(String customerEmail, Long flowerId, CreateReviewRequest request) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerEmail));
        Flower flower = flowerRepository.findById(flowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Flower not found: " + flowerId));

        if (reviewRepository.existsByCustomerIdAndFlowerId(customer.getId(), flowerId)) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already reviewed this flower");
        }
        if (!orderDetailRepository.existsDeliveredPurchase(flowerId, customer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You can only review flowers from orders that have been delivered to you");
        }

        Review review = Review.builder()
                .customer(customer)
                .flower(flower)
                .rating(request.getRating().shortValue())
                .comment(request.getComment())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(String customerEmail, Long flowerId, Long reviewId, UpdateReviewRequest request) {
        Review review = findOwnedReviewOrThrow(customerEmail, flowerId, reviewId);

        review.setRating(request.getRating().shortValue());
        review.setComment(request.getComment());

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(String customerEmail, Long flowerId, Long reviewId) {
        Review review = findOwnedReviewOrThrow(customerEmail, flowerId, reviewId);
        reviewRepository.delete(review);
    }

    private Review findOwnedReviewOrThrow(String customerEmail, Long flowerId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getFlower().getId().equals(flowerId)) {
            throw new ResourceNotFoundException("Review not found: " + reviewId);
        }
        if (!review.getCustomer().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new AccessDeniedException("You can only modify your own review");
        }

        return review;
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewsForFlower(Long flowerId, Pageable pageable) {
        if (!flowerRepository.existsById(flowerId)) {
            throw new ResourceNotFoundException("Flower not found: " + flowerId);
        }

        Page<ReviewResponse> reviews = reviewRepository.findByFlowerId(flowerId, pageable).map(ReviewResponse::from);
        Double average = reviewRepository.findAverageRatingByFlowerId(flowerId);
        long count = reviewRepository.countByFlowerId(flowerId);

        return ReviewSummaryResponse.builder()
                .averageRating(average != null ? Math.round(average * 10.0) / 10.0 : 0.0)
                .reviewCount(count)
                .reviews(reviews)
                .build();
    }
}
