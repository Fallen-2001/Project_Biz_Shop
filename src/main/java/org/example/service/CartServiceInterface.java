package org.example.service;

import org.example.model.CartItem;
import org.example.model.Product;
import org.example.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface CartServiceInterface {
    List<CartItem> getCartItems(User user);
    void addToCart(Long productId, Integer quantity) throws Exception;
    void addToCart(User user, Long productId, Integer quantity) throws Exception;
    void updateCartItemQuantity(Long cartItemId, Integer newQuantity) throws Exception;
    void removeFromCart(Long cartItemId) throws Exception;
    void clearCart(User user);
    BigDecimal getCartTotal(User user);
    int getCartItemCount(User user);
    boolean isCartEmpty(User user);
}