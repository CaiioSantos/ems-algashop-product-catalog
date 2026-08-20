package com.algaworks.algashop.product.catalog.application.utility.mapper;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@Component
@ConfigurationProperties("algashop.mapping")
public class ApplicationMapperProperties {
    @NotBlank
    private String imageStoreUrl;
}
