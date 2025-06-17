package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.dao.ProductDao;
import org.example.model.Product;
import java.util.List;

@ApplicationScoped
public class ProductService {
    @Inject
    private ProductDao productDao;

    public List<Product> getAllProducts() {
        return productDao.findAll();
    }

    public void addProduct(Product product) {
        if (product != null && product.getName() != null && !product.getName().trim().isEmpty()) {
            productDao.save(product);
        }
    }

    public void deleteProduct(Long productId) {
        if (productId != null) {
            productDao.delete(productId);
        }
    }
}