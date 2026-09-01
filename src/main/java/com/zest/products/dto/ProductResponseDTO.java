package com.zest.products.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProductResponseDTO {

    private Long id;
    private String productName;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long modifiedBy;
    private LocalDateTime modifiedOn;
    private List<ItemResponseDTO> items;

    public ProductResponseDTO(Long id, String productName, Long createdBy, LocalDateTime createdOn, Long modifiedBy, LocalDateTime modifiedOn, List<ItemResponseDTO> items) {
        this.id = id;
        this.productName = productName;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.modifiedBy = modifiedBy;
        this.modifiedOn = modifiedOn;
        this.items = items;
    }

    public ProductResponseDTO(Long id, String productName, Long createdBy,
                              LocalDateTime createdOn, Long modifiedBy, LocalDateTime modifiedOn) {
        this.id = id;
        this.productName = productName;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.modifiedBy = modifiedBy;
        this.modifiedOn = modifiedOn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedOn(LocalDateTime modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public List<ItemResponseDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemResponseDTO> items) {
        this.items = items;
    }
}