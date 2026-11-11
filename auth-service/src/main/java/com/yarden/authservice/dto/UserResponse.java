package com.yarden.authservice.dto;

import java.util.List;

public record UserResponse(Long id, String username, List<String> roles) {}
