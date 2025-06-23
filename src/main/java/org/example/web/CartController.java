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
import org.example.service.CartService;
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
        loadCart();
    }

    public void loadCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                cartItems = cartService.getCartItems(currentUser);
                cartLoaded = true;

                // Sprawdź czy nie ma problemów z dostępnością produktów
                checkCartAvailability();

                logger.debug("Cart loaded for user: {} with {} items", currentUser.getUsername(), cartItems.size());
            } catch (Exception e) {
                logger.error("Error loading cart for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas ładowania koszyka: " + e.getMessage());
                cartItems = new ArrayList<>();
            }
        } else {
            cartItems = new ArrayList<>();
            cartLoaded = false;
        }
    }

    public String addToCart(Long productId) {
        return addToCart(productId, 1);
    }

    public String addToCart(Long productId, Integer quantity) {
        if (!authService.isLoggedIn()) {
            addErrorMessage("Musisz być zalogowany, aby dodać produkt do koszyka");
            return "/login.xhtml?faces-redirect=true";
        }

        if (quantity == null || quantity <= 0) {
            addErrorMessage("Nieprawidłowa ilość produktu");
            return null;
        }

        try {
            cartService.addToCart(productId, quantity);
            loadCart(); // Odśwież koszyk

            String message = quantity == 1 ?
                    "Produkt został dodany do koszyka" :
                    String.format("Dodano %d sztuk produktu do koszyka", quantity);
            addInfoMessage(message);

            logger.info("Product {} added to cart for user: {} with quantity: {}",
                    productId, authService.getCurrentUser().getUsername(), quantity);

        } catch (Exception e) {
            logger.error("Error adding product {} to cart with quantity {}", productId, quantity, e);
            addErrorMessage("Błąd podczas dodawania produktu: " + e.getMessage());
        }

        return null; // Pozostań na tej samej stronie
    }

    public void updateQuantity(Long cartItemId, Integer newQuantity) {
        if (newQuantity == null || newQuantity < 0) {
            addErrorMessage("Nieprawidłowa ilość");
            return;
        }

        try {
            if (newQuantity == 0) {
                removeFromCart(cartItemId);
                return;
            }

            cartService.updateCartItemQuantity(cartItemId, newQuantity);
            loadCart(); // Odśwież koszyk
            addInfoMessage("Ilość została zaktualizowana");
            logger.info("Cart item {} quantity updated to {}", cartItemId, newQuantity);

        } catch (Exception e) {
            logger.error("Error updating cart item {} quantity to {}", cartItemId, newQuantity, e);
            addErrorMessage("Błąd podczas aktualizacji ilości: " + e.getMessage());
            loadCart(); // Odśwież koszyk aby pokazać aktualny stan
        }
    }

    public void removeFromCart(Long cartItemId) {
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

    public void synchronizeCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null && cartService instanceof CartService) {
            try {
                ((CartService) cartService).synchronizeCartWithStock(currentUser);
                loadCart(); // Odśwież koszyk
                addInfoMessage("Koszyk został zsynchronizowany z dostępnością produktów");
                logger.info("Cart synchronized for user: {}", currentUser.getUsername());

            } catch (Exception e) {
                logger.error("Error synchronizing cart for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas synchronizacji koszyka: " + e.getMessage());
            }
        }
    }

    private void checkCartAvailability() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null && cartService instanceof CartService) {
            try {
                List<CartItem> unavailableItems = ((CartService) cartService).getUnavailableCartItems(currentUser);

                if (!unavailableItems.isEmpty()) {
                    StringBuilder message = new StringBuilder("Uwaga! Niektóre produkty w koszyku nie są już dostępne: ");
                    for (CartItem item : unavailableItems) {
                        message.append(item.getProduct().getName()).append(", ");
                    }
                    // Usuń ostatni przecinek i spację
                    if (message.length() > 2) {
                        message.setLength(message.length() - 2);
                    }

                    addWarningMessage(message.toString());
                    logger.warn("User {} has {} unavailable items in cart",
                            currentUser.getUsername(), unavailableItems.size());
                }

            } catch (Exception e) {
                logger.error("Error checking cart availability", e);
            }
        }
    }

    public BigDecimal getCartTotal() {
        return cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getCartItemCount() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public int getCartItemsCount() {
        return cartItems.size();
    }

    public boolean isCartEmpty() {
        return cartItems.isEmpty();
    }

    public boolean canPlaceOrder() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null || isCartEmpty()) {
            return false;
        }

        if (cartService instanceof CartService) {
            return ((CartService) cartService).canPlaceOrder(currentUser);
        }

        // Fallback check
        return cartItems.stream().allMatch(item ->
                item.getProduct().isActive() &&
                        item.getProduct().isAvailable(item.getQuantity())
        );
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

        if (!canPlaceOrder()) {
            addErrorMessage("Niektóre produkty w koszyku nie są już dostępne. Zsynchronizuj koszyk.");
            return null;
        }

        return "/user/checkout.xhtml?faces-redirect=true";
    }

    // Metody pomocnicze dla UI
    public String getStockStatus(CartItem item) {
        if (!item.getProduct().isActive()) {
            return "Produkt niedostępny";
        }

        Integer stock = item.getProduct().getStockQuantity();
        if (stock == null || stock == 0) {
            return "Wyprzedany";
        } else if (stock < item.getQuantity()) {
            return "Niewystarczająca ilość (dostępne: " + stock + ")";
        } else if (stock <= 5) {
            return "Ostatnie sztuki";
        } else {
            return "Dostępny";
        }
    }

    public String getStockStatusClass(CartItem item) {
        if (!item.getProduct().isActive() ||
                item.getProduct().getStockQuantity() == null ||
                item.getProduct().getStockQuantity() == 0) {
            return "stock-out";
        } else if (item.getProduct().getStockQuantity() < item.getQuantity()) {
            return "stock-insufficient";
        } else if (item.getProduct().getStockQuantity() <= 5) {
            return "stock-low";
        } else {
            return "stock-ok";
        }
    }

    public boolean isItemAvailable(CartItem item) {
        return item.getProduct().isActive() &&
                item.getProduct().isAvailable(item.getQuantity());
    }

    public int getMaxQuantityForItem(CartItem item) {
        if (!item.getProduct().isActive()) {
            return 0;
        }

        Integer stock = item.getProduct().getStockQuantity();
        return stock != null ? Math.min(stock, 100) : 0; // Max 100 sztuk
    }

    // Gettery i settery
    public List<CartItem> getCartItems() {
        if (!cartLoaded) {
            loadCart();
        }
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public boolean isCartLoaded() {
        return cartLoaded;
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", message));
    }

    private void addWarningMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Uwaga", message));
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Błąd", message));
    }
}