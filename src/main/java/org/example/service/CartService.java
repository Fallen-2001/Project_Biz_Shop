package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.dao.CartDaoInterface;
import org.example.dao.ProductDaoInterface;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CartService implements CartServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Inject
    private CartDaoInterface cartDao;

    @Inject
    private ProductDaoInterface productDao;

    @Inject
    private AuthServiceInterface authService;

    @Override
    public List<CartItem> getCartItems(User user) {
        if (user == null) {
            logger.warn("Attempted to get cart items for null user");
            return List.of();
        }

        logger.debug("Getting cart items for user: {}", user.getUsername());
        try {
            List<CartItem> items = cartDao.findByUserWithProducts(user);
            logger.debug("Found {} cart items for user: {}", items.size(), user.getUsername());
            return items;
        } catch (Exception e) {
            logger.error("Error getting cart items for user: {}", user.getUsername(), e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public void addToCart(Long productId, Integer quantity) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany, aby dodać produkt do koszyka");
        }

        addToCart(currentUser, productId, 1); // Zawsze dodajemy 1 sztukę
    }

    @Override
    @Transactional
    public void addToCart(User user, Long productId, Integer quantity) throws Exception {
        logger.debug("Adding product {} to cart for user: {}", productId, user.getUsername());

        // Walidacja podstawowa
        if (user == null) {
            throw new Exception("Użytkownik nie może być null");
        }

        if (productId == null) {
            throw new Exception("ID produktu nie może być null");
        }

        // Sprawdź czy produkt istnieje
        Optional<Product> productOpt = productDao.findById(productId);
        if (productOpt.isEmpty()) {
            logger.warn("Attempt to add non-existent product {} to cart", productId);
            throw new Exception("Produkt nie został znaleziony");
        }

        Product product = productOpt.get();

        // Sprawdź czy produkt jest aktywny
        if (!product.isActive()) {
            logger.warn("Attempt to add inactive product {} to cart", productId);
            throw new Exception("Produkt nie jest dostępny");
        }

        // Sprawdź dostępność
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            logger.warn("Attempt to add out-of-stock product {} to cart", productId);
            throw new Exception("Produkt jest obecnie niedostępny");
        }

        // Sprawdź czy produkt już jest w koszyku
        Optional<CartItem> existingItemOpt = cartDao.findByUserAndProduct(user, product);

        if (existingItemOpt.isPresent()) {
            // Jeśli produkt już jest w koszyku, nie dodawaj ponownie
            throw new Exception("Produkt już znajduje się w koszyku");
        } else {
            // Dodaj nowy element do koszyka z ilością = 1
            logger.debug("Adding new cart item for product {} with quantity 1", productId);

            CartItem newItem = new CartItem(user, product, 1);
            cartDao.save(newItem);
            logger.info("Added new cart item for user {} and product {} with quantity 1",
                    user.getUsername(), productId);
        }
    }

    @Override
    @Transactional
    public void removeFromCart(Long cartItemId) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany");
        }

        if (cartItemId == null) {
            throw new Exception("ID elementu koszyka nie może być null");
        }

        logger.debug("Removing cart item {} for user {}", cartItemId, currentUser.getUsername());

        // Sprawdź czy element należy do użytkownika
        Optional<CartItem> cartItemOpt = cartDao.findByIdAndUser(cartItemId, currentUser);

        if (cartItemOpt.isEmpty()) {
            throw new Exception("Element koszyka nie został znaleziony lub nie należy do Ciebie");
        }

        cartDao.delete(cartItemId);
        logger.info("Removed cart item {} for user {}", cartItemId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        if (user == null) {
            logger.warn("Attempted to clear cart for null user");
            return;
        }

        logger.debug("Clearing cart for user: {}", user.getUsername());
        try {
            cartDao.deleteByUser(user);
            logger.info("Cart cleared for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error clearing cart for user: {}", user.getUsername(), e);
            throw new RuntimeException("Błąd podczas czyszczenia koszyka", e);
        }
    }

    @Override
    public BigDecimal getCartTotal(User user) {
        if (user == null) {
            return BigDecimal.ZERO;
        }

        try {
            List<CartItem> cartItems = getCartItems(user);
            return cartItems.stream()
                    .map(item -> item.getProduct().getPrice()) // Każdy produkt to 1 sztuka
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            logger.error("Error calculating cart total for user: {}", user.getUsername(), e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public int getCartItemCount(User user) {
        if (user == null) {
            return 0;
        }

        try {
            // Liczba produktów = liczba pozycji (każda po 1 sztuce)
            return cartDao.countByUser(user);
        } catch (Exception e) {
            logger.error("Error getting cart item count for user: {}", user.getUsername(), e);
            return 0;
        }
    }

    @Override
    public boolean isCartEmpty(User user) {
        return getCartItemCount(user) == 0;
    }

    // Dodatkowe metody pomocnicze

    /**
     * Sprawdza dostępność wszystkich produktów w koszyku
     */
    public List<CartItem> getUnavailableCartItems(User user) {
        if (user == null) {
            return List.of();
        }

        try {
            List<CartItem> cartItems = getCartItems(user);
            return cartItems.stream()
                    .filter(item -> {
                        Product product = item.getProduct();
                        return !product.isActive() || !product.isAvailable(item.getQuantity());
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("Error getting unavailable cart items for user: {}", user.getUsername(), e);
            return List.of();
        }
    }

    /**
     * Synchronizuje ilości w koszyku z dostępnością produktów
     */
    @Transactional
    public void synchronizeCartWithStock(User user) {
        if (user == null) {
            logger.warn("Attempted to synchronize cart for null user");
            return;
        }

        logger.debug("Synchronizing cart with stock for user: {}", user.getUsername());

        try {
            List<CartItem> cartItems = getCartItems(user);

            for (CartItem item : cartItems) {
                Product product = item.getProduct();

                // Usuń nieaktywne produkty
                if (!product.isActive()) {
                    cartDao.delete(item.getId());
                    logger.info("Removed inactive product {} from cart of user {}",
                            product.getName(), user.getUsername());
                    continue;
                }

                // Usuń produkty bez stanu magazynowego
                if (product.getStockQuantity() == null) {
                    cartDao.delete(item.getId());
                    logger.info("Removed product {} with null stock from cart of user {}",
                            product.getName(), user.getUsername());
                    continue;
                }

                // Dostosuj ilość do dostępności
                if (item.getQuantity() > product.getStockQuantity()) {
                    if (product.getStockQuantity() > 0) {
                        // Ogranicz ilość do dostępnej
                        int newQuantity = Math.min(product.getStockQuantity(), 10); // Max 10 sztuk
                        item.setQuantity(newQuantity);
                        cartDao.updateAndRefresh(item);
                        logger.info("Adjusted quantity for product {} in cart of user {} to {}",
                                product.getName(), user.getUsername(), newQuantity);
                    } else {
                        // Usuń wyprzedane produkty
                        cartDao.delete(item.getId());
                        logger.info("Removed out-of-stock product {} from cart of user {}",
                                product.getName(), user.getUsername());
                    }
                }

                // Sprawdź maksymalną dozwoloną ilość
                if (item.getQuantity() > 10) {
                    item.setQuantity(10);
                    cartDao.updateAndRefresh(item);
                    logger.info("Limited quantity for product {} in cart of user {} to 10",
                            product.getName(), user.getUsername());
                }
            }

            logger.info("Cart synchronization completed for user: {}", user.getUsername());

        } catch (Exception e) {
            logger.error("Error synchronizing cart for user: {}", user.getUsername(), e);
            throw new RuntimeException("Błąd podczas synchronizacji koszyka", e);
        }
    }

    /**
     * Sprawdza czy można złożyć zamówienie z aktualnym koszykiem
     */
    public boolean canPlaceOrder(User user) {
        if (user == null) {
            return false;
        }

        try {
            List<CartItem> cartItems = getCartItems(user);

            if (cartItems.isEmpty()) {
                return false;
            }

            return cartItems.stream().allMatch(item -> {
                Product product = item.getProduct();
                return product.isActive() &&
                        product.getStockQuantity() != null &&
                        product.isAvailable(item.getQuantity());
            });

        } catch (Exception e) {
            logger.error("Error checking if can place order for user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Pobiera elementy koszyka z blokadą (do operacji krytycznych)
     */
    @Transactional
    public List<CartItem> getCartItemsWithLock(User user) {
        if (user == null) {
            return List.of();
        }

        try {
            List<CartItem> cartItems = getCartItems(user);

            // Zablokuj każdy element osobno
            for (CartItem item : cartItems) {
                cartDao.findWithLock(item.getId());
            }

            return cartItems;
        } catch (Exception e) {
            logger.error("Error getting cart items with lock for user: {}", user.getUsername(), e);
            return List.of();
        }
    }

    /**
     * Sprawdza czy użytkownik ma już dany produkt w koszyku
     */
    public boolean hasProductInCart(User user, Long productId) {
        if (user == null || productId == null) {
            return false;
        }

        try {
            Optional<Product> productOpt = productDao.findById(productId);
            if (productOpt.isEmpty()) {
                return false;
            }

            return cartDao.existsByUserAndProduct(user, productOpt.get());
        } catch (Exception e) {
            logger.error("Error checking if user {} has product {} in cart", user.getUsername(), productId, e);
            return false;
        }
    }

    /**
     * Pobiera ilość konkretnego produktu w koszyku użytkownika
     */
    public int getProductQuantityInCart(User user, Long productId) {
        if (user == null || productId == null) {
            return 0;
        }

        try {
            Optional<Product> productOpt = productDao.findById(productId);
            if (productOpt.isEmpty()) {
                return 0;
            }

            Optional<CartItem> cartItemOpt = cartDao.findByUserAndProduct(user, productOpt.get());
            return cartItemOpt.map(CartItem::getQuantity).orElse(0);

        } catch (Exception e) {
            logger.error("Error getting product {} quantity in cart for user {}", productId, user.getUsername(), e);
            return 0;
        }
    }

    /**
     * Usuwa nieaktywne produkty z koszyka
     */
    @Transactional
    public int removeInactiveProductsFromCart(User user) {
        if (user == null) {
            return 0;
        }

        try {
            List<CartItem> cartItems = getCartItems(user);
            int removedCount = 0;

            for (CartItem item : cartItems) {
                Product product = item.getProduct();
                if (!product.isActive() || product.getStockQuantity() == null || product.getStockQuantity() == 0) {
                    cartDao.delete(item.getId());
                    removedCount++;
                    logger.info("Removed inactive/unavailable product {} from cart of user {}",
                            product.getName(), user.getUsername());
                }
            }

            if (removedCount > 0) {
                logger.info("Removed {} inactive products from cart of user {}", removedCount, user.getUsername());
            }

            return removedCount;

        } catch (Exception e) {
            logger.error("Error removing inactive products from cart for user: {}", user.getUsername(), e);
            return 0;
        }
    }

    /**
     * Pobiera podsumowanie koszyka
     */
    public CartSummary getCartSummary(User user) {
        if (user == null) {
            return new CartSummary();
        }

        try {
            List<CartItem> cartItems = getCartItems(user);

            int totalItems = cartItems.size();
            int totalQuantity = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
            BigDecimal totalValue = getCartTotal(user);
            boolean canPlaceOrder = canPlaceOrder(user);

            return new CartSummary(totalItems, totalQuantity, totalValue, canPlaceOrder);

        } catch (Exception e) {
            logger.error("Error getting cart summary for user: {}", user.getUsername(), e);
            return new CartSummary();
        }
    }

    /**
     * Klasa pomocnicza dla podsumowania koszyka
     */
    public static class CartSummary {
        private final int totalItems;
        private final int totalQuantity;
        private final BigDecimal totalValue;
        private final boolean canPlaceOrder;

        public CartSummary() {
            this(0, 0, BigDecimal.ZERO, false);
        }

        public CartSummary(int totalItems, int totalQuantity, BigDecimal totalValue, boolean canPlaceOrder) {
            this.totalItems = totalItems;
            this.totalQuantity = totalQuantity;
            this.totalValue = totalValue;
            this.canPlaceOrder = canPlaceOrder;
        }

        public int getTotalItems() { return totalItems; }
        public int getTotalQuantity() { return totalQuantity; }
        public BigDecimal getTotalValue() { return totalValue; }
        public boolean isCanPlaceOrder() { return canPlaceOrder; }
    }
}