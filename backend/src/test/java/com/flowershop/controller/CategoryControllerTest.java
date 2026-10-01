package com.flowershop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowershop.config.SecurityConfig;
import com.flowershop.dto.CategoryRequest;
import com.flowershop.dto.CategoryResponse;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.security.CustomUserDetailsService;
import com.flowershop.security.JwtService;
import com.flowershop.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // Required so SecurityConfig / JwtAuthenticationFilter can be constructed in the slice context.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listAll_isPublic_returns200() throws Exception {
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Roses").description("desc").flowerCount(3).build();
        when(categoryService.listAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Roses"))
                .andExpect(jsonPath("$[0].flowerCount").value(3));
    }

    @Test
    void getById_whenFound_returns200() throws Exception {
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Roses").description("desc").flowerCount(3).build();
        when(categoryService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Roses"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(categoryService.getById(99L)).thenThrow(new ResourceNotFoundException("Category not found: 99"));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Lilies");
        request.setDescription("Fresh lilies");

        CategoryResponse response = CategoryResponse.builder().id(5L).name("Lilies").description("Fresh lilies").flowerCount(0).build();
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Lilies"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returns403() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Lilies");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).create(any());
    }

    @Test
    void create_whenUnauthenticated_returns403() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Lilies");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("   ");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenDuplicateName_returns409() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Roses");

        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Category 'Roses' already exists"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated");
        request.setDescription("Updated desc");

        CategoryResponse response = CategoryResponse.builder().id(1L).name("Updated").description("Updated desc").flowerCount(2).build();
        when(categoryService.update(eq(1L), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void update_asCustomer_returns403() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated");

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void delete_asCustomer_returns403() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_whenCategoryHasFlowers_returns409() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "Cannot delete category 'Roses' — it still has 2 flower(s) assigned"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isConflict());
    }
}
