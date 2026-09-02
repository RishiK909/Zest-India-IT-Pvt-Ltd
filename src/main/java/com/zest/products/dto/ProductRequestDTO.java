package com.zest.products.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ProductRequestDTO {

    @NotBlank(message = "Product name is required")
    private String productName;

    @Valid
    private List<ItemRequestDTO> items;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public List<ItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemRequestDTO> items) {
        this.items = items;
    }
}
