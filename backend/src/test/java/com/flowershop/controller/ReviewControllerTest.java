package com.flowershop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowershop.config.SecurityConfig;
import com.flowershop.dto.CreateReviewRequest;
import com.flowershop.dto.ReviewResponse;
import com.flowershop.dto.ReviewSummaryResponse;
import com.flowershop.dto.UpdateReviewRequest;
import com.flowershop.exception.ApiException;
import com.flowershop.security.CustomUserDetailsService;
import com.flowershop.security.JwtService;
import com.flowershop.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    // Required so SecurityConfig / JwtAuthenticationFilter can be constructed in the slice context.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private ReviewResponse sampleReview() {
        return ReviewResponse.builder()
                .id(1L)
                .flowerId(10L)
                .customerId(1L)
                .customerName("Jane Doe")
                .rating((short) 5)
                .comment("Lovely!")
                .build();
    }

    @Test
    void list_isPublic_returns200() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewResponse> page = new PageImpl<>(List.of(sampleReview()), pageable, 1);
        ReviewSummaryResponse summary = ReviewSummaryResponse.builder()
                .averageRating(5.0)
                .reviewCount(1)
                .reviews(page)
                .build();
        when(reviewService.getReviewsForFlower(eq(10L), any(Pageable.class))).thenReturn(summary);

        mockMvc.perform(get("/api/flowers/10/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.reviews.content[0].comment").value("Lovely!"));
    }

    @Test
    @WithMockUser(username = "jane@example.com", roles = "CUSTOMER")
    void create_asCustomer_returns201() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Lovely!");

        when(reviewService.createReview(eq("jane@example.com"), eq(10L), any(CreateReviewRequest.class)))
                .thenReturn(sampleReview());

        mockMvc.perform(post("/api/flowers/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("Lovely!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns403() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        mockMvc.perform(post("/api/flowers/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "jane@example.com", roles = "CUSTOMER")
    void create_withInvalidRating_returns400() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(7);

        mockMvc.perform(post("/api/flowers/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "jane@example.com", roles = "CUSTOMER")
    void create_whenNotDelivered_returns403() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        when(reviewService.createReview(eq("jane@example.com"), eq(10L), any(CreateReviewRequest.class)))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "You can only review flowers from orders that have been delivered to you"));

        mockMvc.perform(post("/api/flowers/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jane@example.com", roles = "CUSTOMER")
    void create_whenDuplicate_returns409() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        when(reviewService.createReview(eq("jane@example.com"), eq(10L), any(CreateReviewRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "You have already reviewed this flower"));

        mockMvc.perform(post("/api/flowers/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "CUSTOMER")
    void update_whenNotOwner_returns403() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(1);
        request.setComment("Not mine");

        when(reviewService.updateReview(eq("john@example.com"), eq(10L), eq(1L), any(UpdateReviewRequest.class)))
                .thenThrow(new AccessDeniedException("You can only modify your own review"));

        mockMvc.perform(put("/api/flowers/10/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jane@example.com", roles = "CUSTOMER")
    void delete_asOwner_returns204() throws Exception {
        doNothing().when(reviewService).deleteReview("jane@example.com", 10L, 1L);

        mockMvc.perform(delete("/api/flowers/10/reviews/1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview("jane@example.com", 10L, 1L);
    }
}
