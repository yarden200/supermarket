package com.yarden.orderservice.service;

import com.yarden.orderservice.client.ProductClient;
import com.yarden.orderservice.client.dto.StockItemRequest;
import com.yarden.orderservice.client.dto.StockItemResponse;
import com.yarden.orderservice.dto.CreateOrderRequest;
import com.yarden.orderservice.dto.OrderItemRequest;
import com.yarden.orderservice.dto.OrderResponse;
import com.yarden.orderservice.event.OrderEvent;
import com.yarden.orderservice.event.OrderEventType;
import com.yarden.orderservice.exception.ResourceNotFoundException;
import com.yarden.orderservice.mapper.OrderMapper;
import com.yarden.orderservice.model.Order;
import com.yarden.orderservice.model.OrderStatus;
import com.yarden.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for OrderService — Mockito only, no Spring.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ProductClient productClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private OrderService orderService;

    @Test
    void create_buildsOrderFromStockResponseAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)));

        when(productClient.decreaseStock(any())).thenReturn(List.of(
                new StockItemResponse(1L, "Milk", new BigDecimal("6.90"), 38),
                new StockItemResponse(2L, "Bread", new BigDecimal("12.50"), 9)));
        // save returns the same order it received
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderEvent event = mock(OrderEvent.class);
        OrderResponse response = mock(OrderResponse.class);
        when(orderMapper.toEvent(any(Order.class), eq(OrderEventType.CREATED))).thenReturn(event);
        when(orderMapper.toResponse(any(Order.class), anyMap())).thenReturn(response);

        OrderResponse result = orderService.create(request, 7L);

        assertThat(result).isSameAs(response);

        // The order that was saved
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(7L);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("26.30"); // 6.90 * 2 + 12.50

        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void create_mergesDuplicateProductsIntoOneStockRequest() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(1L, 1)));

        when(productClient.decreaseStock(any())).thenReturn(List.of(
                new StockItemResponse(1L, "Milk", new BigDecimal("6.90"), 37)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.create(request, 7L);

        verify(productClient).decreaseStock(List.of(new StockItemRequest(1L, 3)));
    }

    @Test
    void getById_whenMissing_throws() {
        when(orderRepository.findWithItemsById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}