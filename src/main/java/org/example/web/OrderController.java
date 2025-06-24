package org.example.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
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
import org.example.service.CartServiceInterface;
import org.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped  // Zmienione z @SessionScoped na @RequestScoped
public class OrderController implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Inject
    private OrderService orderService;

    @Inject
    private CartServiceInterface cartService;

    @Inject
    private AuthServiceInterface authService;

    @Inject
    private CartController cartController;

    private String shippingAddress;
    private List<Order> userOrders = new ArrayList<>();
    private List<Order> allOrders = new ArrayList<>();
    private Order selectedOrder;
    private OrderStatus selectedStatus;

    @PostConstruct
    public void init() {
        logger.debug("OrderController initialized - loading orders");
        loadOrders();
    }

    public void loadOrders() {
        User currentUser = authService.getCurrentUser();
        logger.debug("Loading orders for user: {}", currentUser != null ? currentUser.getUsername() : "null");

        if (currentUser != null) {
            try {
                // Zawsze pobierz świeże dane z bazy
                userOrders = orderService.getUserOrders(currentUser);
                logger.info("Loaded {} orders for user: {}", userOrders.size(), currentUser.getUsername());

                // Log szczegółów zamówień dla debugowania
                for (Order order : userOrders) {
                    logger.debug("Order: ID={}, Date={}, Status={}, Total={}",
                            order.getId(), order.getOrderDate(), order.getStatus(), order.getTotalAmount());
                }

                // Załaduj wszystkie zamówienia dla admina
                if (currentUser.getRole() == Role.ADMIN) {
                    allOrders = orderService.getAllOrders();
                    logger.debug("Loaded {} total orders for admin", allOrders.size());
                }

            } catch (Exception e) {
                logger.error("Error loading orders for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas ładowania zamówień: " + e.getMessage());
                userOrders = new ArrayList<>();
                allOrders = new ArrayList<>();
            }
        } else {
            logger.debug("No current user - clearing orders lists");
            userOrders = new ArrayList<>();
            allOrders = new ArrayList<>();
        }
    }

    public String placeOrder() {
        if (!authService.isLoggedIn()) {
            addErrorMessage("Musisz być zalogowany, aby złożyć zamówienie");
            return "/login.xhtml?faces-redirect=true";
        }

        User currentUser = authService.getCurrentUser();

        // Walidacja adresu dostawy
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            addErrorMessage("Adres dostawy jest wymagany");
            return null;
        }

        if (shippingAddress.trim().length() < 10) {
            addErrorMessage("Adres dostawy musi być bardziej szczegółowy (minimum 10 znaków)");
            return null;
        }

        // Sprawdź koszyk przed złożeniem zamówienia
        if (cartService.isCartEmpty(currentUser)) {
            addErrorMessage("Koszyk jest pusty");
            return "/user/cart.xhtml?faces-redirect=true";
        }

        // Dodatkowe sprawdzenie dostępności produktów
        if (cartService instanceof CartService) {
            CartService cs = (CartService) cartService;

            // Synchronizuj koszyk przed złożeniem zamówienia
            try {
                cs.synchronizeCartWithStock(currentUser);
            } catch (Exception e) {
                logger.error("Error synchronizing cart before order placement", e);
                addErrorMessage("Błąd podczas synchronizacji koszyka");
                return null;
            }

            // Sprawdź czy nadal można złożyć zamówienie
            if (!cs.canPlaceOrder(currentUser)) {
                addErrorMessage("Nie można złożyć zamówienia. Niektóre produkty są niedostępne.");
                return "/user/cart.xhtml?faces-redirect=true";
            }

            // Sprawdź czy są niedostępne produkty
            List<org.example.model.CartItem> unavailableItems = cs.getUnavailableCartItems(currentUser);
            if (!unavailableItems.isEmpty()) {
                StringBuilder message = new StringBuilder("Następujące produkty są niedostępne: ");
                for (org.example.model.CartItem item : unavailableItems) {
                    message.append(item.getProduct().getName()).append(", ");
                }
                // Usuń ostatni przecinek
                if (message.length() > 2) {
                    message.setLength(message.length() - 2);
                }
                addErrorMessage(message.toString());
                return "/user/cart.xhtml?faces-redirect=true";
            }
        }

        try {
            // Utwórz zamówienie
            Order order = orderService.createOrderFromCart(currentUser, shippingAddress.trim());

            // WAŻNE: Wyczyść koszyk w CartController po pomyślnym złożeniu zamówienia
            cartController.clearCartSilently();

            // Wyczyść adres dostawy po pomyślnym złożeniu zamówienia
            shippingAddress = null;

            // WAŻNE: Odśwież listę zamówień po dodaniu nowego
            loadOrders();

            // Wyczyść zapisany adres z localStorage
            FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts().add("clearSavedAddress();");

            // Przekieruj z komunikatem sukcesu
            addInfoMessage(String.format("Zamówienie zostało złożone pomyślnie! Numer zamówienia: #%d", order.getId()));
            logger.info("Order {} placed successfully by user: {}", order.getId(), currentUser.getUsername());

            return "/user/orders.xhtml?faces-redirect=true&orderPlaced=true";

        } catch (Exception e) {
            logger.error("Error placing order for user: {}", currentUser.getUsername(), e);
            addErrorMessage("Błąd podczas składania zamówienia: " + e.getMessage());

            // W przypadku błędu, sprawdź czy koszyk nie został przypadkowo wyczyszczony
            if (cartService.isCartEmpty(currentUser)) {
                return "/user/products.xhtml?faces-redirect=true";
            }

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

    // Metody pomocnicze dla checkout

    public boolean isCheckoutValid() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Sprawdź czy koszyk nie jest pusty
        if (cartService.isCartEmpty(currentUser)) {
            return false;
        }

        // Sprawdź adres dostawy
        if (shippingAddress == null || shippingAddress.trim().length() < 10) {
            return false;
        }

        // Sprawdź dostępność produktów
        if (cartService instanceof CartService) {
            return ((CartService) cartService).canPlaceOrder(currentUser);
        }

        return true;
    }

    public void validateCheckout() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            addErrorMessage("Musisz być zalogowany");
            return;
        }

        if (cartService.isCartEmpty(currentUser)) {
            addErrorMessage("Koszyk jest pusty");
            return;
        }

        if (shippingAddress == null || shippingAddress.trim().length() < 10) {
            return; // Nie dodawaj komunikatu - walidacja real-time w JS
        }

        // Synchronizuj koszyk i sprawdź dostępność
        if (cartService instanceof CartService) {
            CartService cs = (CartService) cartService;
            try {
                cs.synchronizeCartWithStock(currentUser);

                if (!cs.canPlaceOrder(currentUser)) {
                    addErrorMessage("Niektóre produkty w koszyku są niedostępne. Sprawdź koszyk.");
                    return;
                }
            } catch (Exception e) {
                logger.error("Error validating checkout", e);
                addErrorMessage("Błąd podczas walidacji zamówienia");
            }
        }

        // Jeśli dotrzemy tutaj, wszystko jest OK - nie dodajemy komunikatu sukcesu
    }

    public void prepopulateShippingAddress() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getAddress() != null && !currentUser.getAddress().trim().isEmpty()) {
            this.shippingAddress = currentUser.getAddress();
            addInfoMessage("Uzupełniono adres dostawy z Twojego profilu");
        }
    }

    // Gettery i settery
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<Order> getUserOrders() {
        // Zawsze odśwież dane przed zwróceniem
        if (userOrders == null || userOrders.isEmpty()) {
            loadOrders();
        }
        logger.debug("Returning {} user orders", userOrders != null ? userOrders.size() : 0);
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

    // NOWA METODA - getter dla checkoutValid
    public boolean getCheckoutValid() {
        return isCheckoutValid();
    }

    // NOWA METODA - wymusza odświeżenie zamówień
    public void refreshOrders() {
        logger.info("Manually refreshing orders list");
        loadOrders();
        addInfoMessage("Lista zamówień została odświeżona");
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