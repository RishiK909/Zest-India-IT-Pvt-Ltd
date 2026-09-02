package com.zest.products.service;

import com.zest.products.dto.*;
import com.zest.products.entity.Item;
import com.zest.products.entity.Product;
import com.zest.products.exception.ResourceNotFoundException;
import com.zest.products.repository.ItemRepository;
import com.zest.products.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all business logic for Product and its related Items.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;

    public ProductServiceImpl(ProductRepository productRepository, ItemRepository itemRepository) {
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Creates a new product. If items are included in the request
     *
     * @param request   product name and optional list of items
     * @param createdBy id of the user creating this product
     * @return the created product, including any items
     */
    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request, Long createdBy) {

        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setCreatedBy(createdBy);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<Item> items = new ArrayList<>();

            for (ItemRequestDTO itemDto : request.getItems()) {
                Item item = new Item();
                item.setQuantity(itemDto.getQuantity());
                item.setProduct(product);
                items.add(item);
            }

            product.setItems(items);
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    /**
     * Fetches a single product by its id.
     *
     * @throws ResourceNotFoundException if no active product exists with this id
     */
    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    /**
     * Fetches all products in a paginated format.
     */
    @Override
    public PagedResponse<ProductResponseDTO> getAllProducts(Pageable pageable) {
        Page<Product> page = productRepository.findAll(pageable);

        List<ProductResponseDTO> content = new ArrayList<>();
        for (Product product : page.getContent()) {
            content.add(mapToResponse(product));
        }

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Updates an existing product's name and, if provided, replaces all its items.
     *
     * @throws ResourceNotFoundException if no active product exists with this id
     */
    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request, Long modifiedBy) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setProductName(request.getProductName());
        product.setModifiedBy(modifiedBy);
        product.setModifiedOn(LocalDateTime.now());

        if (request.getItems() != null) {
            product.getItems().clear();

            List<Item> newItems = new ArrayList<>();
            for (ItemRequestDTO itemDto : request.getItems()) {
                Item item = new Item();
                item.setQuantity(itemDto.getQuantity());
                item.setProduct(product);
                newItems.add(item);
            }

            product.getItems().addAll(newItems);
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    /**
     * Soft-deletes a product and all of its items.
     *
     * @throws ResourceNotFoundException if no active product exists with this id
     */
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        LocalDateTime now = LocalDateTime.now();

        List<Item> items = itemRepository.findAllByProductId(id);
        for (Item item : items) {
            item.setDeletedAt(now);
        }
        itemRepository.saveAll(items);

        product.setDeletedAt(now);
        productRepository.save(product);
    }

    /**
     * Fetches all items belonging to a product, in a paginated format.
     *
     * @throws ResourceNotFoundException if no active product exists with this id
     */
    @Override
    public PagedResponse<ItemResponseDTO> getItemsByProductId(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Page<Item> page = itemRepository.findByProductId(productId, pageable);

        List<ItemResponseDTO> content = new ArrayList<>();
        for (Item item : page.getContent()) {
            content.add(mapToItemResponse(item));
        }

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Converts a Product entity into its response DTO, including its items.
     */
    private ProductResponseDTO mapToResponse(Product product) {
        List<ItemResponseDTO> itemDtos = new ArrayList<>();

        if (product.getItems() != null) {
            for (Item item : product.getItems()) {
                itemDtos.add(mapToItemResponse(item));
            }
        }

        return new ProductResponseDTO(
                product.getId(),
                product.getProductName(),
                product.getCreatedBy(),
                product.getCreatedOn(),
                product.getModifiedBy(),
                product.getModifiedOn(),
                itemDtos
        );
    }

    /**
     * Converts an Item entity into its response DTO.
     */
    private ItemResponseDTO mapToItemResponse(Item item) {
        return new ItemResponseDTO(
                item.getId(),
                item.getProduct().getId(),
                item.getQuantity()
        );
    }
}