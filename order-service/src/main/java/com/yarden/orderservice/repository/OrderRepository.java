package com.yarden.orderservice.repository;

import com.yarden.orderservice.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT o.customerId FROM Order o WHERE o.id = :id")
    Optional<Long> findCustomerIdById(@Param("id") Long id);
}