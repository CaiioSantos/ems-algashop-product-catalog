package com.algaworks.algashop.product.catalog.domain.model;


import java.util.UUID;

public class CategoryNotFoundException extends DomainEntityNotFoundException {

    public CategoryNotFoundException(UUID categoryId) {
        super(String.format("Category with id %s not found", categoryId));
    }
}