package org.example.module.service;

import org.example.module.entity.Product;
import org.example.module.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.info("=== ĐANG TRUY VẤN DATABASE CHO PRODUCT ID: {} ===", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @CacheEvict(value = "products", key = "#product.id")
    @Transactional
    public Product updateProduct(Product product) {
        log.info("=== XÓA CACHE CHO PRODUCT ID: {} KHI CẬP NHẬT ===", product.getId());
        return productRepository.save(product);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
}