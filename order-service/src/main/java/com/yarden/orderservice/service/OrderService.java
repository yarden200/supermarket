package com.yarden.orderservice.service;

import com.yarden.orderservice.client.ProductClient;
import com.yarden.orderservice.client.dto.StockItemRequest;
import com.yarden.orderservice.client.dto.StockItemResponse;
import com.yarden.orderservice.dto.CreateOrderRequest;
import com.yarden.orderservice.dto.OrderItemRequest;
import com.yarden.orderservice.dto.OrderResponse;
import com.yarden.orderservice.dto.UpdateOrderStatusRequest;
import com.yarden.orderservice.exception.ResourceNotFoundException;
import com.yarden.orderservice.mapper.OrderMapper;
import com.yarden.orderservice.model.Order;
import com.yarden.orderservice.model.OrderItem;
import com.yarden.orderservice.model.OrderStatus;
import com.yarden.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductClient productClient;

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponse);
    }

    public OrderResponse create(CreateOrderRequest request) {
        Map<Long, Integer> quantities = request.items().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum,
                        LinkedHashMap::new));

        List<StockItemRequest> stockRequest = quantities.entrySet().stream()
                .map(e -> new StockItemRequest(e.getKey(), e.getValue()))
                .toList();

        List<StockItemResponse> stock = productClient.decreaseStock(stockRequest);
        Map<Long, StockItemResponse> productsById = stock.stream()
                .collect(Collectors.toMap(StockItemResponse::productId, Function.identity()));

        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            StockItemResponse product = productsById.get(entry.getKey());
            OrderItem item = OrderItem.builder()
                    .productId(product.productId())
                    .productName(product.name())
                    .quantity(entry.getValue())
                    .priceAtPurchase(product.price())
                    .build();
            order.addItem(item);
            total = total.add(product.price().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        order.setTotalPrice(total.setScale(2));

        Order saved = orderRepository.save(order);

        Map<Long, Integer> remainingStock = stock.stream()
                .collect(Collectors.toMap(StockItemResponse::productId, StockItemResponse::remainingStock));
        return orderMapper.toResponse(saved, remainingStock);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        order.setStatus(request.status());
        return orderMapper.toResponse(order);
    }
}
