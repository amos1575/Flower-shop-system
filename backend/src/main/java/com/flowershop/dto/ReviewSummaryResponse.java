package com.flowershop.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class ReviewSummaryResponse {
    private double averageRating;
    private long reviewCount;
    private Page<ReviewResponse> reviews;
}
