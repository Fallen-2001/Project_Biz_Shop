package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.dao.ProductDaoInterface;
import org.example.model.Product;
import org.example.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductServiceImpl implements ProductServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Inject
    private ProductDaoInterface productDao;

    @Inject
    private AuthServiceInterface authService;

    @Override
    public List<Product> getAllProducts() {
        logger.debug("Getting all products");
        return productDao.findAll();
    }

    @Override
    public List<Product> getActiveProducts() {
        logger.debug("Getting active products");
        return productDao.findActiveProducts();
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        logger.debug("Getting products by category: {}", category);
        if (category == null || category.trim().isEmpty()) {
            return getActiveProducts();
        }
        return productDao.findByCategory(category.trim());
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        logger.debug("Getting product by id: {}", id);
        if (id == null) {
            return Optional.empty();
        }
        return productDao.findById(id);
    }

    @Override
    public List<Product> searchProducts(String searchTerm) {
        logger.debug("Searching products with term: {}", searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getActiveProducts();
        }
        return productDao.searchByName(searchTerm.trim());
    }

    @Override
    @Transactional
    public void addProduct(Product product) throws Exception {
        validateAdminAccess();
        validateProduct(product);

        logger.debug("Adding new product: {}", product.getName());

        // Ustaw domyślne wartości
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        if (!product.isActive()) { // Zmienione z getActive() na isActive()
            product.setActive(true);
        }

        try {
            productDao.save(product);
            logger.info("Product added successfully: {}", product.getName());
        } catch (Exception e) {
            logger.error("Error adding product: {}", product.getName(), e);
            throw new Exception("Błąd podczas dodawania produktu", e);
        }
    }

    @Override
    @Transactional
    public void updateProduct(Product product) throws Exception {
        validateAdminAccess();
        validateProduct(product);

        if (product.getId() == null) {
            throw new Exception("ID produktu jest wymagane do aktualizacji");
        }

        logger.debug("Updating product: {}", product.getId());

        Optional<Product> existingProductOpt = productDao.findById(product.getId());
        if (existingProductOpt.isEmpty()) {
            throw new Exception("Produkt nie został znaleziony");
        }

        try {
            productDao.update(product);
            logger.info("Product updated successfully: {}", product.getId());
        } catch (Exception e) {
            logger.error("Error updating product: {}", product.getId(), e);
            throw new Exception("Błąd podczas aktualizacji produktu", e);
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) throws Exception {
        validateAdminAccess();

        if (id == null) {
            throw new Exception("ID produktu jest wymagane");
        }

        logger.debug("Deleting product: {}", id);

        Optional<Product> productOpt = productDao.findById(id);
        if (productOpt.isEmpty()) {
            throw new Exception("Produkt nie został znaleziony");
        }

        try {
            productDao.delete(id);
            logger.info("Product deleted successfully: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting product: {}", id, e);
            throw new Exception("Błąd podczas usuwania produktu", e);
        }
    }

    @Transactional
    public void deactivateProduct(Long id) throws Exception {
        validateAdminAccess();

        Optional<Product> productOpt = productDao.findById(id);
        if (productOpt.isEmpty()) {
            throw new Exception("Produkt nie został znaleziony");
        }

        Product product = productOpt.get();
        product.setActive(false);
        productDao.update(product);

        logger.info("Product deactivated: {}", id);
    }

    @Transactional
    public void activateProduct(Long id) throws Exception {
        validateAdminAccess();

        Optional<Product> productOpt = productDao.findById(id);
        if (productOpt.isEmpty()) {
            throw new Exception("Produkt nie został znaleziony");
        }

        Product product = productOpt.get();
        product.setActive(true);
        productDao.update(product);

        logger.info("Product activated: {}", id);
    }

    @Transactional
    public void updateStock(Long productId, Integer newStock) throws Exception {
        validateAdminAccess();

        if (newStock == null || newStock < 0) {
            throw new Exception("Stan magazynowy musi być liczbą dodatnią");
        }

        Optional<Product> productOpt = productDao.findById(productId);
        if (productOpt.isEmpty()) {
            throw new Exception("Produkt nie został znaleziony");
        }

        Product product = productOpt.get();
        Integer oldStock = product.getStockQuantity();
        product.setStockQuantity(newStock);
        productDao.update(product);

        logger.info("Stock updated for product {}: {} -> {}", productId, oldStock, newStock);
    }

    public List<String> getAllCategories() {
        // W rzeczywistej implementacji można by dodać osobną metodę w DAO
        // Tymczasowo zwracamy przykładowe kategorie
        return List.of("Elektronika", "Odzież", "Dom i ogród", "Sport", "Książki", "Zabawki");
    }

    public List<Product> getLowStockProducts(int threshold) throws Exception {
        validateAdminAccess();

        return productDao.findAll().stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() <= threshold)
                .toList();
    }

    private void validateAdminAccess() throws Exception {
        var currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new Exception("Musisz być zalogowany");
        }
        if (currentUser.getRole() != Role.ADMIN) {
            throw new Exception("Tylko administrator może wykonać tę operację");
        }
    }

    private void validateProduct(Product product) throws Exception {
        if (product == null) {
            throw new Exception("Produkt nie może być null");
        }

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new Exception("Nazwa produktu jest wymagana");
        }

        if (product.getName().trim().length() > 255) {
            throw new Exception("Nazwa produktu jest za długa (max 255 znaków)");
        }

        if (product.getPrice() == null) {
            throw new Exception("Cena produktu jest wymagana");
        }

        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Cena produktu musi być większa od 0");
        }

        if (product.getPrice().compareTo(new BigDecimal("999999.99")) > 0) {
            throw new Exception("Cena produktu jest za wysoka");
        }

        if (product.getDescription() != null && product.getDescription().length() > 1000) {
            throw new Exception("Opis produktu jest za długi (max 1000 znaków)");
        }

        if (product.getCategory() != null && product.getCategory().length() > 100) {
            throw new Exception("Kategoria jest za długa (max 100 znaków)");
        }

        if (product.getStockQuantity() != null && product.getStockQuantity() < 0) {
            throw new Exception("Stan magazynowy nie może być ujemny");
        }
    }
}