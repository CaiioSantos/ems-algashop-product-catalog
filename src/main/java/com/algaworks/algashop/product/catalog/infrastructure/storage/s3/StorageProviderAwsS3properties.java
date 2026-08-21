package com.algaworks.algashop.product.catalog.infrastructure.storage.s3;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties("algashop.storage.s3")
public class StorageProviderAwsS3properties {

    @NotBlank
    private String bucketName;
}
