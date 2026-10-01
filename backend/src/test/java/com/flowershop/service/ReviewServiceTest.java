package com.flowershop.service;

import com.flowershop.dto.CreateReviewRequest;
import com.flowershop.dto.ReviewResponse;
import com.flowershop.dto.ReviewSummaryResponse;
import com.flowershop.dto.UpdateReviewRequest;
import com.flowershop.entity.Flower;
import com.flowershop.entity.Review;
import com.flowershop.entity.Role;
import com.flowershop.entity.User;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.FlowerRepository;
import com.flowershop.repository.OrderDetailRepository;
import com.flowershop.repository.ReviewRepository;
import com.flowershop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private FlowerRepository flowerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User customer;
    private User otherCustomer;
    private Flower flower;

    private void initFixtures() {
        customer = User.builder().id(1L).fullName("Jane Doe").email("jane@example.com")
                .passwordHash("hash").role(Role.CUSTOMER).active(true).build();
        otherCustomer = User.builder().id(2L).fullName("John Roe").email("john@example.com")
                .passwordHash("hash").role(Role.CUSTOMER).active(true).build();
        flower = Flower.builder().id(10L).name("Red Rose").build();
    }

    @Test
    void createReview_whenEligible_savesAndReturnsReview() {
        initFixtures();
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Beautiful!");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
        when(flowerRepository.findById(10L)).thenReturn(Optional.of(flower));
        when(reviewRepository.existsByCustomerIdAndFlowerId(1L, 10L)).thenReturn(false);
        when(orderDetailRepository.existsDeliveredPurchase(10L, 1L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReviewResponse response = reviewService.createReview("jane@example.com", 10L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRating()).isEqualTo((short) 5);
        assertThat(response.getComment()).isEqualTo("Beautiful!");
    }

    @Test
    void createReview_whenUserNotFound_throwsResourceNotFoundException() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("ghost@example.com", 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReview_whenFlowerNotFound_throwsResourceNotFoundException() {
        initFixtures();
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
        when(flowerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("jane@example.com", 999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReview_whenDuplicate_throwsConflictApiException() {
        initFixtures();
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(4);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
        when(flowerRepository.findById(10L)).thenReturn(Optional.of(flower));
        when(reviewRepository.existsByCustomerIdAndFlowerId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview("jane@example.com", 10L, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenNotDelivered_throwsForbiddenApiException() {
        initFixtures();
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(4);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
        when(flowerRepository.findById(10L)).thenReturn(Optional.of(flower));
        when(reviewRepository.existsByCustomerIdAndFlowerId(1L, 10L)).thenReturn(false);
        when(orderDetailRepository.existsDeliveredPurchase(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview("jane@example.com", 10L, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_whenOwnerAndFound_updatesReview() {
        initFixtures();
        Review review = Review.builder().id(5L).customer(customer).flower(flower).rating((short) 2).comment("Meh").build();
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(5);
        request.setComment("Changed my mind, love it!");

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.updateReview("jane@example.com", 10L, 5L, request);

        assertThat(response.getRating()).isEqualTo((short) 5);
        assertThat(response.getComment()).isEqualTo("Changed my mind, love it!");
    }

    @Test
    void updateReview_whenReviewNotFound_throwsResourceNotFoundException() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(3);

        assertThatThrownBy(() -> reviewService.updateReview("jane@example.com", 10L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReview_whenReviewBelongsToDifferentFlower_throwsResourceNotFoundException() {
        initFixtures();
        Flower otherFlower = Flower.builder().id(20L).name("Tulip").build();
        Review review = Review.builder().id(5L).customer(customer).flower(otherFlower).rating((short) 2).build();
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(3);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview("jane@example.com", 10L, 5L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReview_whenNotOwner_throwsAccessDeniedException() {
        initFixtures();
        Review review = Review.builder().id(5L).customer(otherCustomer).flower(flower).rating((short) 4).build();
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(1);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview("jane@example.com", 10L, 5L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_whenOwner_deletesReview() {
        initFixtures();
        Review review = Review.builder().id(5L).customer(customer).flower(flower).rating((short) 4).build();
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        reviewService.deleteReview("jane@example.com", 10L, 5L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_whenNotOwner_throwsAccessDeniedException() {
        initFixtures();
        Review review = Review.builder().id(5L).customer(otherCustomer).flower(flower).rating((short) 4).build();
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview("jane@example.com", 10L, 5L))
                .isInstanceOf(AccessDeniedException.class);

        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void getReviewsForFlower_whenFlowerExists_returnsSummary() {
        initFixtures();
        Review review = Review.builder().id(5L).customer(customer).flower(flower).rating((short) 4).comment("Nice").build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);

        when(flowerRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findByFlowerId(10L, pageable)).thenReturn(page);
        when(reviewRepository.findAverageRatingByFlowerId(10L)).thenReturn(4.3333);
        when(reviewRepository.countByFlowerId(10L)).thenReturn(3L);

        ReviewSummaryResponse response = reviewService.getReviewsForFlower(10L, pageable);

        assertThat(response.getReviewCount()).isEqualTo(3L);
        assertThat(response.getAverageRating()).isEqualTo(4.3);
        assertThat(response.getReviews().getContent()).hasSize(1);
    }

    @Test
    void getReviewsForFlower_whenNoReviews_averageRatingIsZero() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(), pageable, 0);

        when(flowerRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findByFlowerId(10L, pageable)).thenReturn(page);
        when(reviewRepository.findAverageRatingByFlowerId(10L)).thenReturn(null);
        when(reviewRepository.countByFlowerId(10L)).thenReturn(0L);

        ReviewSummaryResponse response = reviewService.getReviewsForFlower(10L, pageable);

        assertThat(response.getAverageRating()).isEqualTo(0.0);
        assertThat(response.getReviewCount()).isEqualTo(0L);
    }

    @Test
    void getReviewsForFlower_whenFlowerNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(flowerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviewsForFlower(999L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
