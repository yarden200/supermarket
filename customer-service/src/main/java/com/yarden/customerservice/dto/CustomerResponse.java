package com.yarden.customerservice.dto;

public record CustomerResponse(
        Long id,
        Long userId,
        String username,
        String fullName,
        String email,
        String phone,
        String address
) {}