package org.example.service;

import org.example.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductServiceInterface {
    List<Product> getAllProducts();
    List<Product> getActiveProducts();
    List<Product> getProductsByCategory(String category);
    Optional<Product> getProductById(Long id);
    List<Product> searchProducts(String searchTerm);
    void addProduct(Product product) throws Exception;
    void updateProduct(Product product) throws Exception;
    void deleteProduct(Long id) throws Exception;
}
