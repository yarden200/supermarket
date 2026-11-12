package com.yarden.customerservice.service;

import com.yarden.customerservice.dto.CustomerRequest;
import com.yarden.customerservice.dto.CustomerResponse;
import com.yarden.customerservice.exception.ResourceNotFoundException;
import com.yarden.customerservice.mapper.CustomerMapper;
import com.yarden.customerservice.model.Customer;
import com.yarden.customerservice.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public CustomerResponse getByUserId(Long userId) {
        return customerMapper.toResponse(findByUserIdOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAll(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(customerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse saveProfile(Long userId, String username, CustomerRequest request) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseGet(() -> Customer.builder().userId(userId).build());

        customer.setUsername(username);
        customerMapper.updateEntity(request, customer);

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long userId, CustomerRequest request) {
        Customer customer = findByUserIdOrThrow(userId);
        customerMapper.updateEntity(request, customer);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void delete(Long userId) {
        Customer customer = findByUserIdOrThrow(userId);
        customerRepository.delete(customer);
    }

    private Customer findByUserIdOrThrow(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile for user", userId));
    }
}
