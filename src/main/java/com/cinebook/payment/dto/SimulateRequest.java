package com.cinebook.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimulateRequest(@NotBlank String txnRef, @NotNull SimulatedResult result) {
}
