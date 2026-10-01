package com.flowershop.dto;

import com.flowershop.entity.Flower;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class FlowerResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;

    public static FlowerResponse from(Flower flower) {
        return FlowerResponse.builder()
                .id(flower.getId())
                .categoryId(flower.getCategory().getId())
                .categoryName(flower.getCategory().getName())
                .name(flower.getName())
                .description(flower.getDescription())
                .price(flower.getPrice())
                .stockQuantity(flower.getStockQuantity())
                .imageUrl(flower.getImageUrl())
                .active(flower.isActive())
                .createdAt(flower.getCreatedAt())
                .build();
    }
}
