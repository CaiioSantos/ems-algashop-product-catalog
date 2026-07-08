package com.algaworks.algashop.product.catalog.application.product.management;

import com.algaworks.algashop.product.catalog.application.ResourceNotFoundException;
import com.algaworks.algashop.product.catalog.application.product.query.ProductDetailOutput;
import com.algaworks.algashop.product.catalog.domain.model.CategoryNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.ProductNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.category.Category;
import com.algaworks.algashop.product.catalog.domain.model.category.CategoryRepository;
import com.algaworks.algashop.product.catalog.domain.model.product.*;
import com.algaworks.algashop.product.catalog.infrastructure.utility.mapper.Mapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductManagementApplicationService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;

    private final StockService stockService;

    private final Mapper mapper;
    @CachePut(value = "algashop:products:v1", key = "#result.id", condition = "#input.enabled == true")
    public ProductDetailOutput create(ProductInput input) {
        Product product  = this.mapToproduct(input);
            this.productRepository.save(product);
        return this.mapper.convert(product, ProductDetailOutput.class);
    }

    private Product mapToproduct(ProductInput input) {
        Category category = this.findCategory(input.getCategoryId());
        return Product.builder()
                .name(input.getName())
                .brand(input.getBrand())
                .description(input.getDescription())
                .regularPrice(input.getRegularPrice())
                .salePrice(input.getSalePrice())
                .enabled(input.getEnabled())
                .category(category)
                .build();
    }

    @CachePut(value = "algashop:products:v1", key = "#result.id", condition = "#input.enabled == true")
    @CacheEvict( cacheNames = "algashop:products:v1", key = "#productId", condition = "#input.enabled == false")
    public ProductDetailOutput update(UUID productId, ProductInput input) {
        Product product = this.findProduct(productId);
        Category category = this.findCategory(input.getCategoryId());
        this.updateproduct(product, input);
        product.setCategory(category);
        this.productRepository.save(product);
        return this.mapper.convert(product, ProductDetailOutput.class);
    }

    @Transactional
    public void restock(UUID productId,int quantity) {
        Product product = findProduct(productId);
        StockMovement stockMovement = stockService.restock(product, quantity);
        stockMovementRepository.save(stockMovement);
    }
    @Transactional
    public void withdraw(UUID productId,int quantity) {
        Product product = findProduct(productId);
        StockMovement stockMovement = stockService.withdraw(product, quantity);
        stockMovementRepository.save(stockMovement);
    }

    private void updateproduct(Product product, ProductInput input) {
        product.setName(input.getName());
        product.setBrand(input.getBrand());
        product.setDescription(input.getDescription());
        product.setEnabled(input.getEnabled());
        product.changePrice(input.getRegularPrice(), input.getSalePrice());
    }

    @CacheEvict( cacheNames = "algashop:products:v1", key = "#productId")
    public void disable(UUID productId) {
        Product product = this.findProduct(productId);
        product.disable();
        this.productRepository.save(product);

    }

    @CacheEvict( cacheNames = "algashop:products:v1", key = "#productId")
    public void enable(UUID productId) {
        Product product = this.findProduct(productId);
        product.enable();
        this.productRepository.save(product);
    }

    private Category findCategory(@NotNull UUID categoryId) {
        return this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
    private Product findProduct(UUID productId) {
        return this.productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}