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
        logger.debug("Getting cart items for user: {}", user.getUsername());
        return cartDao.findByUser(user);
    }

    @Override
    @Transactional
    public void addToCart(Long productId, Integer quantity) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany, aby dodać produkt do koszyka");
        }

        addToCart(currentUser, productId, quantity);
    }

    @Override
    @Transactional
    public void addToCart(User user, Long productId, Integer quantity) throws Exception {
        logger.debug("Adding product {} to cart for user: {} with quantity: {}", productId, user.getUsername(), quantity);

        // Walidacja podstawowa
        if (quantity == null || quantity <= 0) {
            throw new Exception("Ilość musi być większa od 0");
        }

        if (quantity > 100) {
            throw new Exception("Nie można dodać więcej niż 100 sztuk jednocześnie");
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

        // Szczegółowe sprawdzenie dostępności
        if (product.getStockQuantity() == null) {
            logger.error("Product {} has null stock quantity", productId);
            throw new Exception("Błąd systemu: brak informacji o stanie magazynowym produktu");
        }

        if (product.getStockQuantity() == 0) {
            logger.warn("Attempt to add out-of-stock product {} to cart", productId);
            throw new Exception("Produkt jest obecnie niedostępny (wyprzedany)");
        }

        // Sprawdź czy istnieje już element w koszyku
        Optional<CartItem> existingItemOpt = cartDao.findByUserAndProduct(user, product);

        if (existingItemOpt.isPresent()) {
            // Aktualizuj istniejący element koszyka
            CartItem existingItem = existingItemOpt.get();
            int currentQuantityInCart = existingItem.getQuantity();
            int newTotalQuantity = currentQuantityInCart + quantity;

            logger.debug("Product {} already in cart with quantity {}, adding {}",
                    productId, currentQuantityInCart, quantity);

            // Sprawdź dostępność dla nowej łącznej ilości
            if (!product.isAvailable(newTotalQuantity)) {
                String message = String.format(
                        "Niewystarczająca ilość w magazynie. Dostępne: %d, w koszyku: %d, próbowano dodać: %d",
                        product.getStockQuantity(), currentQuantityInCart, quantity);
                logger.warn("Insufficient stock for product {}: {}", productId, message);
                throw new Exception(message);
            }

            existingItem.setQuantity(newTotalQuantity);
            cartDao.update(existingItem);
            logger.info("Updated cart item quantity for user {} and product {}: {} -> {}",
                    user.getUsername(), productId, currentQuantityInCart, newTotalQuantity);

        } else {
            // Dodaj nowy element do koszyka
            logger.debug("Adding new cart item for product {} with quantity {}", productId, quantity);

            // Sprawdź dostępność dla nowego elementu
            if (!product.isAvailable(quantity)) {
                String message = String.format(
                        "Niewystarczająca ilość w magazynie. Dostępne: %d, żądane: %d",
                        product.getStockQuantity(), quantity);
                logger.warn("Insufficient stock for new cart item {}: {}", productId, message);
                throw new Exception(message);
            }

            CartItem newItem = new CartItem(user, product, quantity);
            cartDao.save(newItem);
            logger.info("Added new cart item for user {} and product {} with quantity {}",
                    user.getUsername(), productId, quantity);
        }
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(Long cartItemId, Integer newQuantity) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany");
        }

        logger.debug("Updating cart item {} quantity to {}", cartItemId, newQuantity);

        if (newQuantity == null || newQuantity < 0) {
            throw new Exception("Nieprawidłowa ilość");
        }

        if (newQuantity == 0) {
            removeFromCart(cartItemId);
            return;
        }

        if (newQuantity > 100) {
            throw new Exception("Maksymalna ilość to 100 sztuk");
        }

        // Znajdź element koszyka i sprawdź czy należy do użytkownika
        List<CartItem> userCartItems = cartDao.findByUser(currentUser);
        Optional<CartItem> cartItemOpt = userCartItems.stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst();

        if (cartItemOpt.isEmpty()) {
            throw new Exception("Element koszyka nie został znaleziony");
        }

        CartItem cartItem = cartItemOpt.get();
        Product product = cartItem.getProduct();

        // Sprawdź dostępność produktu
        if (!product.isActive()) {
            throw new Exception("Produkt nie jest już dostępny");
        }

        if (product.getStockQuantity() == null || !product.isAvailable(newQuantity)) {
            String message = String.format(
                    "Niewystarczająca ilość w magazynie. Dostępne: %d, żądane: %d",
                    product.getStockQuantity() != null ? product.getStockQuantity() : 0, newQuantity);
            throw new Exception(message);
        }

        Integer oldQuantity = cartItem.getQuantity();
        cartItem.setQuantity(newQuantity);
        cartDao.update(cartItem);

        logger.info("Updated cart item {} quantity from {} to {}", cartItemId, oldQuantity, newQuantity);
    }

    @Override
    @Transactional
    public void removeFromCart(Long cartItemId) throws Exception {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany");
        }

        logger.debug("Removing cart item {} for user {}", cartItemId, currentUser.getUsername());

        // Sprawdź czy element należy do użytkownika
        List<CartItem> userCartItems = cartDao.findByUser(currentUser);
        boolean itemBelongsToUser = userCartItems.stream()
                .anyMatch(item -> item.getId().equals(cartItemId));

        if (!itemBelongsToUser) {
            throw new Exception("Element koszyka nie został znaleziony");
        }

        cartDao.delete(cartItemId);
        logger.info("Removed cart item {} for user {}", cartItemId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        logger.debug("Clearing cart for user: {}", user.getUsername());
        cartDao.deleteByUser(user);
        logger.info("Cart cleared for user: {}", user.getUsername());
    }

    @Override
    public BigDecimal getCartTotal(User user) {
        List<CartItem> cartItems = cartDao.findByUser(user);
        return cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int getCartItemCount(User user) {
        return cartDao.countByUser(user);
    }

    @Override
    public boolean isCartEmpty(User user) {
        return getCartItemCount(user) == 0;
    }

    // Nowe metody pomocnicze

    /**
     * Sprawdza dostępność wszystkich produktów w koszyku
     */
    public List<CartItem> getUnavailableCartItems(User user) {
        List<CartItem> cartItems = cartDao.findByUser(user);
        return cartItems.stream()
                .filter(item -> {
                    Product product = item.getProduct();
                    return !product.isActive() || !product.isAvailable(item.getQuantity());
                })
                .toList();
    }

    /**
     * Synchronizuje ilości w koszyku z dostępnością produktów
     */
    @Transactional
    public void synchronizeCartWithStock(User user) {
        List<CartItem> cartItems = cartDao.findByUser(user);

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Usuń nieaktywne produkty
            if (!product.isActive()) {
                cartDao.delete(item.getId());
                logger.info("Removed inactive product {} from cart of user {}",
                        product.getName(), user.getUsername());
                continue;
            }

            // Dostosuj ilość do dostępności
            if (product.getStockQuantity() != null && item.getQuantity() > product.getStockQuantity()) {
                if (product.getStockQuantity() > 0) {
                    item.setQuantity(product.getStockQuantity());
                    cartDao.update(item);
                    logger.info("Adjusted quantity for product {} in cart of user {} to {}",
                            product.getName(), user.getUsername(), product.getStockQuantity());
                } else {
                    cartDao.delete(item.getId());
                    logger.info("Removed out-of-stock product {} from cart of user {}",
                            product.getName(), user.getUsername());
                }
            }
        }
    }

    /**
     * Sprawdza czy można złożyć zamówienie z aktualnym koszykiem
     */
    public boolean canPlaceOrder(User user) {
        List<CartItem> cartItems = cartDao.findByUser(user);

        if (cartItems.isEmpty()) {
            return false;
        }

        return cartItems.stream().allMatch(item -> {
            Product product = item.getProduct();
            return product.isActive() && product.isAvailable(item.getQuantity());
        });
    }
}