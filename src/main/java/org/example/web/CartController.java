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
        loadCart();
    }

    public void loadCart() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                cartItems = cartService.getCartItems(currentUser);
                cartLoaded = true;
                logger.debug("Cart loaded for user: {} with {} items", currentUser.getUsername(), cartItems.size());
            } catch (Exception e) {
                logger.error("Error loading cart for user: {}", currentUser.getUsername(), e);
                addErrorMessage("Błąd podczas ładowania koszyka");
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

        try {
            cartService.addToCart(productId, quantity);
            loadCart(); // Odśwież koszyk
            addInfoMessage("Produkt został dodany do koszyka");
            logger.info("Product {} added to cart for user: {}", productId, authService.getCurrentUser().getUsername());
        } catch (Exception e) {
            logger.error("Error adding product {} to cart", productId, e);
            addErrorMessage("Błąd podczas dodawania produktu do koszyka: " + e.getMessage());
        }

        return null; // Pozostań na tej samej stronie
    }

    public void updateQuantity(Long cartItemId, Integer newQuantity) {
        try {
            cartService.updateCartItemQuantity(cartItemId, newQuantity);
            loadCart(); // Odśwież koszyk
            addInfoMessage("Ilość została zaktualizowana");
            logger.info("Cart item {} quantity updated to {}", cartItemId, newQuantity);
        } catch (Exception e) {
            logger.error("Error updating cart item {} quantity", cartItemId, e);
            addErrorMessage("Błąd podczas aktualizacji ilości: " + e.getMessage());
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
                addErrorMessage("Błąd podczas czyszczenia koszyka");
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

    public boolean isCartEmpty() {
        return cartItems.isEmpty();
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

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Błąd", message));
    }
}