package com.algaworks.algashop.product.catalog.infrastructure.persistence.product;

import com.algaworks.algashop.product.catalog.application.product.query.PageModel;
import com.algaworks.algashop.product.catalog.application.product.query.ProductDetailOutput;
import com.algaworks.algashop.product.catalog.application.product.query.ProductQueryService;
import com.algaworks.algashop.product.catalog.application.product.query.ProductSummaryOutput;
import com.algaworks.algashop.product.catalog.application.utility.mapper.Mapper;
import com.algaworks.algashop.product.catalog.domain.model.ProductNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.modelmapper.Converters.Collection.map;

@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    private final Mapper mapper;
    @Override
    public ProductDetailOutput findById(UUID productId) {
        return this.productRepository.findById(productId)
                .map(product -> this.mapper.convert(product, ProductDetailOutput.class))
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public PageModel<ProductSummaryOutput> filter(Integer size, Integer number) {
        var products = this.productRepository.findAll(PageRequest.of(number, size))
                .map(product -> this.mapper.convert(product, ProductSummaryOutput.class));
        var productOutputs = products.map(p -> this.mapper.convert(p, ProductSummaryOutput.class));
        return PageModel.of(productOutputs);
    }
}
