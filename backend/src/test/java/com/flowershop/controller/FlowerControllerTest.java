package com.flowershop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowershop.config.SecurityConfig;
import com.flowershop.dto.FlowerRequest;
import com.flowershop.dto.FlowerResponse;
import com.flowershop.dto.StockUpdateRequest;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.security.CustomUserDetailsService;
import com.flowershop.security.JwtService;
import com.flowershop.service.FlowerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlowerController.class)
@Import(SecurityConfig.class)
class FlowerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlowerService flowerService;

    // Required so SecurityConfig / JwtAuthenticationFilter can be constructed in the slice context.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private FlowerResponse sampleFlower() {
        return sampleFlower(10);
    }

    private FlowerResponse sampleFlower(int stockQuantity) {
        return FlowerResponse.builder()
                .id(1L)
                .categoryId(1L)
                .categoryName("Roses")
                .name("Red Rose")
                .description("A nice rose")
                .price(new BigDecimal("9.99"))
                .stockQuantity(stockQuantity)
                .active(true)
                .build();
    }

    @Test
    void search_isPublic_returns200() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<FlowerResponse> page = new PageImpl<>(List.of(sampleFlower()), pageable, 1);
        when(flowerService.search(any(), any(), eq(false), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/flowers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Red Rose"));
    }

    @Test
    void getById_whenFound_returns200() throws Exception {
        when(flowerService.getById(1L)).thenReturn(sampleFlower());

        mockMvc.perform(get("/api/flowers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(flowerService.getById(99L)).thenThrow(new ResourceNotFoundException("Flower not found: 99"));

        mockMvc.perform(get("/api/flowers/99"))
                .andExpect(status().isNotFound());
    }

    private FlowerRequest validRequest() {
        FlowerRequest request = new FlowerRequest();
        request.setCategoryId(1L);
        request.setName("White Lily");
        request.setDescription("Fragrant");
        request.setPrice(new BigDecimal("12.50"));
        request.setStockQuantity(5);
        return request;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(flowerService.create(any(FlowerRequest.class))).thenReturn(sampleFlower());

        mockMvc.perform(post("/api/flowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/flowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(flowerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withNegativePrice_returns400() throws Exception {
        FlowerRequest request = validRequest();
        request.setPrice(new BigDecimal("-5"));

        mockMvc.perform(post("/api/flowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(flowerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_returns400() throws Exception {
        FlowerRequest request = validRequest();
        request.setName("  ");

        mockMvc.perform(post("/api/flowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(flowerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withMissingCategoryId_returns400() throws Exception {
        FlowerRequest request = validRequest();
        request.setCategoryId(null);

        mockMvc.perform(post("/api/flowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(flowerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenCategoryUnknown_returns404() throws Exception {
        when(flowerService.create(any(FlowerRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 999"));

        FlowerRequest request = validRequest();
        request.setCategoryId(999L);

        mockMvc.perform(post("/api/flowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(flowerService.update(eq(1L), any(FlowerRequest.class))).thenReturn(sampleFlower());

        mockMvc.perform(put("/api/flowers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_asAdmin_returns200() throws Exception {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setStockQuantity(25);

        when(flowerService.updateStock(1L, 25)).thenReturn(sampleFlower(25));

        mockMvc.perform(patch("/api/flowers/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(25));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_withNegativeStock_returns400() throws Exception {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setStockQuantity(-1);

        mockMvc.perform(patch("/api/flowers/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(flowerService, never()).updateStock(any(), anyInt());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateStock_asCustomer_returns403() throws Exception {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setStockQuantity(25);

        mockMvc.perform(patch("/api/flowers/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(flowerService, never()).updateStock(any(), anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(flowerService).delete(1L);

        mockMvc.perform(delete("/api/flowers/1"))
                .andExpect(status().isNoContent());

        verify(flowerService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void delete_asCustomer_returns403() throws Exception {
        mockMvc.perform(delete("/api/flowers/1"))
                .andExpect(status().isForbidden());

        verify(flowerService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reactivate_asAdmin_returns200() throws Exception {
        when(flowerService.reactivate(1L)).thenReturn(sampleFlower());

        mockMvc.perform(patch("/api/flowers/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void reactivate_asCustomer_returns403() throws Exception {
        mockMvc.perform(patch("/api/flowers/1/reactivate"))
                .andExpect(status().isForbidden());

        verify(flowerService, never()).reactivate(any());
    }
}
