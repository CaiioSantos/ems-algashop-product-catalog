package com.algaworks.algashop.product.catalog.domain.model.product;

import com.algaworks.algashop.product.catalog.domain.model.DomainException;
import com.algaworks.algashop.product.catalog.domain.model.IdGenerator;
import com.algaworks.algashop.product.catalog.domain.model.category.Category;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Document(collection = "products")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Product {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;
    private String name;
    private String brand;
    private String description;
    private Integer quantityInStock = 0;
    private Boolean enabled;
    private BigDecimal regularPrice;
    private BigDecimal salePrice;

    @Version
    private Long version;

    @CreatedDate
    private OffsetDateTime addedAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;

    @CreatedBy
    private UUID createdByUserId;

    @LastModifiedBy
    private UUID lastModifiedByUserId;

    @DocumentReference
    @Field(name = "categoryId")
    private Category category;

    private Integer discountPercentageRounded;

    @Builder
    public Product(String name, String brand, String description, Boolean enabled, BigDecimal regularPrice,
                   BigDecimal salePrice, Category category) {
        this.setId(IdGenerator.generateTimeBasedUUID());
        this.setName(name);
        this.setBrand(brand);
        this.setDescription(description);
        this.setEnabled(enabled);
        this.setRegularPrice(regularPrice);
        this.setSalePrice(salePrice);
        this.setCategory(category);
    }

    public void setName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }
    public void setBrand(String brand) {
        if (StringUtils.isBlank(brand)) {
            throw new IllegalArgumentException();
        }
        this.brand = brand;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setRegularPrice(BigDecimal regularPrice) {
        Objects.requireNonNull(regularPrice);
        if (regularPrice.signum() == -1) {
            throw new IllegalArgumentException();
        }
        if (this.salePrice == null) {
            this.salePrice = regularPrice;
        }else if (regularPrice.compareTo(this.salePrice) < 0) {
            throw new DomainException("Regular price cannot be lower than sale price");
        }
        this.regularPrice = regularPrice;
        this.calculateDiscountPercentageRounded();
    }

    public void setSalePrice(BigDecimal salePrice) {
        Objects.requireNonNull(salePrice);
        if (salePrice.signum() == -1) {
            throw new IllegalArgumentException();
        }
        if (this.regularPrice == null) {
            this.regularPrice = salePrice;
        }else if (this.regularPrice.compareTo(salePrice) < 0) {
            throw new DomainException("Sale price cannot be higher than regular price");
        }
        this.salePrice = salePrice;
        this.calculateDiscountPercentageRounded();
    }

    public void setCategory(Category category) {
        Objects.requireNonNull(category);
        this.category = category;

    }

    public void setEnabled(Boolean enabled) {
        Objects.requireNonNull(enabled);
        this.enabled = enabled;
    }

    public void disable() {
        this.setEnabled(false);
    }

     public void enable() {
        this.setEnabled(true);
    }

    public boolean isInStock() {
        return this.getQuantityInStock() != null && this.getQuantityInStock() > 0;
    }

    public boolean getHasDiscount() {
        return getDiscountPercentageRounded() != null && getDiscountPercentageRounded() > 0;
    }

    private void setId(UUID id) {
     Objects.requireNonNull(id);
        this.id = id;
    }

    private void setQuantityInStock(Integer quantityInStock) {
        Objects.requireNonNull(quantityInStock);
        if (quantityInStock < 0) {
            throw new IllegalArgumentException();
        }
        this.quantityInStock = quantityInStock;
    }

    private void calculateDiscountPercentageRounded() {
        if (this.regularPrice == null || this.salePrice == null || this.regularPrice.signum() == 0) {
            this.discountPercentageRounded = 0;
            return;
        }
        discountPercentageRounded = BigDecimal.ONE.
                 subtract(this.salePrice.divide(this.regularPrice, 4, BigDecimal.ROUND_HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .intValue();
    }


}
