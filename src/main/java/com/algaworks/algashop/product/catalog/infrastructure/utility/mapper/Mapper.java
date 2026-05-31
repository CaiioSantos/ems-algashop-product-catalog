package com.algaworks.algashop.product.catalog.infrastructure.utility.mapper;

public interface Mapper {
    <T> T convert(Object object, Class<T> destinationType);
}