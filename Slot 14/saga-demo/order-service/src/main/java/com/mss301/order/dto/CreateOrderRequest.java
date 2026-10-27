package com.mss301.order.dto;

import jakarta.validation.constraints.*;

public record CreateOrderRequest(@NotBlank String customerId, @NotBlank String productId, @Positive int quantity, @Positive double amount) {}
