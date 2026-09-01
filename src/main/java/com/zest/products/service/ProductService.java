package com.zest.products.service;

import com.zest.products.dto.PagedResponse;
import com.zest.products.dto.ProductRequestDTO;
import com.zest.products.dto.ProductResponseDTO;
import com.zest.products.dto.ItemResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO request, Long createdBy);

    ProductResponseDTO getProductById(Long id);

    PagedResponse<ProductResponseDTO> getAllProducts(Pageable pageable);

    ProductResponseDTO updateProduct(Long id, ProductRequestDTO request, Long modifiedBy);

    void deleteProduct(Long id);

    PagedResponse<ItemResponseDTO> getItemsByProductId(Long productId, Pageable pageable);
}