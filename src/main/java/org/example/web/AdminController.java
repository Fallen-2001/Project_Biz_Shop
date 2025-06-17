package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Product;
import org.example.service.ProductService;

@Named
@RequestScoped
public class AdminController {
    private Product newProduct = new Product();

    @Inject
    private ProductService productService;

    public void addProduct() {
        productService.addProduct(newProduct);
        newProduct = new Product(); // Сбрасываем форму
    }

    public void deleteProduct(Long productId) {
        productService.deleteProduct(productId);
    }


    public Product getNewProduct() { return newProduct; }
    public void setNewProduct(Product newProduct) { this.newProduct = newProduct; }
}