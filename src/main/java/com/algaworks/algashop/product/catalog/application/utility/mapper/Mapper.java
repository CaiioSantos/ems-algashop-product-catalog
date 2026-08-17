package com.algaworks.algashop.product.catalog.application.utility.mapper;

public interface Mapper {
    <T> T convert(Object object, Class<T> destinationType);
}