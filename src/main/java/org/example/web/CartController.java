package org.example.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.CartItem;
import org.example.model.User;
import org.example.model.Product;
import org.example.service.AuthServiceInterface;
import org.example.service.CartService;
import org.example.service.CartServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        loadCartSafely();
    }

    // Bezpieczne ładowanie koszyka z obsługą błędów
    public void loadCartSafely() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            try {
                // Wymuś ponowne załadowanie z bazy danych
                List<CartItem> freshCartItems = cartService.getCartItems(currentUser);

                // Sprawdź dostępność wszystkich produktów
                for (CartItem item : freshCartItems) {
                    if (item.getProduct() != null) {
                        if (!item.getProduct().isActive() ||
                                item.getProduct().getStockQuantity() == null ||
                                item.getProduct().getStockQuantity() <= 0) {
                            logger.warn("Found unavailable product in cart: {}",
                                    item.getProduct().getName());
                        }
                    }
                }

                this.cartItems = freshCartItems;
                this.cartLoaded = true;

                // Sprawdź czy nie ma problemów z dostępnością produktów
                checkCartAvailability();

                logger.debug("Cart loaded safely for user: {} with {} items",
                        currentUser.getUsername(), cartItems.size());

            } catch (Exception e) {
                logger.error("Error loading cart safely for user: {}", currentUser.getUsername(), e);
                this.cartItems = new ArrayList<>();
                this.cartLoaded = false;
                addErrorMessage("Błąd podczas ładowania koszyka. Spróbuj ponownie.");
            }
        } else {
            this.cartItems = new ArrayList<>();
            this.cartLoaded = false;
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

    // Metoda AJAX dla aktualizacji ilości
    public void updateQuantityAjax(Long cartItemId, Integer newQuantity) {
        try {
            if (newQuantity != null && newQuantity > 0) {
                updateQuantity(cartItemId, newQuantity);
                // Komunikat zostanie dodany w updateQuantity
            }
        } catch (Exception e) {
            logger.error("AJAX error updating quantity for item {}", cartItemId, e);
            addErrorMessage("Błąd podczas aktualizacji: " + e.getMessage());
        }
    }

    public void updateQuantity(Long cartItemId, Integer newQuantity) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            addErrorMessage("Musisz być zalogowany");
            return;
        }

        logger.debug("Updating cart item {} quantity to {}", cartItemId, newQuantity);

        if (newQuantity == null || newQuantity < 0) {
            addErrorMessage("Nieprawidłowa ilość");
            return;
        }

        if (newQuantity == 0) {
            removeFromCart(cartItemId);
            return;
        }

        if (newQuantity > 100) {
            addErrorMessage("Maksymalna ilość to 100 sztuk");
            return;
        }

        try {
            // Znajdź element koszyka i sprawdź czy należy do użytkownika
            List<CartItem> userCartItems = cartService.getCartItems(currentUser);
            Optional<CartItem> cartItemOpt = userCartItems.stream()
                    .filter(item -> item.getId().equals(cartItemId))
                    .findFirst();

            if (cartItemOpt.isEmpty()) {
                addErrorMessage("Element koszyka nie został znaleziony");
                loadCart(); // Odśwież koszyk
                return;
            }

            CartItem cartItem = cartItemOpt.get();
            Product product = cartItem.getProduct();

            // Sprawdź dostępność produktu
            if (!product.isActive()) {
                addErrorMessage("Produkt nie jest już dostępny");
                loadCart(); // Odśwież koszyk
                return;
            }

            if (product.getStockQuantity() == null || !product.isAvailable(newQuantity)) {
                String message = String.format(
                        "Niewystarczająca ilość w magazynie. Dostępne: %d, żądane: %d",
                        product.getStockQuantity() != null ? product.getStockQuantity() : 0, newQuantity);
                addErrorMessage(message);
                loadCart(); // Odśwież koszyk
                return;
            }

            Integer oldQuantity = cartItem.getQuantity();
            cartService.updateCartItemQuantity(cartItemId, newQuantity);
            loadCart(); // Odśwież koszyk po zmianie

            addInfoMessage(String.format("Zaktualizowano ilość z %d na %d", oldQuantity, newQuantity));
            logger.info("Updated cart item {} quantity from {} to {}", cartItemId, oldQuantity, newQuantity);

        } catch (Exception e) {
            logger.error("Error updating cart item {} quantity to {}", cartItemId, newQuantity, e);
            addErrorMessage("Błąd podczas aktualizacji ilości: " + e.getMessage());
            loadCart(); // Odśwież koszyk w przypadku błędu
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

    // Wymuszenie odświeżenia koszyka
    public void forceRefreshCart() {
        this.cartLoaded = false;
        this.cartItems.clear();
        loadCartSafely();
        addInfoMessage("Koszyk został odświeżony");
    }

    // Sprawdzenie czy koszyk jest ważny
    public boolean isCartValid() {
        if (cartItems == null || cartItems.isEmpty()) {
            return true; // Pusty koszyk jest zawsze ważny
        }

        try {
            for (CartItem item : cartItems) {
                Product product = item.getProduct();
                if (!product.isActive() || !product.isAvailable(item.getQuantity())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking cart validity", e);
            return false;
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

        // Sprawdź aktualny stan w bazie danych
        if (!isCartValid()) {
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