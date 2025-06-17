package org.example.dao;

import org.example.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductDaoInterface {
    Optional<Product> findById(Long id);
    List<Product> findAll();
    List<Product> findByCategory(String category);
    List<Product> findActiveProducts();
    List<Product> searchByName(String name);
    void save(Product product);
    void update(Product product);
    void delete(Long id);
}
