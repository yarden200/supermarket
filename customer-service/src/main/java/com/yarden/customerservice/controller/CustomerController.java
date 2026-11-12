package com.yarden.customerservice.controller;

import com.yarden.customerservice.dto.CustomerRequest;
import com.yarden.customerservice.dto.CustomerResponse;
import com.yarden.customerservice.security.AuthUser;
import com.yarden.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // ---------- The logged-in user's own profile ----------

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponse> getMyProfile(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(customerService.getByUserId(user.id()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponse> saveMyProfile(@AuthenticationPrincipal AuthUser user,
                                                          @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.saveProfile(user.id(), user.username(), request));
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal AuthUser user) {
        customerService.delete(user.id());
        return ResponseEntity.noContent().build();
    }

    // ---------- Admin ----------

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedModel<CustomerResponse>> getAllCustomers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(customerService.getAll(pageable)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomerByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(customerService.getByUserId(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long userId,
                                                           @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long userId) {
        customerService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}