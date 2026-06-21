package com.algaworks.algashop.product.catalog.presentation;


import com.algaworks.algashop.product.catalog.application.product.management.ProductInput;
import com.algaworks.algashop.product.catalog.application.product.management.ProductManagementApplicationService;
import com.algaworks.algashop.product.catalog.application.product.query.*;
import com.algaworks.algashop.product.catalog.domain.model.CategoryNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductManagementApplicationService productManagementApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailOutput create(@RequestBody @Valid ProductInput input) {
        UUID productId;
        try {
             productId = productManagementApplicationService.create(input);

        } catch (CategoryNotFoundException e) {
            throw new UnprocessableEntityException(e.getMessage(), e);
        }
        return productQueryService.findById(productId);
    }

    @GetMapping("/{productId}")
    public ProductDetailOutput findById(@PathVariable UUID productId) {
        return productQueryService.findById(productId);
    }

    @GetMapping
    public PageModel<ProductSummaryOutput> filter(ProductFilter filter) {
        return productQueryService.filter(filter);
    }

    @PutMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public  ProductDetailOutput update(@PathVariable UUID productId, @RequestBody @Valid ProductInput updateInput){
        productManagementApplicationService.update(productId,updateInput);
        return productQueryService.findById(productId);
    }

    @PutMapping("/{productId}/enable")
    @ResponseStatus(HttpStatus.OK)
    public  void enable(@PathVariable UUID productId){
        productManagementApplicationService.enable(productId);
    }

    @DeleteMapping("/{productId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public  void disable(@PathVariable UUID productId){
        productManagementApplicationService.disable(productId);
    }

    @PostMapping("/{productId}/restock")
    @ResponseStatus(HttpStatus.OK)
    public void restock(@PathVariable UUID productId, @RequestBody @Valid ProductQuantityModel productQuantityModel) {
        productManagementApplicationService.restock(productId,productQuantityModel.getQuantity());
    }

    @PostMapping("/{productId}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable UUID productId, @RequestBody @Valid ProductQuantityModel productQuantityModel) {
        productManagementApplicationService.withdraw(productId,productQuantityModel.getQuantity());
    }
}