package com.brokerx.order_service.application.port.out;

import java.math.BigDecimal;

/**
 * Port de sortie pour communiquer avec le service de wallet
 * Gère les réservations et libérations de fonds
 */
public interface WalletServicePort {
    
    /**
     * Informations sur le solde d'un wallet
     */
    class WalletBalance {
        private final BigDecimal availableBalance;
        private final BigDecimal reservedBalance;
        private final BigDecimal totalBalance;
        
        public WalletBalance(BigDecimal availableBalance, BigDecimal reservedBalance, BigDecimal totalBalance) {
            this.availableBalance = availableBalance;
            this.reservedBalance = reservedBalance;
            this.totalBalance = totalBalance;
        }
        
        public BigDecimal getAvailableBalance() { return availableBalance; }
        public BigDecimal getReservedBalance() { return reservedBalance; }
        public BigDecimal getTotalBalance() { return totalBalance; }
    }
    
    /**
     * Récupère le solde du wallet d'un utilisateur
     */
    WalletBalance getWalletBalance(Long userId);
    
    /**
     * Réserve un montant dans le wallet pour un ordre
     * @param userId L'utilisateur
     * @param amount Le montant à réserver
     * @param orderId L'ID de l'ordre pour traçabilité
     * @return true si la réservation a réussi, false sinon
     */
    boolean reserveFunds(Long userId, BigDecimal amount, String orderId);
    
    /**
     * Libère un montant réservé (en cas d'annulation d'ordre)
     * @param userId L'utilisateur
     * @param amount Le montant à libérer
     * @param orderId L'ID de l'ordre pour traçabilité
     * @return true si la libération a réussi, false sinon
     */
    boolean releaseFunds(Long userId, BigDecimal amount, String orderId);
    
    /**
     * Transfert définitif des fonds réservés vers une transaction (exécution d'ordre)
     * @param userId L'utilisateur
     * @param amount Le montant à transférer
     * @param orderId L'ID de l'ordre pour traçabilité
     * @return true si le transfert a réussi, false sinon
     */
    boolean transferReservedFunds(Long userId, BigDecimal amount, String orderId);
}