package com.algaworks.algashop.product.catalog.application.category.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CategoryUpdateEvent {

    private UUID categoryId;
    private String name;
    private Boolean enabled;
}
