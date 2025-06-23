package org.example.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Product;
import org.example.service.ProductServiceInterface;
import org.example.service.CartServiceInterface;
import org.example.service.AuthServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Named
@RequestScoped
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private List<Product> products;
    private Product selectedProduct;
    private String searchTerm;
    private String selectedCategory;
    private Integer selectedQuantity = 1;

    @Inject
    private ProductServiceInterface productService;

    @Inject
    private CartServiceInterface cartService;

    @Inject
    private AuthServiceInterface authService;

    @PostConstruct
    public void init() {
        loadProducts();
    }

    public void loadProducts() {
        try {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                products = productService.searchProducts(searchTerm.trim());
                logger.debug("Searched products with term '{}', found {} results", searchTerm.trim(), products.size());
            } else if (selectedCategory != null && !selectedCategory.trim().isEmpty()) {
                products = productService.getProductsByCategory(selectedCategory.trim());
                logger.debug("Filtered products by category '{}', found {} results", selectedCategory.trim(), products.size());
            } else {
                products = productService.getActiveProducts();
                logger.debug("Loaded all active products, found {} results", products.size());
            }
        } catch (Exception e) {
            logger.error("Error loading products", e);
            addErrorMessage("Błąd podczas ładowania produktów: " + e.getMessage());
            products = List.of(); // Pusta lista w przypadku błędu
        }
    }

    public String addToCart(Product product) {
        return addToCart(product.getId(), selectedQuantity != null ? selectedQuantity : 1);
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
            // Znajdź produkt żeby sprawdzić jego dostępność
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isEmpty()) {
                addErrorMessage("Produkt nie został znaleziony");
                return null;
            }

            Product product = productOpt.get();
            selectedProduct = product;

            // Sprawdź dostępność
            if (!product.isActive()) {
                addErrorMessage("Produkt nie jest dostępny");
                return null;
            }

            if (product.getStockQuantity() == null || product.getStockQuantity() == 0) {
                addErrorMessage("Produkt jest wyprzedany");
                return null;
            }

            if (quantity > product.getStockQuantity()) {
                addErrorMessage(String.format("Dostępne tylko %d sztuk produktu", product.getStockQuantity()));
                return null;
            }

            cartService.addToCart(productId, quantity);

            String message = quantity == 1 ?
                    String.format("Produkt '%s' został dodany do koszyka", product.getName()) :
                    String.format("Dodano %d szt. produktu '%s' do koszyka", quantity, product.getName());

            addInfoMessage(message);
            logger.info("Product {} added to cart for user: {} with quantity: {}",
                    productId, authService.getCurrentUser().getUsername(), quantity);

        } catch (Exception e) {
            logger.error("Error adding product {} to cart with quantity {}", productId, quantity, e);
            addErrorMessage("Błąd podczas dodawania produktu do koszyka: " + e.getMessage());
        }

        return null; // Pozostań na tej samej stronie
    }

    public String viewProductDetails(Long productId) {
        try {
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isPresent()) {
                selectedProduct = productOpt.get();
                return "/user/product-details.xhtml?faces-redirect=true&productId=" + productId;
            } else {
                addErrorMessage("Produkt nie został znaleziony");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error viewing product details for ID: {}", productId, e);
            addErrorMessage("Błąd podczas ładowania szczegółów produktu");
            return null;
        }
    }

    public void search() {
        logger.debug("Searching products with term: '{}'", searchTerm);
        loadProducts();
    }

    public void filterByCategory() {
        logger.debug("Filtering products by category: '{}'", selectedCategory);
        loadProducts();
    }

    public void clearFilters() {
        logger.debug("Clearing all filters");
        searchTerm = null;
        selectedCategory = null;
        selectedQuantity = 1;
        loadProducts();
    }

    // Metody pomocnicze dla UI
    public String getStockStatusText(Product product) {
        if (!product.isActive()) {
            return "Produkt niedostępny";
        }

        Integer stock = product.getStockQuantity();
        if (stock == null || stock == 0) {
            return "Wyprzedany";
        } else if (stock <= 5) {
            return "Ostatnie sztuki (" + stock + " szt.)";
        } else if (stock <= 10) {
            return "Ograniczona dostępność (" + stock + " szt.)";
        } else {
            return "Dostępny (" + stock + " szt.)";
        }
    }

    public String getStockStatusClass(Product product) {
        if (!product.isActive()) {
            return "stock-out";
        }

        Integer stock = product.getStockQuantity();
        if (stock == null || stock == 0) {
            return "stock-out";
        } else if (stock <= 5) {
            return "stock-low";
        } else if (stock <= 10) {
            return "stock-medium";
        } else {
            return "stock-high";
        }
    }

    public boolean isProductAvailable(Product product) {
        return product.isActive() &&
                product.getStockQuantity() != null &&
                product.getStockQuantity() > 0;
    }

    public int getMaxQuantityForProduct(Product product) {
        if (!isProductAvailable(product)) {
            return 0;
        }
        // Maksymalnie 10 sztuk lub dostępną ilość, jeśli jest mniejsza
        return Math.min(product.getStockQuantity(), 10);
    }

    public List<String> getAvailableCategories() {
        return List.of("Elektronika", "Odzież", "Dom i ogród", "Sport", "Książki", "Zabawki", "Różne");
    }

    public List<Product> getRecommendedProducts() {
        try {
            // Zwróć losowe 4 produkty jako rekomendacje
            List<Product> allProducts = productService.getActiveProducts();
            return allProducts.stream()
                    .filter(this::isProductAvailable)
                    .limit(4)
                    .toList();
        } catch (Exception e) {
            logger.error("Error loading recommended products", e);
            return List.of();
        }
    }

    public List<Product> getProductsByCategory(String category) {
        try {
            return productService.getProductsByCategory(category);
        } catch (Exception e) {
            logger.error("Error loading products by category: {}", category, e);
            return List.of();
        }
    }

    public long getTotalProductCount() {
        return products != null ? products.size() : 0;
    }

    public boolean hasSearchResults() {
        return products != null && !products.isEmpty();
    }

    public boolean isSearchActive() {
        return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
                (selectedCategory != null && !selectedCategory.trim().isEmpty());
    }

    public String getCurrentSearchInfo() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return "Wyniki wyszukiwania dla: '" + searchTerm + "'";
        } else if (selectedCategory != null && !selectedCategory.trim().isEmpty()) {
            return "Kategoria: " + selectedCategory;
        } else {
            return "Wszystkie produkty";
        }
    }

    // Gettery i settery
    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public Integer getSelectedQuantity() {
        return selectedQuantity;
    }

    public void setSelectedQuantity(Integer selectedQuantity) {
        this.selectedQuantity = selectedQuantity;
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