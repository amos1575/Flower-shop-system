package com.flowershop.service;

import com.flowershop.dto.CategoryRequest;
import com.flowershop.dto.CategoryResponse;
import com.flowershop.entity.Category;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.CategoryRepository;
import com.flowershop.repository.FlowerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FlowerRepository flowerRepository;

    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(c -> CategoryResponse.from(c, flowerRepository.countByCategoryId(c.getId())))
                .toList();
    }

    public CategoryResponse getById(Long id) {
        Category category = findOrThrow(id);
        return CategoryResponse.from(category, flowerRepository.countByCategoryId(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Category '" + request.getName() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return CategoryResponse.from(categoryRepository.save(category), 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return CategoryResponse.from(categoryRepository.save(category), flowerRepository.countByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);
        long flowerCount = flowerRepository.countByCategoryId(id);
        if (flowerCount > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete category '" + category.getName() + "' — it still has " + flowerCount + " flower(s) assigned");
        }
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
