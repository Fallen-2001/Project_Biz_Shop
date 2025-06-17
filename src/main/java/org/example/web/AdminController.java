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

@Named
@RequestScoped
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private Product newProduct = new Product();

    @Inject
    private ProductServiceInterface productService;

    public void addProduct() {
        try {
            productService.addProduct(newProduct);
            newProduct = new Product(); // Reset formularza
            addInfoMessage("Produkt został dodany pomyślnie");
            logger.info("Product added successfully: {}", newProduct.getName());
        } catch (Exception e) {
            logger.error("Error adding product", e);
            addErrorMessage("Błąd podczas dodawania produktu: " + e.getMessage());
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