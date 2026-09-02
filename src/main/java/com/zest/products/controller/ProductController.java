package com.zest.products.controller;

import com.zest.products.dto.*;
import com.zest.products.entity.Product;
import com.zest.products.entity.User;
import com.zest.products.exception.ResourceNotFoundException;
import com.zest.products.repository.UserRepository;
import com.zest.products.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Products
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "APIs for creating, updating, fetching and deleting products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }


    // ------------------------------------ Create Product -------------------------------------------------------------

    @Operation(
            summary = "Create a new product",
            description = "Creates a new product in the catalog. Only accessible to Admin users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        ProductResponseDTO created = productService.createProduct(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO<>("Product created successfully", true, created));
    }



    // -------------------------------------------- Get Product By Id --------------------------------------------------
    @Operation(
            summary = "Get product by ID",
            description = "Fetches a single active (non-deleted) product by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found or has been deleted")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin','User')")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> getProductById(
            @Parameter(description = "ID of the product to fetch") @PathVariable Long id) {

        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(new ApiResponseDTO<>("Product fetched successfully", true, product));
    }


    // -------------------------------------------- Get All Product --------------------------------------------------

    @Operation(
            summary = "Get all products",
            description = "Fetches a paginated list of all active products. Supports sorting via Pageable query params (e.g. page, size, sort)."
    )
    @ApiResponse(responseCode = "200", description = "Products fetched successfully")
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','User')")
    public ResponseEntity<ApiResponseDTO<PagedResponse<ProductResponseDTO>>> getAllProducts(Pageable pageable) {
        PagedResponse<ProductResponseDTO> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(new ApiResponseDTO<>("Products fetched successfully", true, products));
    }


    // -------------------------------------------- Update a product --------------------------------------------------

    @Operation(
            summary = "Update a product",
            description = "Updates the details of an existing product. Only accessible to Admin users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found or has been deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> updateProduct(
            @Parameter(description = "ID of the product to update") @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        ProductResponseDTO updated = productService.updateProduct(id, request, userId);

        return ResponseEntity.ok(new ApiResponseDTO<>("Product updated successfully", true, updated));
    }



    // -------------------------------------------- Delete a product ---------------------------------------------------

    @Operation(
            summary = "Delete a product",
            description = "Soft-deletes a product by setting its deletedAt timestamp. The product is not physically removed from the database. Only accessible to Admin users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found or already deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponseDTO<Void>> deleteProduct(
            @Parameter(description = "ID of the product to delete") @PathVariable Long id) {

        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponseDTO<>("Product deleted successfully", true));
    }


    // -------------------------------------------- Get items by product ID --------------------------------------------

    @Operation(
            summary = "Get items by product ID",
            description = "Fetches a paginated list of items belonging to a specific product."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found or has been deleted")
    })
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('Admin','User')")
    public ResponseEntity<ApiResponseDTO<PagedResponse<ItemResponseDTO>>> getItemsByProductId(
            @Parameter(description = "ID of the product whose items should be fetched") @PathVariable Long id,
            Pageable pageable) {

        PagedResponse<ItemResponseDTO> items = productService.getItemsByProductId(id, pageable);
        return ResponseEntity.ok(new ApiResponseDTO<>("Items fetched successfully", true, items));
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return user.getUserId();
    }
}