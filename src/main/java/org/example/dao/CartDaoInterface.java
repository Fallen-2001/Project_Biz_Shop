package org.example.dao;

import org.example.model.CartItem;
import org.example.model.User;
import org.example.model.Product;
import java.util.List;
import java.util.Optional;

public interface CartDaoInterface {

    // Podstawowe operacje CRUD
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    void save(CartItem cartItem);
    void update(CartItem cartItem);
    CartItem updateAndRefresh(CartItem cartItem); // Aktualizacja z odświeżeniem
    void delete(Long id);
    void deleteByUser(User user);
    int countByUser(User user);

    // Dodatkowe metody pomocnicze
    Optional<CartItem> findByIdAndUser(Long id, User user);
    List<CartItem> findByUserWithProducts(User user); // Explicit eager loading

    // Metody do czyszczenia starych elementów koszyka
    List<CartItem> findStaleCartItems(int daysOld);
    void deleteStaleCartItems(int daysOld);

    // Metody do obsługi nieaktywnych produktów
    List<CartItem> findCartItemsWithInactiveProducts();
    int removeCartItemsWithInactiveProducts();

    // Sprawdzanie istnienia
    boolean existsByUserAndProduct(User user, Product product);

    // Blokowanie dla współbieżności
    Optional<CartItem> findWithLock(Long id);
}