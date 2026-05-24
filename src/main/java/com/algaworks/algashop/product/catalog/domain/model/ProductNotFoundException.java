package com.algaworks.algashop.product.catalog.domain.model;


import java.util.UUID;

public class ProductNotFoundException extends DomainEntityNotFoundException {

    public ProductNotFoundException(UUID productId) {
        super(String.format("Product with id %s not found", productId));
    }
}
