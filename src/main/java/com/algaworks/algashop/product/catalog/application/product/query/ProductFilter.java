package com.algaworks.algashop.product.catalog.application.product.query;

import com.algaworks.algashop.product.catalog.infrastructure.utility.mapper.SortablePageFilter;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductFilter extends SortablePageFilter<ProductFilter.SortType> {

    private String term;

    private Boolean hasDiscount;

    private Boolean enabled;

    private Boolean inStock;

    private BigDecimal priceFrom;

    private BigDecimal priceTo;

    private UUID[] categoryIds;

    private OffsetDateTime addedAtFrom;
    private OffsetDateTime addedAtTo;

    @Override
    public SortType getSortByPropertyOrDefault() {
        return getSortByProperty() == null ? SortType.ADDED_AT : getSortByProperty();
    }

    @Override
    public Sort.Direction getSortDirectionOrDefault() {
        return getSortDirection() == null ? Sort.Direction.ASC : getSortDirection();
    }

    @Getter
    @RequiredArgsConstructor
    public enum SortType {
        ADDED_AT("addedAt"),
        SALE_PRICE("salePrice");

        private final String propertyName;
    }
}
