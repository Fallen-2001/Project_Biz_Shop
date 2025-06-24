package org.example.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.CartItem;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.example.service.CartServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class CartController implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Inject
    private CartServiceInterface cartService;

    @Inject
    private AuthServiceInterface authService;

    private List<CartItem> cartItems = new ArrayList<>();
    private boolean cartLoaded = false;

    @PostConstruct
    public void init() {
        logger.debug("CartController initialized");
        loadCart();
    }

    public void loadCart() {
        User currentUser = authService.getCurrentUser();
        logger.debug("Loading cart for user: {}", currentUser != null ? currentUser.getUsername() : "null");

        if (currentUser != null) {
            try {
                List<CartItem> freshCartItems = cartService.getCartItems(currentUser);
                this.cartItems = freshCartItems;
                this.cartLoaded = true;

                logger.debug("Cart loaded successfully: {} items for user: {}",
                        cartItems.size(), currentUser.getUsername());

                // Log szczegóły każdego elementu
                for (int i = 0; i < cartItems.size(); i++) {
                    CartItem item = cartItems.get(i);
                    logger.debug("Cart item {}: {} (ID: {}, Product ID: {})",
                            i, item.getProduct().getName(), item.getId(), item.getProduct().getId());
                }

            } catch (Exception e) {
                logger.error("Error loading cart for user: {}", currentUser.getUsername(), e);
                this.cartItems = new ArrayList<>();
                this.cartLoaded = false;
                addErrorMessage("Błąd podczas ładowania koszyka");
            }
        } else {
            logger.debug("No current user, setting empty cart");
            this.cartItems = new ArrayList<>();
            this.cartLoaded = false;
        }
    }

    public String addToCart(Long productId) {
        logger.debug("Adding product {} to cart", productId);

        if (!authService.isLoggedIn()) {
            addErrorMessage("Musisz być zalogowany, aby dodać produkt do koszyka");
            return "/login.xhtml?faces-redirect=true";
        }

        try {
            cartService.addToCart(productId, 1); // Quantity jest ignorowane w uproszczonym systemie
            loadCart(); // Odśwież koszyk
            addInfoMessage("Produkt został dodany do koszyka");

            logger.info("Product {} added to cart for user: {}",
                    productId, authService.getCurrentUser().getUsername());

        } catch (Exception e) {
            logger.error("Error adding product {} to cart", productId, e);
            addErrorMessage("Błąd podczas dodawania produktu: " + e.getMessage());
        }

        return null; // Pozostań na tej samej stronie
    }

    public void removeFromCart(Long cartItemId) {
        logger.debug("Removing cart item {}", cartItemId);

        try {
            cartService.removeFromCart(cartItemId);
            loadCart(); // Odśwież koszyk
            addInfoMessage("Produkt został usunięty z koszyka");
            logger.info("Cart item {} removed from cart", cartItemId);

        } catch (Exception e) {
            logger.error("Error removing cart item {}", cartItemId, e);
            addErrorMessage("Błąd podczas usuwania produktu z koszyka: " + e.getMessage());
        }
    }

    public void clearCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                cartService.clearCart(currentUser);
                loadCart(); // Odśwież koszyk
                addInfoMessage("Koszyk został wyczyszczony");
                logger.info("Cart cleared for user: {}", currentUser.getUsername());

            } catch (Exception e) {
                logger.error("Error clearing cart for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas czyszczenia koszyka: " + e.getMessage());
            }
        }
    }

    public void forceRefreshCart() {
        logger.debug("Force refreshing cart");
        this.cartLoaded = false;
        this.cartItems.clear();
        loadCart();
        addInfoMessage("Koszyk został odświeżony");
    }

    public BigDecimal getCartTotal() {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cartItems.stream()
                .map(item -> item.getProduct().getPrice()) // Każdy produkt to 1 sztuka
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getCartItemCount() {
        return cartItems != null ? cartItems.size() : 0; // Liczba różnych produktów
    }

    public int getCartItemsCount() {
        return cartItems != null ? cartItems.size() : 0; // To samo co wyżej
    }

    public boolean isCartEmpty() {
        return cartItems == null || cartItems.isEmpty();
    }

    public String goToCheckout() {
        if (!authService.isLoggedIn()) {
            addErrorMessage("Musisz być zalogowany, aby przejść do finalizacji zamówienia");
            return "/login.xhtml?faces-redirect=true";
        }

        if (isCartEmpty()) {
            addErrorMessage("Koszyk jest pusty");
            return null;
        }

        return "/user/checkout.xhtml?faces-redirect=true";
    }

    // Gettery i settery
    public List<CartItem> getCartItems() {
        logger.debug("getCartItems() called, cartLoaded: {}, cartItems.size(): {}",
                cartLoaded, cartItems != null ? cartItems.size() : "null");

        if (!cartLoaded) {
            logger.debug("Cart not loaded, calling loadCart()");
            loadCart();
        }

        if (cartItems == null) {
            logger.warn("cartItems is null after loadCart(), returning empty list");
            return new ArrayList<>();
        }

        logger.debug("getCartItems() returning {} items", cartItems.size());
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public boolean isCartLoaded() {
        return cartLoaded;
    }

    // Debug metody
    public String getCartDebugInfo() {
        return String.format("Loaded: %s, Size: %d, Empty: %s",
                cartLoaded, cartItems != null ? cartItems.size() : 0, isCartEmpty());
    }

    // Metoda do sprawdzenia czy lista nie jest null
    public List<CartItem> getCartItemsSafe() {
        if (cartItems == null) {
            logger.warn("cartItems is null, initializing empty list");
            cartItems = new ArrayList<>();
        }
        logger.debug("getCartItemsSafe() returning {} items", cartItems.size());
        return cartItems;
    }

    // Metoda do debugowania pierwszego elementu
    public String getFirstItemDebug() {
        if (cartItems != null && !cartItems.isEmpty()) {
            try {
                CartItem first = cartItems.get(0);
                return String.format("First item: %s (ID: %d, Product: %s)",
                        first.getProduct().getName(), first.getId(), first.getProduct().getId());
            } catch (Exception e) {
                return "Error accessing first item: " + e.getMessage();
            }
        }
        return "No items in cart";
    }

    // Metoda do sprawdzenia czy getCartItems() działa
    public String getTestGetCartItems() {
        try {
            List<CartItem> items = getCartItems();
            return String.format("getCartItems() returned %d items", items != null ? items.size() : 0);
        } catch (Exception e) {
            return "Error in getCartItems(): " + e.getMessage();
        }
    }

    // Metoda do sprawdzenia czy cartService działa
    public String getServiceDebugInfo() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return "No current user";
        }

        try {
            List<CartItem> serviceItems = cartService.getCartItems(currentUser);
            return String.format("CartService returned %d items for user %s",
                    serviceItems.size(), currentUser.getUsername());
        } catch (Exception e) {
            return "Error from CartService: " + e.getMessage();
        }
    }

    // Metoda do manualnego załadowania z serwisu
    public void loadCartFromService() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                List<CartItem> serviceItems = cartService.getCartItems(currentUser);
                this.cartItems = serviceItems;
                this.cartLoaded = true;

                addInfoMessage(String.format("Załadowano %d produktów z serwisu", serviceItems.size()));
                logger.info("Manually loaded {} items from service for user: {}",
                        serviceItems.size(), currentUser.getUsername());

            } catch (Exception e) {
                logger.error("Error manually loading from service", e);
                addErrorMessage("Błąd podczas ładowania z serwisu: " + e.getMessage());
            }
        }
    }

    // Metoda do sprawdzenia czy session działa
    public String getSessionDebugInfo() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            return String.format("Session ID: %s, New: %s",
                    context.getExternalContext().getSessionId(false),
                    context.getExternalContext().getSession(false) != null);
        } catch (Exception e) {
            return "Session error: " + e.getMessage();
        }
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", message));
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Błąd", message));
    }

    public boolean isCanPlaceOrder() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        try {
            return cartService.canPlaceOrder(currentUser);
        } catch (Exception e) {
            logger.error("Error checking if can place order", e);
            return false;
        }
    }

    /**
     * Alias dla JSF EL (canPlaceOrder zamiast isCanPlaceOrder)
     */
    public boolean getCanPlaceOrder() {
        return isCanPlaceOrder();
    }

    /**
     * Synchronizuje koszyk z dostępnością produktów
     */
    public void synchronizeCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                cartService.synchronizeCartWithStock(currentUser);
                loadCart(); // Odśwież koszyk po synchronizacji
                addInfoMessage("Koszyk został zsynchronizowany");

            } catch (Exception e) {
                logger.error("Error synchronizing cart", e);
                addErrorMessage("Błąd podczas synchronizacji koszyka: " + e.getMessage());
            }
        }
    }

    /**
     * Sprawdza dostępność produktów w koszyku
     */
    public boolean hasUnavailableProducts() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        try {
            return cartService.hasUnavailableProducts(currentUser);
        } catch (Exception e) {
            logger.error("Error checking unavailable products", e);
            return false;
        }
    }

    /**
     * Pobiera liczbę niedostępnych produktów
     */
    public int getUnavailableProductsCount() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }

        try {
            return cartService.getUnavailableProductsCount(currentUser);
        } catch (Exception e) {
            logger.error("Error getting unavailable products count", e);
            return 0;
        }
    }
}