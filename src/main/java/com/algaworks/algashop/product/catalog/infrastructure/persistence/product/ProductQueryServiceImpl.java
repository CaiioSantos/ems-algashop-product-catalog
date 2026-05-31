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
        Optional<Criteria> criteriaOpt = buildCriteria(filter);
        Optional<TextCriteria> textCriteriaOpt = buildTextCriteria(filter);

        Query countQuery = new Query();
        criteriaOpt.ifPresent(countQuery::addCriteria);
        textCriteriaOpt.ifPresent(countQuery::addCriteria);
        long totalItems = mongoOperations.count(countQuery, Product.class);

        if (totalItems == 0) {
            return PageModel.<ProductSummaryOutput>builder()
                    .number(0)
                    .size(0)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        textCriteriaOpt.ifPresent(textCriteria -> {
            aggregationOperations.add(Aggregation.match(textCriteria));
            AggregationOperation addTextScoreField = context ->
                    new Document("$addFields",
                    new Document("score", new Document("$meta", "textScore")));
            aggregationOperations.add(addTextScoreField);
        });
        criteriaOpt.ifPresent(criteria -> aggregationOperations.add(Aggregation.match(criteria)));


        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());
        aggregationOperations.addAll(Arrays.asList(
                Aggregation.lookup("categories", "categoryId", "_id", "category"),
                Aggregation.unwind("category", true),
                Aggregation.sort(sortWith(filter)),
                projectionForSummary(),
                Aggregation.skip(pageRequest.getOffset()),
                Aggregation.limit(filter.getSize())
        ));

        Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
        List<ProductSummaryOutput> products = mongoOperations
                .aggregate(aggregation, "products", ProductSummaryOutput.class)
                .getMappedResults();
    int totalPages = (int) Math.ceil((double) totalItems / filter.getSize());

        return PageModel.<ProductSummaryOutput>builder()
                .content(products)
                .number(pageRequest.getPageNumber())
                .size(pageRequest.getPageSize())
                .totalElements(totalItems)
                .totalPages(totalPages)
                .build();
    }

    private ProjectionOperation projectionForSummary() {
     return Aggregation.project()
             .and("_id").as("_id")
             .and("addedAt").as("addedAt")
             .and("name").as("name")
             .and("brand").as("brand")
             .and("regularPrice").as("regularPrice")
             .and("salePrice").as("salePrice")
             .and("quantityInStock").as("quantityInStock")
             .and("enabled").as("enabled")
             .and("discountPercentageRounded").as("discountPercentageRounded")
             .and("score").as("score")
             .and("category._id").as("category._id")
             .and("category.name").as("category.name")
             .and("score").as("score")

             .andExpression("salePrice < regularPrice").as("hasDiscount")
             .andExpression("quantityInStock > 0").as("inStock")
             .and(StringOperators.Substr.valueOf("description")
                     .substring(0, 100)).as("shortDescription");
    }


    private Sort sortWith(ProductFilter filter) {
        return Sort.by(filter.getSortDirectionOrDefault(), filter.getSortByPropertyOrDefault().toString());
    }

    private Optional<Criteria> buildCriteria(ProductFilter filter) {
        List<CriteriaDefinition> criteriaList = new ArrayList<>();

            if (filter.getTerm() != null) {
                criteriaList.add(where("name").regex(filter.getTerm(), "i"));
            }
            if (filter.getHasDiscount() != null) {
                if (filter.getHasDiscount()) {
                    criteriaList.add(AggregationExpressionCriteria.
                            whereExpr(ComparisonOperators.valueOf("$salePrice")
                                    .lessThan("$regularPrice")));
                } else {
                    criteriaList.add(AggregationExpressionCriteria.
                            whereExpr(ComparisonOperators.valueOf("$salePrice")
                                    .equalTo("$regularPrice")));
                }
            }
            if (filter.getEnabled() != null) {
                criteriaList.add(where("enabled").is(filter.getEnabled()));
            }

        if (filter.getInStock() != null) {
            if (filter.getInStock()) {
                criteriaList.add((where("quantityInStock").gt(0)));
            } else {
                criteriaList.add((where("quantityInStock").is(0)));
            }
        }

            if (filter.getPriceFrom() != null && filter.getPriceTo() != null) {
                criteriaList.add((where("salePrice")
                        .gte(filter.getPriceFrom())
                        .lte(filter.getPriceTo())));
            } else {
                if (filter.getPriceFrom() != null) {
                    criteriaList.add((where("salePrice").gte(filter.getPriceFrom())));
                }
                if (filter.getPriceTo() != null) {
                    criteriaList.add((where("salePrice").lte(filter.getPriceTo())));
                }
            }

            if (filter.getCategoryIds() != null && filter.getCategoryIds().length > 0) {
                criteriaList.add((where("categoryId").in(
                        (Object[]) filter.getCategoryIds())));
            }

            if (filter.getAddedAtFrom() != null && filter.getAddedAtTo() != null) {
                criteriaList.add((where("addedAt")
                        .gte(filter.getAddedAtFrom())
                        .lte(filter.getAddedAtTo())));
            } else {
                if (filter.getAddedAtFrom() != null) {
                    criteriaList.add((where("addedAt").gte(filter.getAddedAtFrom())));
                }
                if (filter.getAddedAtTo() != null) {
                    criteriaList.add((where("addedAt").lte(filter.getAddedAtTo())));
                }
            }

     if (criteriaList.isEmpty()) {
         return Optional.empty();
     }
        return Optional.of(
                new Criteria().andOperator(criteriaList.toArray(new Criteria[0]))
        );
    }
     public Optional<TextCriteria> buildTextCriteria(ProductFilter filter) {
         if (StringUtils.isBlank(filter.getTerm())) {
             return Optional.empty();
         }
         return Optional.of(TextCriteria.forDefaultLanguage().matchingPhrase(filter.getTerm()));
     }
}
