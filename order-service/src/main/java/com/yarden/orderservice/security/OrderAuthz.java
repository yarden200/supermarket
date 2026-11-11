package com.yarden.orderservice.security;

import com.yarden.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("orderAuthz")
public class OrderAuthz {

    @Autowired
    private OrderRepository orderRepository;

    public boolean canRead(Long orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return false;
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        return orderRepository.findCustomerIdById(orderId)
                .map(customerId -> customerId.equals(user.id()))
                .orElse(true);
    }
}
