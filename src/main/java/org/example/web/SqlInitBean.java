package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

@Named
@RequestScoped
public class SqlInitBean {

    private static final Logger logger = LoggerFactory.getLogger(SqlInitBean.class);

    @PersistenceContext(unitName = "shopPU")
    private EntityManager em;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void initializeDataWithSql() {
        logger.info("SQL-based data initialization triggered");

        try {
            // Najpierw sprawdź czy dane już istnieją
            Long userCount = (Long) em.createNativeQuery("SELECT COUNT(*) FROM users").getSingleResult();
            if (userCount > 0) {
                addInfoMessage("Dane już istnieją w bazie!");
                return;
            }

            // Czyść bazy danych
            clearDatabase();

            // Dodaj użytkowników za pomocą natywnych zapytań SQL
            initializeUsersWithSql();

            // Dodaj produkty
            initializeProductsWithSql();

            addInfoMessage("Dane zostały zainicjalizowane pomyślnie za pomocą SQL!");
            logger.info("SQL-based data initialization completed successfully");

        } catch (Exception e) {
            logger.error("Error during SQL-based data initialization", e);
            addErrorMessage("Błąd podczas inicjalizacji danych: " + e.getMessage());
        }
    }

    private void clearDatabase() {
        try {
            // Usuń dane w odpowiedniej kolejności (respektując foreign keys)
            em.createNativeQuery("DELETE FROM order_items").executeUpdate();
            em.createNativeQuery("DELETE FROM orders").executeUpdate();
            em.createNativeQuery("DELETE FROM cart_items").executeUpdate();
            em.createNativeQuery("DELETE FROM products").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();

            // Reset sekwencji
            try {
                em.createNativeQuery("DELETE FROM SEQUENCE").executeUpdate();
            } catch (Exception e) {
                logger.debug("Sequence table doesn't exist yet: {}", e.getMessage());
            }

            logger.info("Database cleared successfully");
        } catch (Exception e) {
            logger.warn("Error clearing database: {}", e.getMessage());
        }
    }

    private void initializeUsersWithSql() {
        logger.info("SQL: Initializing users...");

        // Admin
        String adminPasswordHash = passwordEncoder.encode("admin");
        em.createNativeQuery(
                        "INSERT INTO users (id, username, password, role, email, first_name, last_name, address, phone) " +
                                "VALUES (1, 'admin', ?, 'ADMIN', 'admin@shop.com', 'Administrator', 'Systemu', " +
                                "'ul. Administracyjna 1, 00-001 Warszawa', '+48 123 456 789')")
                .setParameter(1, adminPasswordHash)
                .executeUpdate();

        // User
        String userPasswordHash = passwordEncoder.encode("user");
        em.createNativeQuery(
                        "INSERT INTO users (id, username, password, role, email, first_name, last_name, address, phone) " +
                                "VALUES (2, 'user', ?, 'USER', 'user@example.com', 'Jan', 'Kowalski', " +
                                "'ul. Testowa 15/2, 02-123 Warszawa', '+48 987 654 321')")
                .setParameter(1, userPasswordHash)
                .executeUpdate();

        // Test user
        String testUserPasswordHash = passwordEncoder.encode("test123");
        em.createNativeQuery(
                        "INSERT INTO users (id, username, password, role, email, first_name, last_name, address, phone) " +
                                "VALUES (3, 'testuser', ?, 'USER', 'test@example.com', 'Anna', 'Nowak', " +
                                "'ul. Przykładowa 7, 03-456 Kraków', '+48 555 444 333')")
                .setParameter(1, testUserPasswordHash)
                .executeUpdate();

        logger.info("SQL: Users created successfully");
    }

    private void initializeProductsWithSql() {
        logger.info("SQL: Initializing products...");

        // Elektronika
        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (1, 'Laptop Dell Inspiron 15', 2999.99, " +
                                "'Wydajny laptop z procesorem Intel Core i5, 8GB RAM, 512GB SSD', " +
                                "'Elektronika', 5, 'https://example.com/images/laptop-dell.jpg', true)")
                .executeUpdate();

        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (2, 'Smartfon Samsung Galaxy S23', 3299.00, " +
                                "'Najnowszy smartfon Samsung z aparatem 200MP', " +
                                "'Elektronika', 8, 'https://example.com/images/samsung-s23.jpg', true)")
                .executeUpdate();

        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (3, 'Słuchawki Sony WH-1000XM5', 1299.99, " +
                                "'Bezprzewodowe słuchawki z aktywną redukcją szumów', " +
                                "'Elektronika', 12, 'https://example.com/images/sony-headphones.jpg', true)")
                .executeUpdate();

        // Dom i ogród
        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (4, 'Odkurzacz bezprzewodowy Dyson', 1899.00, " +
                                "'Potężny odkurzacz bezprzewodowy z technologią cyklonową', " +
                                "'Dom i ogród', 4, 'https://example.com/images/dyson-vacuum.jpg', true)")
                .executeUpdate();

        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (5, 'Ekspres do kawy DeLonghi', 899.99, " +
                                "'Automatyczny ekspres do kawy z młynkiem', " +
                                "'Dom i ogród', 7, 'https://example.com/images/delonghi-coffee.jpg', true)")
                .executeUpdate();

        // Sport
        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (6, 'Rower górski Trek', 3999.00, " +
                                "'Profesjonalny rower górski z amortyzacją', " +
                                "'Sport', 3, 'https://example.com/images/trek-bike.jpg', true)")
                .executeUpdate();

        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (7, 'Mata do jogi Manduka', 299.00, " +
                                "'Profesjonalna mata do jogi z naturalnego kauczuku', " +
                                "'Sport', 15, 'https://example.com/images/yoga-mat.jpg', true)")
                .executeUpdate();

        // Książki
        em.createNativeQuery(
                        "INSERT INTO products (id, name, price, description, category, stock_quantity, image_url, active) " +
                                "VALUES (8, '\"Clean Code\" - Robert Martin', 89.99, " +
                                "'Klasyczna książka o pisaniu czystego kodu', " +
                                "'Książki', 30, 'https://example.com/images/clean-code.jpg', true)")
                .executeUpdate();

        logger.info("SQL: Products created successfully");
    }

    // Metody pomocnicze
    public boolean isDataEmpty() {
        try {
            Long userCount = (Long) em.createNativeQuery("SELECT COUNT(*) FROM users").getSingleResult();
            Long productCount = (Long) em.createNativeQuery("SELECT COUNT(*) FROM products").getSingleResult();
            return userCount == 0 || productCount == 0;
        } catch (Exception e) {
            return true;
        }
    }

    public long getUserCount() {
        try {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM users").getSingleResult()).longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getProductCount() {
        try {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM products").getSingleResult()).longValue();
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