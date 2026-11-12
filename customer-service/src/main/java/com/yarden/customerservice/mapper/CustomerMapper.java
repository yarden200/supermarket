package com.yarden.customerservice.mapper;

import com.yarden.customerservice.dto.CustomerRequest;
import com.yarden.customerservice.dto.CustomerResponse;
import com.yarden.customerservice.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getUserId(),
                customer.getUsername(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress()
        );
    }

    public void updateEntity(CustomerRequest request, Customer customer) {
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
    }
}