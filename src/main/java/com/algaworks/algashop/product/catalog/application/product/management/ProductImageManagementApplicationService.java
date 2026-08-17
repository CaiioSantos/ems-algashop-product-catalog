package com.algaworks.algashop.product.catalog.application.product.management;

import com.algaworks.algashop.product.catalog.application.product.query.ImageOutput;
import com.algaworks.algashop.product.catalog.application.storage.StorageProvider;
import com.algaworks.algashop.product.catalog.application.utility.mapper.Mapper;
import com.algaworks.algashop.product.catalog.domain.model.DomainException;
import com.algaworks.algashop.product.catalog.domain.model.ProductNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.product.Image;
import com.algaworks.algashop.product.catalog.domain.model.product.Product;
import com.algaworks.algashop.product.catalog.domain.model.product.ProductRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageManagementApplicationService {

    private final ProductRepository productRepository;
    private final StorageProvider storageProvider;
    private final Mapper mapper;

    @CacheEvict(value = "algashop:products:v1", key = "#productId")
    public ImageOutput create(UUID productId, ImageInput imageInput) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(imageInput);
        var product = findById(productId);
        if (!this.storageProvider.fileExists(imageInput.getRemoteFileName())) {
            throw new DomainException(String.format("Image %s is already in use", imageInput.getRemoteFileName()));
        }

        if (this.productRepository.existsByImagesName(imageInput.getRemoteFileName())) {
            throw new DomainException("Image with the same name already exists");
        }

        var imageId = product.addImage(imageInput.getRemoteFileName());
        this.productRepository.save(product);
        Image image = product.getImage(imageId).orElseThrow();
        return this.mapper.convert(image, ImageOutput.class);
    }

    @CacheEvict(value = "algashop:products:v1", key = "#productId")
    public void delete(UUID productId, UUID imageId) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(imageId);
        var product = this.findById(productId);
        Image image = getImage(productId, imageId, product);
        product.removeImage(imageId);
        this.storageProvider.deleteFile(image.getName());
        this.productRepository.save(product);
    }

    @CacheEvict(value = "algashop:products:v1", key = "#productId")
    public void primary(UUID productId, UUID imageId) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(imageId);
        var product = this.findById(productId);
        getImage(productId, imageId, product);
        product.changeMainImage(imageId);
        this.productRepository.save(product);
    }

    private static Image getImage(UUID productId, UUID imageId, Product product) {
        return product.getImage(imageId).orElseThrow(() -> new DomainException(
                String.format("Image of id %s was not found on product %s", imageId, productId)));
    }

    private Product findById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}