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
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Document(collection = "products")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@CompoundIndex(name = "pidx_product_by_category_enabledTrue_salePrice",
        def = "{'category.id': 1, 'salePrice': 1}",
        partialFilter = "{'enabled': true}")
@CompoundIndex(name = "pidx_product_by_category_enabledTrue_addedAt",
        def = "{'category.id': 1, 'addedAt': -1}",
        partialFilter = "{'enabled': true}")
public class Product extends AbstractAggregateRoot<Product> {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @TextIndexed(weight = 1)
    private String name;

    @Indexed(name = "idx_product_by_brand")
    private String brand;

    @TextIndexed(weight = 5)
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

    private ProductCategory category;

    private Integer discountPercentageRounded;

    @TextScore
    private Float score;

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

        registerProductAddedEvent();
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

    public void setCategory(Category category) {
        Objects.requireNonNull(category);
        this.category = ProductCategory.of(category);

    }

    public void setEnabled(Boolean enabled) {
        Objects.requireNonNull(enabled);
        Boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        if (wasEnabled != null && wasEnabled && !this.getEnabled()) {
            this.registerEvent(ProductDelistedEvent.builder()
                    .productId(this.getId())
                    .build());
        } else if (wasEnabled != null && !wasEnabled && this.getEnabled()) {
            this.registerEvent(ProductListedEvent.builder()
                    .productId(this.getId())
                    .build());
        }

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

    public void changePrice(BigDecimal regularPrice,BigDecimal salePrice) {
        Objects.requireNonNull(regularPrice);
        Objects.requireNonNull(salePrice);

        BigDecimal oldRegularPrice = this.regularPrice;
        BigDecimal oldSaleprice = this.salePrice;

        boolean wasOnSale = getHasDiscount();

        if (this.regularPrice.compareTo(salePrice) < 0) {
            throw new DomainException("Sale price cannot be higher than regular price");
        }

        setRegularPrice(regularPrice);
        setSalePrice(salePrice);

        if (pricesDidNotChange(oldRegularPrice,oldSaleprice)){
            return;
        }
        registerPriceChangedEvent(oldRegularPrice, oldSaleprice);

        if (isNewlyOnSale(wasOnSale)) {
            registerProductPlacedOnSaleEvent();
        }

    }

    private boolean pricesDidNotChange(BigDecimal oldRegularPrice, BigDecimal oldSaleprice) {
    return Objects.equals(this.regularPrice, oldRegularPrice) && Objects.equals(this.salePrice, oldSaleprice);
    }

    private void registerPriceChangedEvent(BigDecimal oldRegularPrice, BigDecimal oldSaleprice) {
        super.registerEvent(
                ProductPriceChangedEvent.builder()
                        .productId(this.id)
                        .newSalePrice(this.salePrice)
                        .newRegularPrice(this.regularPrice)
                        .oldRegularPrice(oldRegularPrice)
                        .oldSalePrice(oldSaleprice)
                        .build()
        );
    }

    private boolean isNewlyOnSale(boolean wasOnSale) {
        return getHasDiscount() && !wasOnSale;
    }

    private void registerProductPlacedOnSaleEvent() {
        super.registerEvent(
                ProductPlacedOnSaleEvent.builder()
                        .productId(this.id)
                        .regularPrice(this.regularPrice)
                        .salePrice(this.salePrice)
                        .build()
        );
    }

    public void registerProductAddedEvent() {
        super.registerEvent(
                ProductAddedEvent.builder()
                        .productId(this.id)
                        .build()
        );
    }

    private void statusProductDelistedEvent() {
        super.registerEvent(
                ProductDelistedEvent.builder()
                        .productId(this.id)
                        .build()
        );
    }

    private void statusProductListedEvent() {
        super.registerEvent(
                ProductListedEvent.builder()
                        .productId(this.id)
                        .build()
        );
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

    private void setRegularPrice(BigDecimal regularPrice) {
        Objects.requireNonNull(regularPrice);
        if (regularPrice.signum() == -1) {
            throw new IllegalArgumentException();
        }

        this.regularPrice = regularPrice;
        this.calculateDiscountPercentageRounded();
    }

    private void setSalePrice(BigDecimal salePrice) {
        Objects.requireNonNull(salePrice);
        if (salePrice.signum() == -1) {
            throw new IllegalArgumentException();
        }

        this.salePrice = salePrice;
        this.calculateDiscountPercentageRounded();
    }


}
