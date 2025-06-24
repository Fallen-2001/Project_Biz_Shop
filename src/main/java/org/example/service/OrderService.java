package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.dao.CartDaoInterface;
import org.example.dao.OrderDaoInterface;
import org.example.dao.ProductDaoInterface;
import org.example.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Inject
    private OrderDaoInterface orderDao;

    @Inject
    private CartDaoInterface cartDao;

    @Inject
    private ProductDaoInterface productDao;

    @Inject
    private CartServiceInterface cartService;

    @Inject
    private EmailService emailService;

    @Inject
    private AuthServiceInterface authService;

    @Transactional
    public Order createOrderFromCart(User user, String shippingAddress) throws Exception {
        logger.debug("Creating order from cart for user: {}", user.getUsername());

        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            throw new Exception("Adres dostawy jest wymagany");
        }

        List<CartItem> cartItems = cartDao.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new Exception("Koszyk jest pusty");
        }

        logger.info("Creating order with {} cart items for user: {}", cartItems.size(), user.getUsername());

        // Sprawdź dostępność produktów
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new Exception("Produkt " + product.getName() + " nie jest już dostępny");
            }
            if (!product.isAvailable(cartItem.getQuantity())) {
                throw new Exception("Niewystarczająca ilość produktu " + product.getName() +
                        ". Dostępne: " + product.getStockQuantity());
            }
        }

        // Utwórz zamówienie
        Order order = new Order(user, shippingAddress.trim());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        // Dodaj pozycje zamówienia
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            OrderItem orderItem = new OrderItem(product, cartItem.getQuantity(), product.getPrice());
            order.addOrderItem(orderItem);

            // Zmniejsz stan magazynowy
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productDao.update(product);
        }

        // Oblicz łączną kwotę
        order.calculateTotalAmount();

        // Zapisz zamówienie
        orderDao.save(order);

        // Wymuś flush do bazy danych
        orderDao.flush(); // Dodajemy tę metodę do interface'u

        logger.info("Order created successfully: ID={}, Total={}, Items={} for user: {}",
                order.getId(), order.getTotalAmount(), order.getOrderItems().size(), user.getUsername());

        // Wyczyść koszyk
        cartService.clearCart(user);

        // Wyślij email z potwierdzeniem
        try {
            emailService.sendOrderConfirmation(order);
        } catch (Exception e) {
            logger.warn("Failed to send order confirmation email for order: {}", order.getId(), e);
        }

        return order;
    }

    public List<Order> getUserOrders(User user) {
        if (user == null) {
            logger.warn("Attempted to get orders for null user");
            return List.of();
        }

        logger.debug("Getting orders for user: {} (ID: {})", user.getUsername(), user.getId());

        try {
            List<Order> orders = orderDao.findByUser(user);
            logger.info("Found {} orders for user: {}", orders.size(), user.getUsername());

            // Log szczegółów dla debugowania
            for (Order order : orders) {
                logger.debug("Order found: ID={}, Date={}, Status={}, Total={}, ItemsCount={}",
                        order.getId(), order.getOrderDate(), order.getStatus(),
                        order.getTotalAmount(), order.getOrderItems().size());
            }

            return orders;
        } catch (Exception e) {
            logger.error("Error getting orders for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to retrieve orders for user: " + user.getUsername(), e);
        }
    }

    public List<Order> getAllOrders() {
        logger.debug("Getting all orders");
        try {
            List<Order> orders = orderDao.findAll();
            logger.info("Found {} total orders", orders.size());
            return orders;
        } catch (Exception e) {
            logger.error("Error getting all orders", e);
            throw new RuntimeException("Failed to retrieve all orders", e);
        }
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status == null) {
            logger.warn("Attempted to get orders with null status");
            return List.of();
        }

        logger.debug("Getting orders by status: {}", status);
        try {
            List<Order> orders = orderDao.findByStatus(status);
            logger.info("Found {} orders with status: {}", orders.size(), status);
            return orders;
        } catch (Exception e) {
            logger.error("Error getting orders by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve orders by status: " + status, e);
        }
    }

    public Optional<Order> getOrderById(Long orderId) {
        if (orderId == null) {
            logger.warn("Attempted to get order with null ID");
            return Optional.empty();
        }

        logger.debug("Getting order by ID: {}", orderId);
        try {
            Optional<Order> order = orderDao.findById(orderId);
            if (order.isPresent()) {
                logger.debug("Found order: ID={}", orderId);
            } else {
                logger.debug("Order not found: ID={}", orderId);
            }
            return order;
        } catch (Exception e) {
            logger.error("Error getting order by ID: {}", orderId, e);
            return Optional.empty();
        }
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new Exception("Tylko administrator może zmieniać status zamówienia");
        }

        if (orderId == null || newStatus == null) {
            throw new Exception("ID zamówienia i nowy status są wymagane");
        }

        logger.debug("Updating order {} status to {}", orderId, newStatus);

        Optional<Order> orderOpt = orderDao.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new Exception("Zamówienie nie zostało znalezione");
        }

        Order order = orderOpt.get();
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderDao.update(order);
        orderDao.flush(); // Wymuś zapis

        logger.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        // Wyślij email o zmianie statusu
        try {
            emailService.sendOrderStatusUpdate(order, oldStatus, newStatus);
        } catch (Exception e) {
            logger.warn("Failed to send order status update email for order: {}", orderId, e);
        }
    }

    @Transactional
    public void cancelOrder(Long orderId) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany");
        }

        if (orderId == null) {
            throw new Exception("ID zamówienia jest wymagane");
        }

        Optional<Order> orderOpt = orderDao.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new Exception("Zamówienie nie zostało znalezione");
        }

        Order order = orderOpt.get();

        // Sprawdź uprawnienia
        if (currentUser.getRole() != Role.ADMIN && !order.getUser().getId().equals(currentUser.getId())) {
            throw new Exception("Nie masz uprawnień do anulowania tego zamówienia");
        }

        // Sprawdź czy można anulować
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new Exception("Nie można anulować zamówienia, które zostało już wysłane");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new Exception("Zamówienie już zostało anulowane");
        }

        logger.debug("Cancelling order: {}", orderId);

        // Przywróć stan magazynowy
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productDao.update(product);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderDao.update(order);
        orderDao.flush(); // Wymuś zapis

        logger.info("Order {} cancelled by user: {}", orderId, currentUser.getUsername());

        // Wyślij email o anulowaniu
        try {
            emailService.sendOrderCancellation(order);
        } catch (Exception e) {
            logger.warn("Failed to send order cancellation email for order: {}", orderId, e);
        }
    }

    public BigDecimal getTotalRevenue() {
        try {
            List<Order> completedOrders = orderDao.findByStatus(OrderStatus.DELIVERED);
            BigDecimal total = completedOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            logger.debug("Total revenue calculated: {}", total);
            return total;
        } catch (Exception e) {
            logger.error("Error calculating total revenue", e);
            return BigDecimal.ZERO;
        }
    }

    public long getPendingOrdersCount() {
        try {
            long count = orderDao.findByStatus(OrderStatus.PENDING).size();
            logger.debug("Pending orders count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting pending orders count", e);
            return 0;
        }
    }

    // NOWE METODY dla debugowania i administracji

    /**
     * Pobiera statystyki zamówień
     */
    public OrderStatistics getOrderStatistics() {
        try {
            List<Order> allOrders = getAllOrders();

            long totalOrders = allOrders.size();
            long pendingOrders = allOrders.stream().mapToLong(o -> o.getStatus() == OrderStatus.PENDING ? 1 : 0).sum();
            long completedOrders = allOrders.stream().mapToLong(o -> o.getStatus() == OrderStatus.DELIVERED ? 1 : 0).sum();
            BigDecimal totalRevenue = getTotalRevenue();

            return new OrderStatistics(totalOrders, pendingOrders, completedOrders, totalRevenue);
        } catch (Exception e) {
            logger.error("Error getting order statistics", e);
            return new OrderStatistics(0, 0, 0, BigDecimal.ZERO);
        }
    }

    /**
     * Klasa pomocnicza dla statystyk
     */
    public static class OrderStatistics {
        private final long totalOrders;
        private final long pendingOrders;
        private final long completedOrders;
        private final BigDecimal totalRevenue;

        public OrderStatistics(long totalOrders, long pendingOrders, long completedOrders, BigDecimal totalRevenue) {
            this.totalOrders = totalOrders;
            this.pendingOrders = pendingOrders;
            this.completedOrders = completedOrders;
            this.totalRevenue = totalRevenue;
        }

        public long getTotalOrders() { return totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public long getCompletedOrders() { return completedOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
    }

    /**
     * Sprawdza integralność danych zamówień
     */
    public void validateOrdersIntegrity() {
        logger.info("Starting orders integrity validation");

        try {
            List<Order> allOrders = getAllOrders();

            for (Order order : allOrders) {
                // Sprawdź czy zamówienie ma pozycje
                if (order.getOrderItems().isEmpty()) {
                    logger.warn("Order {} has no items", order.getId());
                }

                // Sprawdź czy kwota się zgadza
                BigDecimal calculatedTotal = order.getOrderItems().stream()
                        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (order.getTotalAmount().compareTo(calculatedTotal) != 0) {
                    logger.warn("Order {} total amount mismatch: stored={}, calculated={}",
                            order.getId(), order.getTotalAmount(), calculatedTotal);
                }
            }

            logger.info("Orders integrity validation completed");
        } catch (Exception e) {
            logger.error("Error during orders integrity validation", e);
        }
    }
}