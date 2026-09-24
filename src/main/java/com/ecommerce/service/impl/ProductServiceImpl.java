package com.ecommerce.service.impl;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.specification.ProductSpecifications;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public PageResponse<ProductResponse> getProducts(Long categoryId, String keyword, Pageable pageable) {
        Page<ProductResponse> page = productRepository
                .findAll(ProductSpecifications.withFilters(categoryId, keyword), pageable)
                .map(productMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findActiveProductOrThrow(id));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String sku = productMapper.normalizeSku(request.sku());
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product", "sku", sku);
        }

        Category category = findCategoryOrThrow(request.categoryId());
        Product saved = productRepository.save(productMapper.toEntity(request, category));
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findActiveProductOrThrow(id);

        String sku = productMapper.normalizeSku(request.sku());
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException("Product", "sku", sku);
        }

        Category category = findCategoryOrThrow(request.categoryId());
        productMapper.updateEntity(product, request, category);
        Product updated = productRepository.saveAndFlush(product);
        return productMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findActiveProductOrThrow(id);
        product.setActive(false);
    }

    private Product findActiveProductOrThrow(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }
}