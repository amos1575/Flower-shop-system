package com.flowershop.dto;

import com.flowershop.entity.Category;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private long flowerCount;

    public static CategoryResponse from(Category category, long flowerCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .flowerCount(flowerCount)
                .build();
    }
}
