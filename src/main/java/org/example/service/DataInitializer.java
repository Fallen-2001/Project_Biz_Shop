package org.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.dao.UserDao;
import org.example.dao.ProductDao;
import org.example.model.User;
import org.example.model.Product;
import org.example.model.Role;
import java.math.BigDecimal;

@ApplicationScoped
public class DataInitializer {

    @Inject
    private UserDao userDao;

    @Inject
    private ProductDao productDao;

    @PostConstruct
    @Transactional
    public void initializeData() {
        initializeUsers();
        initializeProducts();
    }

    private void initializeUsers() {
        // Tworzenie użytkownika admin
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("admin"); // W produkcji powinna być zahashowana!
        admin.setRole(Role.ADMIN);
        userDao.save(admin);

        // Tworzenie zwykłego użytkownika
        User user = new User();
        user.setUsername("user");
        user.setPassword("user"); // W produkcji powinna być zahashowana!
        user.setRole(Role.USER);
        userDao.save(user);

        System.out.println("Zainicjalizowano użytkowników:");
        System.out.println("Admin: login=admin, hasło=admin");
        System.out.println("User: login=user, hasło=user");
    }

    private void initializeProducts() {
        // Przykładowe produkty
        Product product1 = new Product();
        product1.setName("Laptop Dell");
        product1.setPrice(new BigDecimal("2999.99"));
        productDao.save(product1);

        Product product2 = new Product();
        product2.setName("Smartfon Samsung");
        product2.setPrice(new BigDecimal("1299.50"));
        productDao.save(product2);

        Product product3 = new Product();
        product3.setName("Słuchawki Sony");
        product3.setPrice(new BigDecimal("299.99"));
        productDao.save(product3);

        Product product4 = new Product();
        product4.setName("Klawiatura mechaniczna");
        product4.setPrice(new BigDecimal("449.00"));
        productDao.save(product4);

        Product product5 = new Product();
        product5.setName("Monitor 27\" 4K");
        product5.setPrice(new BigDecimal("1599.99"));
        productDao.save(product5);

        System.out.println("Zainicjalizowano " + 5 + " przykładowych produktów");
    }
}