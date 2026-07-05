package com.algaworks.algashop.product.catalog.infrastructure.persistence.product;

import com.algaworks.algashop.product.catalog.application.product.query.*;
import com.algaworks.algashop.product.catalog.infrastructure.utility.mapper.Mapper;
import com.algaworks.algashop.product.catalog.domain.model.ProductNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.product.Product;
import com.algaworks.algashop.product.catalog.domain.model.product.ProductRepository;
import com.mongodb.internal.operation.AggregateOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
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
        Optional<Criteria> criteria = buildCriteria(filter);
        Optional<TextCriteria> textCriteria = buildTextCriteria(filter);

        Query query = new Query();
        textCriteria.ifPresent(query::addCriteria);
        criteria.ifPresent(query::addCriteria);

        long totalElements = mongoOperations.count(query, Product.class);

        if (totalElements == 0L) {
            return PageModel.<ProductSummaryOutput>builder()
                    .number(0)
                    .size(0)
                    .totalPages(0)
                    .totalElements(0)
                    .build();
        }

        List<AggregationOperation> operations = new ArrayList<>();

        textCriteria.ifPresent(c -> {
            operations.add(match(c));
            AggregationOperation addTextScoreField = context ->
                    new Document("$addFields", new Document("score", new Document("$meta", "textScore")));
            operations.add(addTextScoreField);
        });
        criteria.ifPresent(c -> operations.add(match(c)));

        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());

        operations.addAll(Arrays.asList(
                sort(sortWith(filter)),
                projectionForSummary(),
                skip(pageRequest.getOffset()),
                limit(filter.getSize())
        ));

        Aggregation aggregation = newAggregation(operations);

        List<ProductSummaryOutput> productSummaryOutputs = mongoOperations
                .aggregate(aggregation, Product.class, ProductSummaryOutput.class)
                .getMappedResults();

        int totalPages = (int) Math.ceil((double) totalElements / (double) filter.getSize());

        return PageModel.<ProductSummaryOutput>builder()
                .content(productSummaryOutputs)
                .number(filter.getPage())
                .size(filter.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private ProjectionOperation projectionForSummary() {
        return project(ProductDetailOutput.class)
                .andExpression("salePrice < regularPrice").as("hasDiscount")
                .andExpression("quantityInStock > 0").as("inStock")
                .and(StringOperators.Substr.valueOf("description")
                        .substring(0, 50)).as("shortDescription");

    }

    private Optional<Criteria> buildCriteria(ProductFilter filter) {
        List<CriteriaDefinition> criterias = new ArrayList<>();

        if (filter.getEnabled() != null) {
            criterias.add(Criteria.where("enabled").is(filter.getEnabled()));
        }

        if (filter.getAddedAtFrom() != null && filter.getAddedAtTo() != null) {
            criterias.add(Criteria.where("addedAt")
                    .gte(filter.getAddedAtFrom())
                    .lte(filter.getAddedAtTo())
            );
        } else {
            if (filter.getAddedAtFrom() != null) {
                criterias.add(Criteria.where("addedAt").gte(filter.getAddedAtFrom()));
            } else if (filter.getAddedAtTo() != null) {
                criterias.add(Criteria.where("addedAt").lte(filter.getAddedAtTo()));
            }
        }

        if (filter.getPriceFrom() != null && filter.getPriceTo() != null ) {
            criterias.add(Criteria.where("salePrice")
                    .gte(filter.getPriceFrom())
                    .lte(filter.getPriceTo())
            );
        } else {
            if (filter.getPriceFrom() != null) {
                criterias.add(Criteria.where("salePrice").gte(filter.getPriceFrom()));
            } else if (filter.getPriceTo() != null) {
                criterias.add(Criteria.where("salePrice").lte(filter.getPriceTo()));
            }
        }

        if (filter.getHasDiscount() != null) {
            if (filter.getHasDiscount()) {
                criterias.add(AggregationExpressionCriteria.whereExpr(
                        ComparisonOperators.valueOf("$salePrice")
                                .lessThan("$regularPrice")
                ));
            } else {
                criterias.add(AggregationExpressionCriteria.whereExpr(
                        ComparisonOperators.valueOf("$salePrice")
                                .equalTo("$regularPrice")
                ));
            }
        }

        if (filter.getInStock() != null) {
            if (filter.getInStock()) {
                criterias.add(Criteria.where("quantityInStock").gt(0));
            } else {
                criterias.add(Criteria.where("quantityInStock").is(0));
            }
        }

        if (filter.getCategoryIds() != null && filter.getCategoryIds().length > 0) {
            criterias.add(Criteria.where("category.id").in(
                    (Object[]) filter.getCategoryIds()
            ));
        }

        if (criterias.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new Criteria().andOperator(criterias.toArray(new Criteria[0]))
        );
    }

    public Optional<TextCriteria> buildTextCriteria(ProductFilter filter) {
        if (StringUtils.isNotBlank(filter.getTerm())) {
            return Optional.of(TextCriteria.forDefaultLanguage().matching(filter.getTerm()));
        }
        return Optional.empty();
    }

    private Sort sortWith(ProductFilter filter) {
        if (StringUtils.isNotBlank(filter.getTerm())) {
            return Sort.by("score");
        }
        return Sort.by(filter.getSortDirectionOrDefault(),
                filter.getSortByPropertyOrDefault().getPropertyName());
    }
}
