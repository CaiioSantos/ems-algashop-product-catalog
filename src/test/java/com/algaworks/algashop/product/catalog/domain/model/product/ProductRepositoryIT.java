package com.algaworks.algashop.product.catalog.domain.model.product;

import com.algaworks.algashop.product.catalog.TestContainerMongoDBConfig;
import com.algaworks.algashop.product.catalog.infrastructure.persistence.MongoConfig;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataMongoTest
@Import({MongoConfig.class, TestContainerMongoDBConfig.class})
@Slf4j
class ProductRepositoryIT {

    private static final Logger log = LoggerFactory.getLogger(ProductRepositoryIT.class);
    @Autowired
    private ProductRepository productRepository;

    @Test
    public void shouldFilter() {
        Page<ProductNameProjection> products = productRepository
                .findAllByEnabled(true, PageRequest.of(0, 3));
        products.forEach(p -> log.info("Product - Id: {} Name: {}", p.id(), p.name()));
    }

}