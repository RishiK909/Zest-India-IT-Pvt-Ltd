package com.zest.products.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ItemRequestDTO {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Long quantity;

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}