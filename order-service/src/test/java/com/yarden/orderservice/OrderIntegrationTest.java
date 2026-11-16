package com.yarden.orderservice;

import com.yarden.orderservice.client.ProductClient;
import com.yarden.orderservice.client.dto.StockItemResponse;
import com.yarden.orderservice.model.Order;
import com.yarden.orderservice.repository.OrderRepository;
import com.yarden.orderservice.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test — the whole service runs against H2.
 * Only product-service is replaced (the HTTP client is mocked).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;

    @MockitoBean private ProductClient productClient;

    @Test
    void createOrder_savesOrderInDatabase() throws Exception {
        when(productClient.decreaseStock(any())).thenReturn(List.of(
                new StockItemResponse(1L, "Milk", new BigDecimal("6.90"), 38)));

        // Logged-in user with id 7, without a real JWT
        var user = new UsernamePasswordAuthenticationToken(
                new AuthUser(7L, "user"), "token", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(post("/api/orders")
                        .with(authentication(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "productId": 1, "quantity": 2 } ] }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(13.80));

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getCustomerId()).isEqualTo(7L);
    }
}