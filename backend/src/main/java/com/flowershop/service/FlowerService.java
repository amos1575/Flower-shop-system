package com.flowershop.service;

import com.flowershop.dto.FlowerRequest;
import com.flowershop.dto.FlowerResponse;
import com.flowershop.entity.Category;
import com.flowershop.entity.Flower;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.CategoryRepository;
import com.flowershop.repository.FlowerRepository;
import com.flowershop.repository.FlowerSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlowerService {

    private final FlowerRepository flowerRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<FlowerResponse> search(String search, Long categoryId, boolean includeInactive, Pageable pageable) {
        Specification<Flower> spec = includeInactive ? Specification.where(null) : FlowerSpecifications.isActive();

        if (categoryId != null) {
            spec = spec.and(FlowerSpecifications.hasCategory(categoryId));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(FlowerSpecifications.nameContains(search));
        }

        return flowerRepository.findAll(spec, pageable).map(FlowerResponse::from);
    }

    @Transactional(readOnly = true)
    public FlowerResponse getById(Long id) {
        return FlowerResponse.from(findOrThrow(id));
    }

    @Transactional
    public FlowerResponse create(FlowerRequest request) {
        Category category = findCategoryOrThrow(request.getCategoryId());

        Flower flower = Flower.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();

        return FlowerResponse.from(flowerRepository.save(flower));
    }

    @Transactional
    public FlowerResponse update(Long id, FlowerRequest request) {
        Flower flower = findOrThrow(id);
        Category category = findCategoryOrThrow(request.getCategoryId());

        flower.setCategory(category);
        flower.setName(request.getName());
        flower.setDescription(request.getDescription());
        flower.setPrice(request.getPrice());
        flower.setStockQuantity(request.getStockQuantity());
        flower.setImageUrl(request.getImageUrl());

        return FlowerResponse.from(flowerRepository.save(flower));
    }

    @Transactional
    public FlowerResponse updateStock(Long id, int stockQuantity) {
        Flower flower = findOrThrow(id);
        flower.setStockQuantity(stockQuantity);
        return FlowerResponse.from(flowerRepository.save(flower));
    }

    @Transactional
    public void delete(Long id) {
        Flower flower = findOrThrow(id);
        flower.setActive(false);
        flowerRepository.save(flower);
    }

    @Transactional
    public FlowerResponse reactivate(Long id) {
        Flower flower = findOrThrow(id);
        flower.setActive(true);
        return FlowerResponse.from(flowerRepository.save(flower));
    }

    private Flower findOrThrow(Long id) {
        return flowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flower not found: " + id));
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
