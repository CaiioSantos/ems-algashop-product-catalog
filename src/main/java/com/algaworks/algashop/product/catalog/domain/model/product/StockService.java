package com.algaworks.algashop.product.catalog.domain.model.product;

import com.algaworks.algashop.product.catalog.domain.model.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final QuantityInStockAdjustment quantityInStockAdjustment;
    private final DomainEventPublisher domainEventPublisher;


    public StockMovement restock(Product product, int quantity) {
        Objects.requireNonNull(product);
         if (quantity < 1){
             throw new IllegalArgumentException();
         }
        var result = quantityInStockAdjustment.increase(product.getId(), quantity);

        if (result.inRestocked()){
             domainEventPublisher.publish(
                     ProductRestockedEvent.builder()
                             .productId(product.getId())
                             .build()
             );
         }
        return StockMovement.builder()
                .productId(product.getId())
                .movementQuantity(quantity)
                .previousQuantity(result.previousQuantity())
                .newQuantity(result.newQuantity())
                .movementType(StockMovement.MovementType.STOCK_IN)
                .build();
    }

    public StockMovement withdraw(Product product, int quantity) {
        Objects.requireNonNull(product);
        if (quantity < 1) {
            throw new IllegalArgumentException();
        }
        var result = quantityInStockAdjustment.decrease(product.getId(), quantity);

        if (result.isOutOfStock()) {
            domainEventPublisher.publish(
                    ProductSoldOutEvent.builder()
                            .productId(product.getId())
                            .build()
            );
        }
        return StockMovement.builder()
                .productId(product.getId())
                .movementQuantity(quantity)
                .previousQuantity(result.previousQuantity())
                .newQuantity(result.newQuantity())
                .movementType(StockMovement.MovementType.STOCK_OUT)
                .build();
    }

}
