package org.example.service;

import org.example.model.CartItem;
import org.example.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface CartServiceInterface {
    List<CartItem> getCartItems(User user);
    void addToCart(Long productId, Integer quantity) throws Exception; // quantity jest ignorowane
    void addToCart(User user, Long productId, Integer quantity) throws Exception; // quantity jest ignorowane
    void removeFromCart(Long cartItemId) throws Exception;
    void clearCart(User user);
    BigDecimal getCartTotal(User user);
    int getCartItemCount(User user);
    boolean isCartEmpty(User user);
}