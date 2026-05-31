package com.algaworks.algashop.product.catalog.application.category.query;

import com.algaworks.algashop.product.catalog.infrastructure.utility.mapper.SortablePageFilter;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryFilter extends SortablePageFilter<CategoryFilter.SortType> {


    private String name;
    private Boolean enabled;


    @Override
    public SortType getSortByPropertyOrDefault() {
        return getSortByProperty() == null ? SortType.NAME : getSortByProperty();
    }

    @Override
    public Sort.Direction getSortDirectionOrDefault() {
        return getSortDirection() == null ? Sort.Direction.ASC : getSortDirection();
    }

    @Getter
    @RequiredArgsConstructor
    public enum SortType {
        NAME("name");
        private final String propertyName;
    }
}
