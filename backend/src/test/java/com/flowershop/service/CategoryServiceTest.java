package com.flowershop.service;

import com.flowershop.dto.CategoryRequest;
import com.flowershop.dto.CategoryResponse;
import com.flowershop.entity.Category;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.CategoryRepository;
import com.flowershop.repository.FlowerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FlowerRepository flowerRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listAll_returnsAllCategoriesWithFlowerCounts() {
        Category roses = Category.builder().id(1L).name("Roses").description("Red roses").build();
        Category tulips = Category.builder().id(2L).name("Tulips").description("Dutch tulips").build();
        when(categoryRepository.findAll()).thenReturn(List.of(roses, tulips));
        when(flowerRepository.countByCategoryId(1L)).thenReturn(3L);
        when(flowerRepository.countByCategoryId(2L)).thenReturn(0L);

        List<CategoryResponse> result = categoryService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Roses");
        assertThat(result.get(0).getFlowerCount()).isEqualTo(3L);
        assertThat(result.get(1).getFlowerCount()).isEqualTo(0L);
    }

    @Test
    void getById_whenFound_returnsCategoryResponse() {
        Category category = Category.builder().id(1L).name("Roses").description("Red roses").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(flowerRepository.countByCategoryId(1L)).thenReturn(5L);

        CategoryResponse response = categoryService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Roses");
        assertThat(response.getFlowerCount()).isEqualTo(5L);
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_whenNameIsUnique_savesAndReturnsCategory() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Lilies");
        request.setDescription("Fresh lilies");

        when(categoryRepository.existsByNameIgnoreCase("Lilies")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Lilies");
        assertThat(response.getFlowerCount()).isEqualTo(0L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_whenNameAlreadyExists_throwsConflictApiException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Roses");

        when(categoryRepository.existsByNameIgnoreCase("Roses")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_whenFound_updatesAndReturnsCategory() {
        Category existing = Category.builder().id(1L).name("Old Name").description("Old desc").build();
        CategoryRequest request = new CategoryRequest();
        request.setName("New Name");
        request.setDescription("New desc");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(flowerRepository.countByCategoryId(1L)).thenReturn(2L);

        CategoryResponse response = categoryService.update(1L, request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getDescription()).isEqualTo("New desc");
        assertThat(response.getFlowerCount()).isEqualTo(2L);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        CategoryRequest request = new CategoryRequest();
        request.setName("Whatever");

        assertThatThrownBy(() -> categoryService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenNoFlowersAssigned_deletesCategory() {
        Category category = Category.builder().id(1L).name("Roses").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(flowerRepository.countByCategoryId(1L)).thenReturn(0L);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_whenCategoryHasFlowers_throwsConflictApiException() {
        Category category = Category.builder().id(1L).name("Roses").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(flowerRepository.countByCategoryId(1L)).thenReturn(4L);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_whenNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(flowerRepository, never()).countByCategoryId(anyLong());
    }
}
