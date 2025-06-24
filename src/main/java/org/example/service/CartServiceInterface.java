package org.example.service;

import org.example.model.CartItem;
import org.example.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface CartServiceInterface {
    // Podstawowe operacje CRUD
    List<CartItem> getCartItems(User user);
    void addToCart(Long productId, Integer quantity) throws Exception; // quantity jest ignorowane
    void addToCart(User user, Long productId, Integer quantity) throws Exception; // quantity jest ignorowane
    void removeFromCart(Long cartItemId) throws Exception;
    void clearCart(User user);
    BigDecimal getCartTotal(User user);
    int getCartItemCount(User user);
    boolean isCartEmpty(User user);

    // NOWE METODY - dodane dla CartController
    /**
     * Sprawdza czy można złożyć zamówienie z aktualnym koszykiem
     */
    boolean canPlaceOrder(User user);

    /**
     * Synchronizuje koszyk z dostępnością produktów
     */
    void synchronizeCartWithStock(User user);

    /**
     * Pobiera listę niedostępnych produktów w koszyku
     */
    List<CartItem> getUnavailableCartItems(User user);

    /**
     * Sprawdza czy są niedostępne produkty w koszyku
     */
    boolean hasUnavailableProducts(User user);

    /**
     * Pobiera liczbę niedostępnych produktów
     */
    int getUnavailableProductsCount(User user);
}