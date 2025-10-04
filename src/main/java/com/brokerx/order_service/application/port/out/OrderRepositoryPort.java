package com.brokerx.order_service.application.port.out;

import com.brokerx.order_service.domain.model.Order;
import java.util.Optional;
import java.util.List;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findByWalletId(Long walletId);

    void deleteById(Long id);
}