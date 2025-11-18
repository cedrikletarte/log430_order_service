package com.brokerx.order_service.application.port.out;

import com.brokerx.order_service.domain.model.Order;
import java.util.Optional;
import java.util.List;

public interface OrderRepositoryPort {

    /* Saving an order */
    Order save(Order order);

    /* Finding an order by ID */
    Optional<Order> findById(Long id);

    /* Finding all orders */
    List<Order> findAll();

    /* Finding orders by wallet ID */
    List<Order> findByWalletId(Long walletId);

    /* Deleting an order by ID */
    void deleteById(Long id);

    /* Deleting all orders */
    void deleteAll();

    /* Checking if an order exists by ID */
    boolean existsById(Long id);
}