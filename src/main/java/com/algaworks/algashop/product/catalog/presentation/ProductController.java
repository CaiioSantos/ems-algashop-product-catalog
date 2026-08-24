package com.algaworks.algashop.product.catalog.presentation;


import com.algaworks.algashop.product.catalog.application.product.management.ProductInput;
import com.algaworks.algashop.product.catalog.application.product.management.ProductManagementApplicationService;
import com.algaworks.algashop.product.catalog.application.product.query.*;
import com.algaworks.algashop.product.catalog.domain.model.CategoryNotFoundException;
import com.algaworks.algashop.product.catalog.infrastructure.security.SecurityAnnotations;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductManagementApplicationService productManagementApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityAnnotations.CanWriteProducts
    public ProductDetailOutput create(@RequestBody @Valid ProductInput input) {
        try {
             return productManagementApplicationService.create(input);

        } catch (CategoryNotFoundException e) {
            throw new UnprocessableEntityException(e.getMessage(), e);
        }
    }

    @GetMapping("/{productId}")
    @SecurityAnnotations.CanReadProducts
    public ResponseEntity<ProductDetailOutput> findById(@NotNull @PathVariable UUID productId) {
        var product = productQueryService.findById(productId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                .eTag("product:id:" + product.getId() + ":version:" + product.getVersion())
                .lastModified(product.getUpdatedAt().toInstant())
                .body(product);

    }

    @GetMapping
    @SecurityAnnotations.CanReadProducts
    public PageModel<ProductSummaryOutput> filter(ProductFilter filter) {
        return productQueryService.filter(filter);
    }

    @PutMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    @SecurityAnnotations.CanWriteProducts
    public  ProductDetailOutput update(@PathVariable UUID productId, @RequestBody @Valid ProductInput updateInput){
        return productManagementApplicationService.update(productId,updateInput);
    }

    @PutMapping("/{productId}/enable")
    @ResponseStatus(HttpStatus.OK)
    @SecurityAnnotations.CanWriteProducts
    public  void enable(@PathVariable UUID productId){
        productManagementApplicationService.enable(productId);
    }

    @DeleteMapping("/{productId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityAnnotations.CanWriteProducts
    public  void disable(@PathVariable UUID productId){
        productManagementApplicationService.disable(productId);
    }

    @PostMapping("/{productId}/restock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityAnnotations.CanRestockProducts
    public void restock(@PathVariable UUID productId, @RequestBody @Valid ProductQuantityModel productQuantityModel) {
        productManagementApplicationService.restock(productId,productQuantityModel.getQuantity());
    }

    @PostMapping("/{productId}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityAnnotations.CanWriteProducts
    public void withdraw(@PathVariable UUID productId, @RequestBody @Valid ProductQuantityModel productQuantityModel) {
        productManagementApplicationService.withdraw(productId,productQuantityModel.getQuantity());
    }
}