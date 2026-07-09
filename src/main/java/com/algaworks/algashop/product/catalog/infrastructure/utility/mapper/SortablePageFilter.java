package com.algaworks.algashop.product.catalog.infrastructure.utility.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class SortablePageFilter<T> extends PageFilter {
    private T sortByProperty;
    private Sort.Direction sortDirection;

    public SortablePageFilter(int size, int page) {
        super(size, page);
    }

    public abstract T getSortByPropertyOrDefault();
    public abstract Sort.Direction getSortDirectionOrDefault();


}
