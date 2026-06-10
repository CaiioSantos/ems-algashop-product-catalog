package com.algaworks.algashop.product.catalog.application.product.management;

import com.algaworks.algashop.product.catalog.application.ResourceNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.CategoryNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.ProductNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.category.Category;
import com.algaworks.algashop.product.catalog.domain.model.category.CategoryRepository;
import com.algaworks.algashop.product.catalog.domain.model.product.Product;
import com.algaworks.algashop.product.catalog.domain.model.product.ProductRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductManagementApplicationService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public UUID create(ProductInput input) {
        Product product  = this.mapToproduct(input);
            this.productRepository.save(product);
        return product.getId();
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

    public void update(UUID productId, ProductInput input) {
        Product product = this.findProduct(productId);
        Category category = this.findCategory(input.getCategoryId());
        this.updateproduct(product, input);
        product.setCategory(category);
        this.productRepository.save(product);
    }

    private void updateproduct(Product product, ProductInput input) {
        product.setName(input.getName());
        product.setBrand(input.getBrand());
        product.setDescription(input.getDescription());
        product.setEnabled(input.getEnabled());

        product.changePrice(input.getRegularPrice(), input.getSalePrice());
    }

    public void disable(UUID productId) {
        Product product = this.findProduct(productId);
        product.disable();
        this.productRepository.save(product);

    }

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