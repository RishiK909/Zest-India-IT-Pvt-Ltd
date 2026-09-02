package com.zest.products.repository;

import com.zest.products.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByProductId(Long productId, Pageable pageable);

    List<Item> findAllByProductId(Long id);
}