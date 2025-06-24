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
import org.example.service.CartServiceInterface;
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
    private CartServiceInterface cartService;

    @Inject
    private AuthServiceInterface authService;

    @Inject
    private CartController cartController;

    private String shippingAddress;
    private List<Order> userOrders = new ArrayList<>();

    @PostConstruct
    public void init() {
        logger.debug("OrderController initialized");
        loadOrders();
    }

    public void loadOrders() {
        User currentUser = authService.getCurrentUser();
        logger.debug("Loading orders for user: {}", currentUser != null ? currentUser.getUsername() : "null");

        if (currentUser != null) {
            try {
                userOrders = orderService.getUserOrders(currentUser);
                logger.info("Loaded {} orders for user: {}", userOrders.size(), currentUser.getUsername());

                // Debug: sprawdź czy zamówienia mają pozycje
                for (Order order : userOrders) {
                    logger.debug("Order {}: {} items, total: {}",
                            order.getId(),
                            order.getOrderItems() != null ? order.getOrderItems().size() : "null",
                            order.getTotalAmount());
                }

            } catch (Exception e) {
                logger.error("Error loading orders for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas ładowania zamówień: " + e.getMessage());
                userOrders = new ArrayList<>();
            }
        } else {
            logger.debug("No current user, clearing orders");
            userOrders = new ArrayList<>();
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
        }

        try {
            // Utwórz zamówienie
            Order order = orderService.createOrderFromCart(currentUser, shippingAddress.trim());

            // Wyczyść koszyk w CartController po pomyślnym złożeniu zamówienia
            cartController.clearCartSilently();

            // Wyczyść adres dostawy po pomyślnym złożeniu zamówienia
            shippingAddress = null;

            // Odśwież listę zamówień
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

    public boolean canCancelOrder(Order order) {
        if (order == null) return false;

        User currentUser = authService.getCurrentUser();
        if (currentUser == null) return false;

        // Admin może anulować każde zamówienie (poza już anulowanymi)
        if (currentUser.getRole() == Role.ADMIN) {
            return order.getStatus() != OrderStatus.CANCELLED;
        }

        // Użytkownik może anulować tylko swoje zamówienia
        if (order.getUser().getId().equals(currentUser.getId())) {
            return order.getStatus() != OrderStatus.CANCELLED;
        }

        return false;
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
    }

    public void prepopulateShippingAddress() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getAddress() != null && !currentUser.getAddress().trim().isEmpty()) {
            this.shippingAddress = currentUser.getAddress();
            addInfoMessage("Uzupełniono adres dostawy z Twojego profilu");
        }
    }

    // Pomocnicze metody do wyświetlania
    public String getOrderStatusIcon(OrderStatus status) {
        if (status == null) return "❓";

        return switch (status) {
            case PENDING -> "⏳";
            case CONFIRMED -> "✅";
            case SHIPPED -> "🚛";
            case DELIVERED -> "📦";
            case CANCELLED -> "❌";
        };
    }

    public String getOrderStatusText(OrderStatus status) {
        if (status == null) return "Nieznany";
        return status.getDisplayName();
    }

    public String getOrderStatusClass(OrderStatus status) {
        if (status == null) return "status-unknown";
        return "status-" + status.name().toLowerCase();
    }

    // Metody debugowania
    public String getOrdersDebugInfo() {
        return String.format("Orders loaded: %d, Current user: %s",
                userOrders != null ? userOrders.size() : 0,
                authService.getCurrentUser() != null ? authService.getCurrentUser().getUsername() : "null");
    }

    public void forceReloadOrders() {
        logger.info("Force reloading orders...");
        loadOrders();
        addInfoMessage("Zamówienia zostały ponownie załadowane. Znaleziono: " + userOrders.size());
    }

    // Gettery i settery
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<Order> getUserOrders() {
        logger.debug("getUserOrders() called, returning {} orders", userOrders != null ? userOrders.size() : 0);

        // Jeśli lista jest pusta, spróbuj załadować ponownie
        if ((userOrders == null || userOrders.isEmpty()) && authService.getCurrentUser() != null) {
            logger.debug("Orders list is empty, attempting to reload...");
            loadOrders();
        }

        return userOrders != null ? userOrders : new ArrayList<>();
    }

    public void setUserOrders(List<Order> userOrders) {
        this.userOrders = userOrders;
    }

    public boolean getCheckoutValid() {
        return isCheckoutValid();
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