package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Product;
import org.example.service.ProductServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Named
@RequestScoped
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private Product newProduct = new Product();

    @Inject
    private ProductServiceInterface productService;

    public void addProduct() {
        try {
            // Ustaw domyślne wartości jeśli nie zostały podane
            if (newProduct.getStockQuantity() == null) {
                newProduct.setStockQuantity(10); // Domyślnie 10 sztuk
            }

            if (newProduct.getCategory() == null || newProduct.getCategory().trim().isEmpty()) {
                newProduct.setCategory("Różne"); // Domyślna kategoria
            }

            if (newProduct.getDescription() == null || newProduct.getDescription().trim().isEmpty()) {
                newProduct.setDescription("Opis produktu"); // Domyślny opis
            }

            // Ustaw produkt jako aktywny
            newProduct.setActive(true);

            productService.addProduct(newProduct);

            // Resetuj formularz z domyślnymi wartościami
            resetForm();

            addInfoMessage("Produkt został dodany pomyślnie. Ilość w magazynie: " + newProduct.getStockQuantity());
            logger.info("Product added successfully: {} with stock: {}", newProduct.getName(), newProduct.getStockQuantity());

        } catch (Exception e) {
            logger.error("Error adding product", e);
            addErrorMessage("Błąd podczas dodawania produktu: " + e.getMessage());
        }
    }

    public void updateProduct(Product product) {
        try {
            productService.updateProduct(product);
            addInfoMessage("Produkt został zaktualizowany");
            logger.info("Product updated: {}", product.getId());
        } catch (Exception e) {
            logger.error("Error updating product: {}", product.getId(), e);
            addErrorMessage("Błąd podczas aktualizacji produktu: " + e.getMessage());
        }
    }

    public void deleteProduct(Long productId) {
        try {
            productService.deleteProduct(productId);
            addInfoMessage("Produkt został usunięty");
            logger.info("Product deleted: {}", productId);
        } catch (Exception e) {
            logger.error("Error deleting product: {}", productId, e);
            addErrorMessage("Błąd podczas usuwania produktu: " + e.getMessage());
        }
    }

    public void deactivateProduct(Long productId) {
        try {
            // Zakładam, że ProductService ma metodę deactivateProduct
            // Jeśli nie ma, możemy użyć updateProduct
            Product product = productService.getProductById(productId).orElse(null);
            if (product != null) {
                product.setActive(false);
                productService.updateProduct(product);
                addInfoMessage("Produkt został dezaktywowany");
                logger.info("Product deactivated: {}", productId);
            } else {
                addErrorMessage("Produkt nie został znaleziony");
            }
        } catch (Exception e) {
            logger.error("Error deactivating product: {}", productId, e);
            addErrorMessage("Błąd podczas dezaktywacji produktu: " + e.getMessage());
        }
    }

    public void updateStock(Long productId, Integer newStock) {
        try {
            Product product = productService.getProductById(productId).orElse(null);
            if (product != null) {
                product.setStockQuantity(newStock);
                productService.updateProduct(product);
                addInfoMessage("Stan magazynowy został zaktualizowany");
                logger.info("Stock updated for product {}: {}", productId, newStock);
            } else {
                addErrorMessage("Produkt nie został znaleziony");
            }
        } catch (Exception e) {
            logger.error("Error updating stock for product: {}", productId, e);
            addErrorMessage("Błąd podczas aktualizacji stanu magazynowego: " + e.getMessage());
        }
    }

    private void resetForm() {
        newProduct = new Product();
        // Ustaw domyślne wartości dla nowego formularza
        newProduct.setStockQuantity(10);
        newProduct.setCategory("Różne");
        newProduct.setActive(true);
    }

    public List<String> getAvailableCategories() {
        return List.of("Elektronika", "Odzież", "Dom i ogród", "Sport", "Książki", "Zabawki", "Różne");
    }

    // Gettery i settery
    public Product getNewProduct() {
        return newProduct;
    }

    public void setNewProduct(Product newProduct) {
        this.newProduct = newProduct;
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