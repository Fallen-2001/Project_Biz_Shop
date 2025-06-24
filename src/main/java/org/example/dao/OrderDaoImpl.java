package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderDaoImpl implements OrderDaoInterface {

    private static final Logger logger = LoggerFactory.getLogger(OrderDaoImpl.class);

    @PersistenceContext(unitName = "shopPU")
    private EntityManager em;

    @Override
    public Optional<Order> findById(Long id) {
        if (id == null) {
            logger.debug("Finding order by id: null - returning empty");
            return Optional.empty();
        }

        logger.debug("Finding order by id: {}", id);
        try {
            Order order = em.find(Order.class, id);
            if (order != null) {
                // Wymuszenie załadowania pozycji zamówienia
                order.getOrderItems().size();
                logger.debug("Found order: ID={}, ItemsCount={}", order.getId(), order.getOrderItems().size());
            }
            return Optional.ofNullable(order);
        } catch (Exception e) {
            logger.error("Error finding order by id: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Order> findByUser(User user) {
        if (user == null) {
            logger.debug("Finding orders by user: null - returning empty list");
            return List.of();
        }

        logger.debug("Finding orders by user: {} (ID: {})", user.getUsername(), user.getId());
        try {
            // Eager loading zamówień z pozycjami
            TypedQuery<Order> query = em.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.product " +
                            "WHERE o.user = :user " +
                            "ORDER BY o.orderDate DESC", Order.class);
            query.setParameter("user", user);

            List<Order> orders = query.getResultList();
            logger.info("Found {} orders for user: {}", orders.size(), user.getUsername());

            // Log szczegółów dla debugowania
            for (Order order : orders) {
                logger.debug("Order: ID={}, Date={}, Status={}, Total={}, ItemsCount={}",
                        order.getId(), order.getOrderDate(), order.getStatus(),
                        order.getTotalAmount(), order.getOrderItems().size());
            }

            return orders;
        } catch (Exception e) {
            logger.error("Error finding orders by user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to find orders for user: " + user.getUsername(), e);
        }
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        if (status == null) {
            logger.debug("Finding orders by status: null - returning empty list");
            return List.of();
        }

        logger.debug("Finding orders by status: {}", status);
        try {
            TypedQuery<Order> query = em.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.product " +
                            "WHERE o.status = :status " +
                            "ORDER BY o.orderDate DESC", Order.class);
            query.setParameter("status", status);

            List<Order> orders = query.getResultList();
            logger.info("Found {} orders with status: {}", orders.size(), status);
            return orders;
        } catch (Exception e) {
            logger.error("Error finding orders by status: {}", status, e);
            throw new RuntimeException("Failed to find orders by status: " + status, e);
        }
    }

    @Override
    public List<Order> findAll() {
        logger.debug("Finding all orders");
        try {
            TypedQuery<Order> query = em.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.product " +
                            "ORDER BY o.orderDate DESC", Order.class);

            List<Order> orders = query.getResultList();
            logger.info("Found {} total orders", orders.size());
            return orders;
        } catch (Exception e) {
            logger.error("Error finding all orders", e);
            throw new RuntimeException("Failed to find all orders", e);
        }
    }

    @Override
    @Transactional
    public void save(Order order) {
        if (order == null) {
            logger.error("Attempting to save null order");
            throw new IllegalArgumentException("Order cannot be null");
        }

        logger.debug("Saving order for user: {}", order.getUser() != null ? order.getUser().getUsername() : "null");
        try {
            em.persist(order);
            em.flush(); // Wymuszenie zapisu
            logger.info("Order saved successfully: ID={}", order.getId());
        } catch (Exception e) {
            logger.error("Error saving order", e);
            throw new RuntimeException("Failed to save order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void update(Order order) {
        if (order == null) {
            logger.error("Attempting to update null order");
            throw new IllegalArgumentException("Order cannot be null");
        }

        logger.debug("Updating order: {}", order.getId());
        try {
            em.merge(order);
            em.flush(); // Wymuszenie zapisu
            logger.info("Order updated successfully: {}", order.getId());
        } catch (Exception e) {
            logger.error("Error updating order: {}", order.getId(), e);
            throw new RuntimeException("Failed to update order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            logger.error("Attempting to delete order with null id");
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        logger.debug("Deleting order: {}", id);
        try {
            Order order = em.find(Order.class, id);
            if (order != null) {
                em.remove(order);
                em.flush(); // Wymuszenie usunięcia
                logger.info("Order deleted successfully: {}", id);
            } else {
                logger.warn("Order not found for deletion: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting order: {}", id, e);
            throw new RuntimeException("Failed to delete order: " + e.getMessage(), e);
        }
    }

    @Override
    public void flush() {
        try {
            em.flush();
            logger.debug("EntityManager flushed successfully");
        } catch (Exception e) {
            logger.error("Error flushing EntityManager", e);
            throw new RuntimeException("Failed to flush changes to database", e);
        }
    }

    @Override
    public List<Order> findByUserWithItems(User user) {
        // Ta metoda robi to samo co findByUser (już ma eager loading)
        return findByUser(user);
    }

    @Override
    public List<Order> findAllWithItems() {
        // Ta metoda robi to samo co findAll (już ma eager loading)
        return findAll();
    }

    @Override
    public Optional<Order> findByIdWithItems(Long id) {
        // Ta metoda robi to samo co findById (już ma eager loading)
        return findById(id);
    }

    @Override
    public boolean hasOrders(User user) {
        if (user == null) {
            return false;
        }

        logger.debug("Checking if user has orders: {}", user.getUsername());
        try {
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(o) FROM Order o WHERE o.user = :user", Long.class);
            query.setParameter("user", user);

            Long count = query.getSingleResult();
            boolean hasOrders = count != null && count > 0;
            logger.debug("User {} has {} orders", user.getUsername(), count);
            return hasOrders;
        } catch (Exception e) {
            logger.error("Error checking if user has orders: {}", user.getUsername(), e);
            return false;
        }
    }

    @Override
    public long countByUser(User user) {
        if (user == null) {
            return 0;
        }

        logger.debug("Counting orders for user: {}", user.getUsername());
        try {
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(o) FROM Order o WHERE o.user = :user", Long.class);
            query.setParameter("user", user);

            Long count = query.getSingleResult();
            long result = count != null ? count : 0;
            logger.debug("User {} has {} orders", user.getUsername(), result);
            return result;
        } catch (Exception e) {
            logger.error("Error counting orders for user: {}", user.getUsername(), e);
            return 0;
        }
    }

    @Override
    public List<Order> findRecentByUser(User user, int limit) {
        if (user == null || limit <= 0) {
            return List.of();
        }

        logger.debug("Finding {} recent orders for user: {}", limit, user.getUsername());
        try {
            TypedQuery<Order> query = em.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.product " +
                            "WHERE o.user = :user " +
                            "ORDER BY o.orderDate DESC", Order.class);
            query.setParameter("user", user);
            query.setMaxResults(limit);

            List<Order> orders = query.getResultList();
            logger.debug("Found {} recent orders for user: {}", orders.size(), user.getUsername());
            return orders;
        } catch (Exception e) {
            logger.error("Error finding recent orders for user: {}", user.getUsername(), e);
            return List.of();
        }
    }

    // DODATKOWE METODY POMOCNICZE

    /**
     * Pobiera zamówienia z określonego zakresu dat
     */
    public List<Order> findByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        logger.debug("Finding orders between {} and {}", startDate, endDate);
        try {
            TypedQuery<Order> query = em.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.product " +
                            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
                            "ORDER BY o.orderDate DESC", Order.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);

            List<Order> orders = query.getResultList();
            logger.debug("Found {} orders in date range", orders.size());
            return orders;
        } catch (Exception e) {
            logger.error("Error finding orders by date range", e);
            return List.of();
        }
    }

    /**
     * Pobiera zamówienia po minimalnej wartości
     */
    public List<Order> findByMinAmount(java.math.BigDecimal minAmount) {
        logger.debug("Finding orders with amount >= {}", minAmount);
        try {
            TypedQuery<Order> query = em.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.product " +
                            "WHERE o.totalAmount >= :minAmount " +
                            "ORDER BY o.totalAmount DESC", Order.class);
            query.setParameter("minAmount", minAmount);

            List<Order> orders = query.getResultList();
            logger.debug("Found {} orders with amount >= {}", orders.size(), minAmount);
            return orders;
        } catch (Exception e) {
            logger.error("Error finding orders by min amount", e);
            return List.of();
        }
    }
}