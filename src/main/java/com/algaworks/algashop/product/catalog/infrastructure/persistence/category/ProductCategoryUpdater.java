package com.algaworks.algashop.product.catalog.infrastructure.persistence.category;

import com.algaworks.algashop.product.catalog.application.category.event.CategoryUpdateEvent;
import com.algaworks.algashop.product.catalog.domain.model.product.Product;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProductCategoryUpdater {

    private final MongoOperations mongoOperations;

    public void copyCategoryDataToProducts(CategoryUpdateEvent categoryUpdateEvent) {
        Query query = new Query(
                Criteria.where("category.id").is(categoryUpdateEvent.getCategoryId())
        );
        Update update = new Update()
                .set("category.name", categoryUpdateEvent.getName())
                .set("category.enabled", categoryUpdateEvent.getEnabled());
        mongoOperations.updateMulti(query, update, Product.class);
    }
}
