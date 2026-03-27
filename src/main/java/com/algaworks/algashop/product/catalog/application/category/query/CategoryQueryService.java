package com.algaworks.algashop.product.catalog.application.category.query;

import com.algaworks.algashop.product.catalog.application.product.query.PageModel;
import com.algaworks.algashop.product.catalog.application.product.query.ProductDetailOutput;

import java.util.UUID;

public interface CategoryQueryService {
    CategoryDetailOutput findById(UUID productId);
    PageModel<CategoryDetailOutput> filter(Integer size, Integer number);
}