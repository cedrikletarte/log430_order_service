package com.brokerx.order_service.infrastructure.persistence.repository.order;

import com.brokerx.order_service.infrastructure.persistence.entity.OrderEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


public interface SpringOrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByWalletId(Long walletId);
}
