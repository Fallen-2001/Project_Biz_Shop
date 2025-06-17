package org.example.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class OrderController implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Inject
    private OrderService orderService;

    @Inject
    private CartService cartService;

    @Inject
    private AuthServiceInterface authService;

    private String shippingAddress;
    private List<Order> userOrders = new ArrayList<>();
    private List<Order> allOrders = new ArrayList<>();
    private Order selectedOrder;
    private OrderStatus selectedStatus;

    @PostConstruct
    public void init() {
        loadOrders();
    }

    public void loadOrders() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                userOrders = orderService.getUserOrders(currentUser);

                // Załaduj wszystkie zamówienia dla admina
                if (currentUser.getRole() == Role.ADMIN) {
                    allOrders = orderService.getAllOrders();
                }

                logger.debug("Orders loaded for user: {}", currentUser.getUsername());
            } catch (Exception e) {
                logger.error("Error loading orders for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas ładowania zamówień");
            }
        }
    }

    public String placeOrder() {
        if (!authService.isLoggedIn()) {
            addErrorMessage("Musisz być zalogowany, aby złożyć zamówienie");
            return "/login.xhtml?faces-redirect=true";
        }

        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            addErrorMessage("Adres dostawy jest wymagany");
            return null;
        }

        if (cartService.isCartEmpty(authService.getCurrentUser())) {
            addErrorMessage("Koszyk jest pusty");
            return "/user/cart.xhtml?faces-redirect=true";
        }

        try {
            User currentUser = authService.getCurrentUser();
            Order order = orderService.createOrderFromCart(currentUser, shippingAddress.trim());

            // Wyczyść adres dostawy
            shippingAddress = null;

            // Odśwież listę zamówień
            loadOrders();

            addInfoMessage("Zamówienie zostało złożone pomyślnie! Numer zamówienia: " + order.getId());
            logger.info("Order {} placed successfully by user: {}", order.getId(), currentUser.getUsername());

            return "/user/orders.xhtml?faces-redirect=true";

        } catch (Exception e) {
            logger.error("Error placing order for user: {}", authService.getCurrentUser().getUsername(), e);
            addErrorMessage("Błąd podczas składania zamówienia: " + e.getMessage());
            return null;
        }
    }

    public void updateOrderStatus(Long orderId) {
        if (selectedStatus == null) {
            addErrorMessage("Wybierz nowy status");
            return;
        }

        try {
            orderService.updateOrderStatus(orderId, selectedStatus);
            loadOrders(); // Odśwież listę zamówień
            addInfoMessage("Status zamówienia został zaktualizowany");
            logger.info("Order {} status updated to {}", orderId, selectedStatus);

            // Reset selected status
            selectedStatus = null;

        } catch (Exception e) {
            logger.error("Error updating order {} status", orderId, e);
            addErrorMessage("Błąd podczas aktualizacji statusu: " + e.getMessage());
        }
    }

    public void cancelOrder(Long orderId) {
        try {
            orderService.cancelOrder(orderId);
            loadOrders(); // Odśwież listę zamówień
            addInfoMessage("Zamówienie zostało anulowane");
            logger.info("Order {} cancelled", orderId);

        } catch (Exception e) {
            logger.error("Error cancelling order {}", orderId, e);
            addErrorMessage("Błąd podczas anulowania zamówienia: " + e.getMessage());
        }
    }

    public void selectOrder(Order order) {
        this.selectedOrder = order;
        logger.debug("Selected order: {}", order.getId());
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (authService.getCurrentUser() != null && authService.getCurrentUser().getRole() == Role.ADMIN) {
            return orderService.getOrdersByStatus(status);
        }
        return new ArrayList<>();
    }

    public long getPendingOrdersCount() {
        if (authService.getCurrentUser() != null && authService.getCurrentUser().getRole() == Role.ADMIN) {
            return orderService.getPendingOrdersCount();
        }
        return 0;
    }

    public boolean canCancelOrder(Order order) {
        if (order == null) return false;

        User currentUser = authService.getCurrentUser();
        if (currentUser == null) return false;

        // Admin może anulować każde zamówienie (poza już anulowanymi i dostarczonymi)
        if (currentUser.getRole() == Role.ADMIN) {
            return order.getStatus() != OrderStatus.CANCELLED &&
                    order.getStatus() != OrderStatus.DELIVERED;
        }

        // Użytkownik może anulować tylko swoje zamówienia
        if (order.getUser().getId().equals(currentUser.getId())) {
            return order.getStatus() == OrderStatus.PENDING ||
                    order.getStatus() == OrderStatus.CONFIRMED;
        }

        return false;
    }

    public boolean canUpdateStatus(Order order) {
        User currentUser = authService.getCurrentUser();
        return currentUser != null &&
                currentUser.getRole() == Role.ADMIN &&
                order.getStatus() != OrderStatus.CANCELLED;
    }

    public OrderStatus[] getAvailableStatuses() {
        return OrderStatus.values();
    }

    public String getOrderStatusStyle(OrderStatus status) {
        return switch (status) {
            case PENDING -> "background-color: #fff3cd; color: #856404;";
            case CONFIRMED -> "background-color: #d4edda; color: #155724;";
            case SHIPPED -> "background-color: #d1ecf1; color: #0c5460;";
            case DELIVERED -> "background-color: #d1e7dd; color: #0f5132;";
            case CANCELLED -> "background-color: #f8d7da; color: #721c24;";
        };
    }

    // Gettery i settery
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<Order> getUserOrders() {
        return userOrders;
    }

    public void setUserOrders(List<Order> userOrders) {
        this.userOrders = userOrders;
    }

    public List<Order> getAllOrders() {
        return allOrders;
    }

    public void setAllOrders(List<Order> allOrders) {
        this.allOrders = allOrders;
    }

    public Order getSelectedOrder() {
        return selectedOrder;
    }

    public void setSelectedOrder(Order selectedOrder) {
        this.selectedOrder = selectedOrder;
    }

    public OrderStatus getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(OrderStatus selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", message));
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Błąd", message));
    }
}