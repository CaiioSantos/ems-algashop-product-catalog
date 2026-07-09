package com.algaworks.algashop.product.catalog.presentation;

import com.algaworks.algashop.product.catalog.application.category.management.CategoryInput;
import com.algaworks.algashop.product.catalog.application.category.management.CategoryManagementService;
import com.algaworks.algashop.product.catalog.application.category.query.CategoryDetailOutput;
import com.algaworks.algashop.product.catalog.application.category.query.CategoryFilter;
import com.algaworks.algashop.product.catalog.application.category.query.CategoryQueryService;
import com.algaworks.algashop.product.catalog.application.product.query.PageModel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;
    private final CategoryManagementService categoryManagementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDetailOutput create(@RequestBody @Valid CategoryInput input) {
        UUID categoryId = categoryManagementService.create(input);
        return categoryQueryService.findById(categoryId);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDetailOutput> findById(@PathVariable UUID categoryId) {
        CategoryDetailOutput category = categoryQueryService.findById(categoryId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .eTag("category:id:" + category.getId() + ":v1:" + category.getVersion())
                .body(category);
    }

    @GetMapping
    public ResponseEntity<PageModel<CategoryDetailOutput>> filter(CategoryFilter filter, WebRequest request) {

        if (!filter.isCacheable()) {
            PageModel<CategoryDetailOutput> result = categoryQueryService.filter(filter);
            return ResponseEntity.ok(result);
        }
        OffsetDateTime lastModified = categoryQueryService.lastModified();

        if (request.checkNotModified(lastModified.toInstant().toEpochMilli())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                    .lastModified(lastModified.toInstant())
                    .build();
        }

        PageModel<CategoryDetailOutput> result = categoryQueryService.filter(filter);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .lastModified(lastModified.toInstant())
                .body(result);
    }

    @PutMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    public  CategoryDetailOutput update(@PathVariable UUID categoryId, @RequestBody @Valid CategoryInput updateInput){
        categoryManagementService.update(categoryId,updateInput);
        return categoryQueryService.findById(categoryId);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public  void delete(@PathVariable UUID categoryId){
        categoryManagementService.disable(categoryId);
    }

}
