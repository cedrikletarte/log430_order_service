package com.brokerx.order_service.infrastructure.persistence.repository.order;

import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.infrastructure.persistence.entity.OrderEntity;
import com.brokerx.order_service.infrastructure.persistence.mapper.OrderMapper;

import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
@Transactional
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final SpringOrderRepository springOrderRepository;
    private final OrderMapper orderMapper;

    public OrderRepositoryAdapter(SpringOrderRepository springOrderRepository, OrderMapper orderMapper) {
        this.springOrderRepository = springOrderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = orderMapper.toEntity(order);
        entity = springOrderRepository.save(entity);
        return orderMapper.toDomain(entity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return springOrderRepository.findById(id)
                .map(orderMapper::toDomain);
    }

    @Override
    public List<Order> findByWalletId(Long walletId) {
        return springOrderRepository.findByWalletId(walletId)
                .stream()
                .map(orderMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findAll() {
        return springOrderRepository.findAll().stream()
                .map(orderMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        springOrderRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        springOrderRepository.deleteAll();
    }

    @Override
    public boolean existsById(Long id) {
        return springOrderRepository.existsById(id);
    }

}
