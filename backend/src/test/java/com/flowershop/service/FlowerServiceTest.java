package com.flowershop.service;

import com.flowershop.dto.FlowerRequest;
import com.flowershop.dto.FlowerResponse;
import com.flowershop.entity.Category;
import com.flowershop.entity.Flower;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.CategoryRepository;
import com.flowershop.repository.FlowerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowerServiceTest {

    @Mock
    private FlowerRepository flowerRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private FlowerService flowerService;

    private Category category;
    private Flower flower;

    private void initFixtures() {
        category = Category.builder().id(1L).name("Roses").description("Red roses").build();
        flower = Flower.builder()
                .id(1L)
                .category(category)
                .name("Red Rose")
                .description("A nice rose")
                .price(new BigDecimal("9.99"))
                .stockQuantity(10)
                .active(true)
                .build();
    }

    @Test
    void search_returnsMappedPageFromRepository() {
        initFixtures();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Flower> page = new PageImpl<>(List.of(flower), pageable, 1);
        when(flowerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<FlowerResponse> result = flowerService.search(null, null, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Red Rose");
        verify(flowerRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getById_whenFound_returnsFlowerResponse() {
        initFixtures();
        when(flowerRepository.findById(1L)).thenReturn(Optional.of(flower));

        FlowerResponse response = flowerService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Roses");
        assertThat(response.getPrice()).isEqualByComparingTo("9.99");
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        when(flowerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_whenCategoryExists_savesAndReturnsFlower() {
        initFixtures();
        FlowerRequest request = new FlowerRequest();
        request.setCategoryId(1L);
        request.setName("White Lily");
        request.setDescription("Fragrant");
        request.setPrice(new BigDecimal("12.50"));
        request.setStockQuantity(5);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(flowerRepository.save(any(Flower.class))).thenAnswer(invocation -> {
            Flower f = invocation.getArgument(0);
            f.setId(42L);
            return f;
        });

        FlowerResponse response = flowerService.create(request);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getName()).isEqualTo("White Lily");
        assertThat(response.isActive()).isTrue();
        verify(flowerRepository).save(any(Flower.class));
    }

    @Test
    void create_whenCategoryUnknown_throwsResourceNotFoundException() {
        FlowerRequest request = new FlowerRequest();
        request.setCategoryId(999L);
        request.setName("Ghost Flower");
        request.setPrice(new BigDecimal("5.00"));
        request.setStockQuantity(1);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(flowerRepository, never()).save(any());
    }

    @Test
    void update_whenFoundAndCategoryExists_updatesFlower() {
        initFixtures();
        Category newCategory = Category.builder().id(2L).name("Tulips").build();
        FlowerRequest request = new FlowerRequest();
        request.setCategoryId(2L);
        request.setName("Updated Rose");
        request.setDescription("Updated desc");
        request.setPrice(new BigDecimal("15.00"));
        request.setStockQuantity(20);

        when(flowerRepository.findById(1L)).thenReturn(Optional.of(flower));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(flowerRepository.save(any(Flower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlowerResponse response = flowerService.update(1L, request);

        assertThat(response.getName()).isEqualTo("Updated Rose");
        assertThat(response.getCategoryName()).isEqualTo("Tulips");
        assertThat(response.getPrice()).isEqualByComparingTo("15.00");
    }

    @Test
    void update_whenFlowerNotFound_throwsResourceNotFoundException() {
        FlowerRequest request = new FlowerRequest();
        request.setCategoryId(1L);
        request.setName("Whatever");
        request.setPrice(BigDecimal.ONE);
        request.setStockQuantity(1);

        when(flowerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenCategoryUnknown_throwsResourceNotFoundException() {
        initFixtures();
        FlowerRequest request = new FlowerRequest();
        request.setCategoryId(999L);
        request.setName("Whatever");
        request.setPrice(BigDecimal.ONE);
        request.setStockQuantity(1);

        when(flowerRepository.findById(1L)).thenReturn(Optional.of(flower));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(flowerRepository, never()).save(any());
    }

    @Test
    void updateStock_whenFound_updatesStockQuantity() {
        initFixtures();
        when(flowerRepository.findById(1L)).thenReturn(Optional.of(flower));
        when(flowerRepository.save(any(Flower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlowerResponse response = flowerService.updateStock(1L, 50);

        assertThat(response.getStockQuantity()).isEqualTo(50);
    }

    @Test
    void updateStock_whenNotFound_throwsResourceNotFoundException() {
        when(flowerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.updateStock(99L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenFound_softDeletesFlower() {
        initFixtures();
        when(flowerRepository.findById(1L)).thenReturn(Optional.of(flower));
        when(flowerRepository.save(any(Flower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        flowerService.delete(1L);

        assertThat(flower.isActive()).isFalse();
        verify(flowerRepository).save(flower);
    }

    @Test
    void delete_whenNotFound_throwsResourceNotFoundException() {
        when(flowerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reactivate_whenFound_setsActiveTrue() {
        initFixtures();
        flower.setActive(false);
        when(flowerRepository.findById(1L)).thenReturn(Optional.of(flower));
        when(flowerRepository.save(any(Flower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlowerResponse response = flowerService.reactivate(1L);

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void reactivate_whenNotFound_throwsResourceNotFoundException() {
        when(flowerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flowerService.reactivate(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
