package com.algaworks.algashop.product.catalog.application.utility;

public interface Mapper {
    <T> T convert(Object object, Class<T> destinationType);
}