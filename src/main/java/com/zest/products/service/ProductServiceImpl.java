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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;

    public ProductServiceImpl(ProductRepository productRepository, ItemRepository itemRepository) {
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request, Long createdBy) {

        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setCreatedBy(createdBy);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<Item> items = request.getItems().stream()
                    .map(itemDto -> {
                        Item item = new Item();
                        item.setQuantity(itemDto.getQuantity());
                        item.setProduct(product);
                        return item;
                    })
                    .collect(Collectors.toList());

            product.setItems(items);
        }

        Product saved = productRepository.save(product);   // cascade se items bhi save ho jayenge
        return mapToResponse(saved);
    }

    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    public PagedResponse<ProductResponseDTO> getAllProducts(Pageable pageable) {
        Page<Product> page = productRepository.findAll(pageable);

        List<ProductResponseDTO> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

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

            List<Item> newItems = request.getItems().stream()
                    .map(itemDto -> {
                        Item item = new Item();
                        item.setQuantity(itemDto.getQuantity());
                        item.setProduct(product);
                        return item;
                    })
                    .toList();

            product.getItems().addAll(newItems);
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

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

    @Override
    public PagedResponse<ItemResponseDTO> getItemsByProductId(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Page<Item> page = itemRepository.findByProductId(productId, pageable);

        List<ItemResponseDTO> content = page.getContent()
                .stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private ProductResponseDTO mapToResponse(Product product) {
        List<ItemResponseDTO> itemDtos = product.getItems() != null
                ? product.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList())
                : List.of();

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

    private ItemResponseDTO mapToItemResponse(Item item) {
        return new ItemResponseDTO(
                item.getId(),
                item.getProduct().getId(),
                item.getQuantity()
        );
    }
}