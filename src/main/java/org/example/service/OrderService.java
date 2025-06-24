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
        order.setStatus(OrderStatus.CONFIRMED); // Proste - zamówienie od razu potwierdzone

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

        // Wyczyść koszyk
        cartService.clearCart(user);

        logger.info("Order created successfully: {} for user: {}", order.getId(), user.getUsername());

        // Wyślij email z potwierdzeniem
        try {
            emailService.sendOrderConfirmation(order);
        } catch (Exception e) {
            logger.warn("Failed to send order confirmation email for order: {}", order.getId(), e);
        }

        return order;
    }

    public List<Order> getUserOrders(User user) {
        logger.debug("Getting orders for user: {}", user.getUsername());
        return orderDao.findByUser(user);
    }

    public List<Order> getAllOrders() {
        logger.debug("Getting all orders");
        return orderDao.findAll();
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderDao.findById(orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany");
        }

        Optional<Order> orderOpt = orderDao.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new Exception("Zamówienie nie zostało znalezione");
        }

        Order order = orderOpt.get();

        // Sprawdź uprawnienia - tylko właściciel zamówienia może anulować
        if (!order.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new Exception("Nie masz uprawnień do anulowania tego zamówienia");
        }

        // Sprawdź czy można anulować
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

        order.setStatus(OrderStatus.CANCELLED);
        orderDao.update(order);

        logger.info("Order {} cancelled by user: {}", orderId, currentUser.getUsername());
    }

    // Uproszczone statystyki
    public BigDecimal getTotalRevenue() {
        return orderDao.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalOrdersCount() {
        return orderDao.findAll().size();
    }

    public long getCompletedOrdersCount() {
        return orderDao.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .count();
    }
}