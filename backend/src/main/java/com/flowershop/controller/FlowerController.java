package com.flowershop.controller;

import com.flowershop.dto.FlowerRequest;
import com.flowershop.dto.FlowerResponse;
import com.flowershop.dto.StockUpdateRequest;
import com.flowershop.service.FlowerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flowers")
@RequiredArgsConstructor
public class FlowerController {

    private final FlowerService flowerService;

    @GetMapping
    public ResponseEntity<Page<FlowerResponse>> search(@RequestParam(required = false) String search,
                                                         @RequestParam(required = false) Long category,
                                                         @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
                                                         Authentication authentication,
                                                         Pageable pageable) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        return ResponseEntity.ok(flowerService.search(search, category, includeInactive && isAdmin, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlowerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flowerService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlowerResponse> create(@Valid @RequestBody FlowerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flowerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlowerResponse> update(@PathVariable Long id, @Valid @RequestBody FlowerRequest request) {
        return ResponseEntity.ok(flowerService.update(id, request));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlowerResponse> updateStock(@PathVariable Long id,
                                                       @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(flowerService.updateStock(id, request.getStockQuantity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flowerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlowerResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(flowerService.reactivate(id));
    }
}
