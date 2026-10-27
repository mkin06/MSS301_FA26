package com.mss301.payment.dto;

import jakarta.validation.constraints.*;

public record PaymentRequest(@NotNull @Positive Long orderId, @NotBlank String customerId, @Positive double amount) {}
