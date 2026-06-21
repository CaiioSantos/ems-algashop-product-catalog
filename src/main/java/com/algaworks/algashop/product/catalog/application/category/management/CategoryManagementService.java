package com.algaworks.algashop.product.catalog.application.category.management;

import com.algaworks.algashop.product.catalog.application.ApplicationMessagePublisher;
import com.algaworks.algashop.product.catalog.application.category.event.CategoryUpdateEvent;
import com.algaworks.algashop.product.catalog.domain.model.CategoryNotFoundException;
import com.algaworks.algashop.product.catalog.domain.model.category.Category;
import com.algaworks.algashop.product.catalog.domain.model.category.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;
    @Qualifier("applicationMessagePublisher")
    private final ApplicationMessagePublisher applicationMessagerPublisher;

    public UUID create(@Valid CategoryInput input) {
        Category category = new Category(input.getName(), input.getEnabled());
        categoryRepository.save(category);
        return category.getId();
    }

    public void update(UUID categoryId, CategoryInput input) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.setName(input.getName());
        category.setEnabled(input.getEnabled());
        categoryRepository.save(category);

        applicationMessagerPublisher.send(new CategoryUpdateEvent(category.getId(), category.getName(), category.getEnabled()));
    }

    public void disable(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.setEnabled(false);
        categoryRepository.save(category);

        applicationMessagerPublisher.send(new CategoryUpdateEvent(category.getId(), category.getName(), category.getEnabled()));
    }
}