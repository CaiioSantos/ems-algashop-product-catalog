package com.algaworks.algashop.product.catalog.domain.model.product;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Getter
public class ProductSoldOutEvent {

    private UUID productId;

    @Builder.Default
    private OffsetDateTime soldOutAt = OffsetDateTime.now();

}
