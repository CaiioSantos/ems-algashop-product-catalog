package com.algaworks.algashop.product.catalog.infrastructure.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class SecurityAnnotations {

    @Target({
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_products:read')")
    public @interface CanReadProducts {
    }

    @Target({
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_products:write')")
    public @interface CanWriteProducts {
    }
    @Target({
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_products:restock:write')")
    public @interface CanRestockProducts {
    }

    @Target({
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_categories:read')")
    public @interface CanReadCategories {
    }

    @Target({
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('SCOPE_categories:write')")
    public @interface CanWriteCategories {
    }

}
