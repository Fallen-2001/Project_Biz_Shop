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
        logger.debug("Adding product {} to cart for user: {}", productId, user.getUsername());

        if (quantity == null || quantity <= 0) {
            throw new Exception("Ilość musi być większa od 0");
        }

        Optional<Product> productOpt = productDao.findById(productId);
        if (productOpt.isEmpty()) {
            throw new Exception("Produkt nie został znaleziony");
        }

        Product product = productOpt.get();
        if (!product.isActive()) {
            throw new Exception("Produkt nie jest dostępny");
        }

        if (!product.isAvailable(quantity)) {
            throw new Exception("Niewystarczająca ilość w magazynie");
        }

        Optional<CartItem> existingItemOpt = cartDao.findByUserAndProduct(user, product);

        if (existingItemOpt.isPresent()) {
            // Aktualizuj istniejący element koszyka
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;

            if (!product.isAvailable(newQuantity)) {
                throw new Exception("Niewystarczająca ilość w magazynie. Dostępne: " + product.getStockQuantity());
            }

            existingItem.setQuantity(newQuantity);
            cartDao.update(existingItem);
            logger.info("Updated cart item quantity for user {} and product {}", user.getUsername(), productId);
        } else {
            // Dodaj nowy element do koszyka
            CartItem newItem = new CartItem(user, product, quantity);
            cartDao.save(newItem);
            logger.info("Added new cart item for user {} and product {}", user.getUsername(), productId);
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

        if (!product.isAvailable(newQuantity)) {
            throw new Exception("Niewystarczająca ilość w magazynie. Dostępne: " + product.getStockQuantity());
        }

        cartItem.setQuantity(newQuantity);
        cartDao.update(cartItem);
        logger.info("Updated cart item {} quantity to {}", cartItemId, newQuantity);
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
}
