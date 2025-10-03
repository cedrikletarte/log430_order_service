package com.brokerx.order_service.application.port.out;

import java.math.BigDecimal;

/**
 * Port de sortie pour récupérer les données de marché
 */
public interface MarketDataServicePort {
    
    /**
     * Informations sur un instrument financier
     */
    class InstrumentInfo {
        private final String symbol;
        private final boolean isActive;
        private final BigDecimal currentPrice;
        private final BigDecimal bidPrice;
        private final BigDecimal askPrice;
        private final BigDecimal tickSize;
        private final BigDecimal minLotSize;
        private final BigDecimal maxOrderSize;
        
        public InstrumentInfo(String symbol, boolean isActive, BigDecimal currentPrice, 
                            BigDecimal bidPrice, BigDecimal askPrice, BigDecimal tickSize, 
                            BigDecimal minLotSize, BigDecimal maxOrderSize) {
            this.symbol = symbol;
            this.isActive = isActive;
            this.currentPrice = currentPrice;
            this.bidPrice = bidPrice;
            this.askPrice = askPrice;
            this.tickSize = tickSize;
            this.minLotSize = minLotSize;
            this.maxOrderSize = maxOrderSize;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public boolean isActive() { return isActive; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getBidPrice() { return bidPrice; }
        public BigDecimal getAskPrice() { return askPrice; }
        public BigDecimal getTickSize() { return tickSize; }
        public BigDecimal getMinLotSize() { return minLotSize; }
        public BigDecimal getMaxOrderSize() { return maxOrderSize; }
    }
    
    /**
     * Récupère les informations d'un instrument
     */
    InstrumentInfo getInstrumentInfo(String symbol);
    
    /**
     * Récupère le prix actuel d'un instrument
     */
    BigDecimal getCurrentPrice(String symbol);
    
    /**
     * Vérifie si un instrument est actif pour le trading
     */
    boolean isSymbolActive(String symbol);
}