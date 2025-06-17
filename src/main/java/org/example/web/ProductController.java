package org.example.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Product;
import org.example.service.ProductService;
import java.util.List;

@Named
@RequestScoped
public class ProductController {
    private List<Product> products;
    private Product selectedProduct;

    @Inject
    private ProductService productService;

    @PostConstruct
    public void init() {
        products = productService.getAllProducts();
    }

    public String addToCart(Product product) {

        selectedProduct = product;
        return null;
    }


    public List<Product> getProducts() {
        return products;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }
}