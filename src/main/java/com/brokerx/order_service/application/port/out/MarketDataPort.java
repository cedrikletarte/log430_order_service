package com.brokerx.order_service.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Port for retrieving market data from external sources
 * This follows hexagonal architecture by defining the output port in the application layer
 */
public interface MarketDataPort {
    
    /**
     * Get stock information by symbol
     * @param symbol The stock symbol (e.g., "AAPL")
     * @return StockData or null if not found
     */
    StockData getStockBySymbol(String symbol);
    
    /**
     * Get stock information by ID
     * @param stockId The stock ID
     * @return StockData or null if not found
     */
    StockData getStockById(Long stockId);
    
    /**
     * DTO for stock data
     */
    record StockData(
        long id,
        String symbol,
        BigDecimal lastPrice,
        String name,
        BigDecimal bid,
        BigDecimal ask,
        long volume,
        LocalDateTime timestamp
    ) {}
}
