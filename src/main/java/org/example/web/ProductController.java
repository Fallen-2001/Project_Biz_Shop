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

@Named
@RequestScoped
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private List<Product> products;
    private Product selectedProduct;
    private String searchTerm;
    private String selectedCategory;

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
            } else if (selectedCategory != null && !selectedCategory.trim().isEmpty()) {
                products = productService.getProductsByCategory(selectedCategory.trim());
            } else {
                products = productService.getActiveProducts();
            }
            logger.debug("Loaded {} products", products.size());
        } catch (Exception e) {
            logger.error("Error loading products", e);
            addErrorMessage("Błąd podczas ładowania produktów");
        }
    }

    public String addToCart(Product product) {
        if (!authService.isLoggedIn()) {
            addErrorMessage("Musisz być zalogowany, aby dodać produkt do koszyka");
            return "/login.xhtml?faces-redirect=true";
        }

        try {
            cartService.addToCart(product.getId(), 1);
            selectedProduct = product;
            addInfoMessage("Produkt został dodany do koszyka");
            logger.info("Product {} added to cart", product.getId());
        } catch (Exception e) {
            logger.error("Error adding product {} to cart", product.getId(), e);
            addErrorMessage("Błąd podczas dodawania produktu do koszyka: " + e.getMessage());
        }

        return null; // Pozostań na tej samej stronie
    }

    public void search() {
        loadProducts();
    }

    public void filterByCategory() {
        loadProducts();
    }

    public void clearFilters() {
        searchTerm = null;
        selectedCategory = null;
        loadProducts();
    }

    // Gettery i settery
    public List<Product> getProducts() {
        return products;
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

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", message));
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Błąd", message));
    }
}