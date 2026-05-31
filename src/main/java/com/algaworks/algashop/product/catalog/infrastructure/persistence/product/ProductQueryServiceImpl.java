package com.algaworks.algashop.product.catalog.infrastructure.persistence.product;

import com.algaworks.algashop.product.catalog.application.product.query.*;
import com.algaworks.algashop.product.catalog.infrastructure.utility.mapper.Mapper;
import com.algaworks.algashop.product.catalog.domain.model.ProductNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.product.Product;
import com.algaworks.algashop.product.catalog.domain.model.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.AggregationExpressionCriteria;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    private final Mapper mapper;

    private final MongoOperations mongoOperations;

    private static final String findWordRegex = "(?i)%s";

    @Override
    public ProductDetailOutput findById(UUID productId) {
        return this.productRepository.findById(productId)
                .map(product -> this.mapper.convert(product, ProductDetailOutput.class))
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public PageModel<ProductSummaryOutput> filter(ProductFilter filter) {
        Query query = queryWith(filter);
        long totalItems = mongoOperations.count(query, Product.class);
        Sort sort = sortWith(filter);

        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        Query pagedQuery = query.with(pageRequest);

        List<Product> products;
        int totalPages = 0;

        if (totalItems > 0) {
            products = mongoOperations.find(pagedQuery, Product.class);
            totalPages = (int) Math.ceil((double) totalItems / pageRequest.getPageSize());
        } else {
            products = new ArrayList<>();
        }

        List<ProductSummaryOutput> productOutputs = products.stream()
                .map(p -> mapper.convert(p, ProductSummaryOutput.class))
                .collect(Collectors.toList());
        return PageModel.<ProductSummaryOutput>builder()
                .content(productOutputs)
                .number(pageRequest.getPageNumber())
                .size(pageRequest.getPageSize())
                .totalElements(totalItems)
                .totalPages(totalPages)
                .build();
    }


    private Sort sortWith(ProductFilter filter) {
        return Sort.by(filter.getSortDirectionOrDefault(), filter.getSortByPropertyOrDefault().toString());
    }

    private Query queryWith(ProductFilter filter) {
            Query query = new Query();

            if (filter.getTerm() != null) {
                query.addCriteria(where("name").regex(filter.getTerm(), "i"));
            }
            if (filter.getHasDiscount() != null) {
                if (filter.getHasDiscount()) {
                    query.addCriteria(AggregationExpressionCriteria.
                            whereExpr(ComparisonOperators.valueOf("$salePrice")
                                    .lessThan("$regularPrice")));
                } else {
                    query.addCriteria(AggregationExpressionCriteria.
                            whereExpr(ComparisonOperators.valueOf("$salePrice")
                                    .equalTo("$regularPrice")));
                }
            }
            if (filter.getEnabled() != null) {
                query.addCriteria(where("enabled").is(filter.getEnabled()));
            }

        if (filter.getInStock() != null) {
            if (filter.getInStock()) {
                query.addCriteria(where("quantityInStock").gt(0));
            } else {
                query.addCriteria(where("quantityInStock").is(0));
            }
        }

            if (filter.getPriceFrom() != null && filter.getPriceTo() != null) {
                query.addCriteria(where("salePrice")
                        .gte(filter.getPriceFrom())
                        .lte(filter.getPriceTo()));
            } else {
                if (filter.getPriceFrom() != null) {
                    query.addCriteria(where("salePrice").gte(filter.getPriceFrom()));
                }
                if (filter.getPriceTo() != null) {
                    query.addCriteria(where("salePrice").lte(filter.getPriceTo()));
                }
            }

            if (filter.getCategoryIds() != null && filter.getCategoryIds().length > 0) {
                query.addCriteria(where("categoryId").in(
                        (Object[]) filter.getCategoryIds()));
            }

            if (filter.getAddedAtFrom() != null && filter.getAddedAtTo() != null) {
                query.addCriteria(where("addedAt")
                        .gte(filter.getAddedAtFrom())
                        .lte(filter.getAddedAtTo()));
            } else {
                if (filter.getAddedAtFrom() != null) {
                    query.addCriteria(where("addedAt").gte(filter.getAddedAtFrom()));
                }
                if (filter.getAddedAtTo() != null) {
                    query.addCriteria(where("addedAt").lte(filter.getAddedAtTo()));
                }
            }

            if (StringUtils.isNotBlank(filter.getTerm())) {
                String regex = String.format(findWordRegex, filter.getTerm());
                query.addCriteria(new Criteria().orOperator(
                        where("name").regex(regex),
                        where("brand").regex(regex),
                        where("description").regex(regex)
                ));
            }

            return query;
    }
}
