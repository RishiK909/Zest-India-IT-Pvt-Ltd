package com.zest.products.dto;

public class ItemResponseDTO {

    private Long id;
    private Long productId;
    private Long quantity;

    public ItemResponseDTO(Long id, Long productId, Long quantity) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}