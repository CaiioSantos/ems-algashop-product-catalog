package com.algaworks.algashop.product.catalog.infrastructure.persistence.mapper;


import com.algaworks.algashop.product.catalog.application.product.query.ProductDetailOutput;
import com.algaworks.algashop.product.catalog.application.product.query.ProductSummaryOutput;
import com.algaworks.algashop.product.catalog.application.utility.Slugfier;
import com.algaworks.algashop.product.catalog.application.utility.mapper.Mapper;
import com.algaworks.algashop.product.catalog.domain.model.product.Product;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NamingConventions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ModelMapperConfig {

    private final Converter<String, String> fromStringToSlugConverter = context ->
        Slugfier.slugify(context.getSource());

    private final Converter<String, String> fromStringToShortConverter = context ->
            StringUtils.abbreviate(context.getSource(), 15);

    @Bean
    public Mapper mapper() {
        ModelMapper modelMapper = new ModelMapper();
        configuration(modelMapper);
        return modelMapper::map;
    }

    private void configuration(ModelMapper modelMapper) {
        modelMapper.getConfiguration()
                .setSourceNamingConvention(NamingConventions.NONE)
                .setDestinationNamingConvention(NamingConventions.NONE)
                .setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.createTypeMap(Product.class, ProductDetailOutput.class)
                .addMappings(mapper -> {
                    mapper.using(fromStringToSlugConverter).map(Product::getName, ProductDetailOutput::setSlug);
                });
        modelMapper.createTypeMap(Product.class, ProductSummaryOutput.class)
                .addMappings(mapper -> {
                    mapper.using(fromStringToShortConverter).map(Product::getDescription, ProductSummaryOutput::setShortDescription);
                });
    }
}
