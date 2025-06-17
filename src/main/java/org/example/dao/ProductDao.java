package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ProductDao {
    private final List<Product> products = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Product> findAll() {
        return new ArrayList<>(products);
    }

    public void save(Product product) {
        if (product.getId() == null) {
            product.setId(idGenerator.getAndIncrement());
        } else {
            delete(product.getId());
        }
        products.add(product);
    }

    public void delete(Long productId) {
        products.removeIf(p -> p.getId().equals(productId));
    }
}