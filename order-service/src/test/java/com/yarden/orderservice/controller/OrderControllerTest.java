package com.yarden.orderservice.controller;

import com.yarden.orderservice.dto.OrderItemResponse;
import com.yarden.orderservice.dto.OrderResponse;
import com.yarden.orderservice.exception.ResourceNotFoundException;
import com.yarden.orderservice.model.OrderStatus;
import com.yarden.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for OrderController — only the web layer is loaded, the service is mocked.
 * Security is left out (excluded package + filters off); permissions were checked in Postman.
 */
@WebMvcTest(controllers = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "com\\.yarden\\.orderservice\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderService orderService;

    @Test
    void getOrderById_whenPresent_returns200() throws Exception {
        OrderResponse response = new OrderResponse(
                1L, 7L, OrderStatus.PENDING, new BigDecimal("13.80"), Instant.now(),
                List.of(new OrderItemResponse(1L, "Milk", 2,
                        new BigDecimal("6.90"), new BigDecimal("13.80"), null)));
        when(orderService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productName").value("Milk"));
    }

    @Test
    void getOrderById_whenMissing_returns404() throws Exception {
        when(orderService.getById(999L)).thenThrow(new ResourceNotFoundException("Order", 999L));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }
}