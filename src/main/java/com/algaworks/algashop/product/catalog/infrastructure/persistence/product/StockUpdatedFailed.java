package com.algaworks.algashop.product.catalog.infrastructure.persistence.product;

public class StockUpdatedFailed extends RuntimeException {
    public StockUpdatedFailed() {
    }

    public StockUpdatedFailed(String message) {
        super(message);
    }

    public StockUpdatedFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public StockUpdatedFailed(Throwable cause) {
        super(cause);
    }

    public StockUpdatedFailed(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
