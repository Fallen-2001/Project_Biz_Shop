package org.example.dao;

import org.example.model.CartItem;
import org.example.model.User;
import org.example.model.Product;
import java.util.List;
import java.util.Optional;

public interface CartDaoInterface {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    void save(CartItem cartItem);
    void update(CartItem cartItem);
    void delete(Long id);
    void deleteByUser(User user);
    int countByUser(User user);
}