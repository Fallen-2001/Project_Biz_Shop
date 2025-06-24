package org.example.dao;

import org.example.model.Order;
import org.example.model.User;
import org.example.model.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderDaoInterface {
    Optional<Order> findById(Long id);
    List<Order> findByUser(User user);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findAll();
    void save(Order order);
    void update(Order order);
    void delete(Long id);

    // NOWE METODY
    /**
     * Wymusza zapis zmian do bazy danych
     */
    void flush();

    /**
     * Pobiera zamówienia użytkownika z eager loading pozycji
     */
    List<Order> findByUserWithItems(User user);

    /**
     * Pobiera wszystkie zamówienia z eager loading pozycji
     */
    List<Order> findAllWithItems();

    /**
     * Pobiera zamówienie po ID z eager loading pozycji
     */
    Optional<Order> findByIdWithItems(Long id);

    /**
     * Sprawdza czy użytkownik ma zamówienia
     */
    boolean hasOrders(User user);

    /**
     * Liczy zamówienia użytkownika
     */
    long countByUser(User user);

    /**
     * Pobiera ostatnie N zamówień użytkownika
     */
    List<Order> findRecentByUser(User user, int limit);
}