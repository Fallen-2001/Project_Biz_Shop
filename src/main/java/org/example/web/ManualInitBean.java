package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import org.example.dao.UserDaoInterface;
import org.example.dao.ProductDaoInterface;
import org.example.model.User;
import org.example.model.Product;
import org.example.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

@Named
@RequestScoped
public class ManualInitBean {

    private static final Logger logger = LoggerFactory.getLogger(ManualInitBean.class);

    @Inject
    private UserDaoInterface userDao;

    @Inject
    private ProductDaoInterface productDao;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void initializeData() {
        logger.info("Manual data initialization triggered");

        try {
            initializeUsers();
            initializeProducts();

            addInfoMessage("Dane zostały zainicjalizowane pomyślnie!");
            logger.info("Manual data initialization completed successfully");

        } catch (Exception e) {
            logger.error("Error during manual data initialization", e);
            addErrorMessage("Błąd podczas inicjalizacji danych: " + e.getMessage());
        }
    }

    private void initializeUsers() {
        logger.info("Manual initializing users...");

        // Admin
        if (!userDao.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRole(Role.ADMIN);
            admin.setEmail("admin@shop.com");
            admin.setFirstName("Administrator");
            admin.setLastName("Systemu");
            admin.setAddress("ul. Administracyjna 1, 00-001 Warszawa");
            admin.setPhone("+48 123 456 789");

            userDao.save(admin);
            logger.info("Manual: Admin user created");
        }

        // User
        if (!userDao.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user"));
            user.setRole(Role.USER);
            user.setEmail("user@example.com");
            user.setFirstName("Jan");
            user.setLastName("Kowalski");
            user.setAddress("ul. Testowa 15/2, 02-123 Warszawa");
            user.setPhone("+48 987 654 321");

            userDao.save(user);
            logger.info("Manual: Regular user created");
        }

        // Test user
        if (!userDao.existsByUsername("testuser")) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPassword(passwordEncoder.encode("test123"));
            testUser.setRole(Role.USER);
            testUser.setEmail("test@example.com");
            testUser.setFirstName("Anna");
            testUser.setLastName("Nowak");
            testUser.setAddress("ul. Przykładowa 7, 03-456 Kraków");
            testUser.setPhone("+48 555 444 333");

            userDao.save(testUser);
            logger.info("Manual: Test user created");
        }
    }

    private void initializeProducts() {
        logger.info("Manual initializing products...");

        if (productDao.findAll().isEmpty()) {
            // Elektronika
            createProduct("Laptop Dell Inspiron 15", new BigDecimal("2999.99"),
                    "Wydajny laptop z procesorem Intel Core i5, 8GB RAM, 512GB SSD",
                    "Elektronika", 5, "https://example.com/images/laptop-dell.jpg");

            createProduct("Smartfon Samsung Galaxy S23", new BigDecimal("3299.00"),
                    "Najnowszy smartfon Samsung z aparatem 200MP",
                    "Elektronika", 8, "https://example.com/images/samsung-s23.jpg");

            createProduct("Słuchawki Sony WH-1000XM5", new BigDecimal("1299.99"),
                    "Bezprzewodowe słuchawki z aktywną redukcją szumów",
                    "Elektronika", 12, "https://example.com/images/sony-headphones.jpg");

            // Dom i ogród
            createProduct("Odkurzacz bezprzewodowy Dyson", new BigDecimal("1899.00"),
                    "Potężny odkurzacz bezprzewodowy z technologią cyklonową",
                    "Dom i ogród", 4, "https://example.com/images/dyson-vacuum.jpg");

            createProduct("Ekspres do kawy DeLonghi", new BigDecimal("899.99"),
                    "Automatyczny ekspres do kawy z młynkiem",
                    "Dom i ogród", 7, "https://example.com/images/delonghi-coffee.jpg");

            // Sport
            createProduct("Rower górski Trek", new BigDecimal("3999.00"),
                    "Profesjonalny rower górski z amortyzacją",
                    "Sport", 3, "https://example.com/images/trek-bike.jpg");

            createProduct("Mata do jogi Manduka", new BigDecimal("299.00"),
                    "Profesjonalna mata do jogi z naturalnego kauczuku",
                    "Sport", 15, "https://example.com/images/yoga-mat.jpg");

            // Książki
            createProduct("\"Clean Code\" - Robert Martin", new BigDecimal("89.99"),
                    "Klasyczna książka o pisaniu czystego kodu",
                    "Książki", 30, "https://example.com/images/clean-code.jpg");

            logger.info("Manual: Sample products created");
        }
    }

    private void createProduct(String name, BigDecimal price, String description,
                               String category, Integer stock, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);
        product.setCategory(category);
        product.setStockQuantity(stock);
        product.setImageUrl(imageUrl);
        product.setActive(true);

        productDao.save(product);
    }

    // Metody pomocnicze
    public boolean isDataEmpty() {
        try {
            return !userDao.existsByUsername("admin") || productDao.findAll().isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    public long getUserCount() {
        try {
            return userDao.findAll().size();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getProductCount() {
        try {
            return productDao.findAll().size();
        } catch (Exception e) {
            return 0;
        }
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
