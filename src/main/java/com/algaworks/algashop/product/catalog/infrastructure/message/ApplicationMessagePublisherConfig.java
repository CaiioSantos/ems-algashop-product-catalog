package com.algaworks.algashop.product.catalog.infrastructure.message;

import com.algaworks.algashop.product.catalog.application.ApplicationMessagerPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationMessagePublisherConfig {

    @Bean
    public ApplicationMessagerPublisher applicationMessagePublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return applicationEventPublisher::publishEvent;
    }
}
