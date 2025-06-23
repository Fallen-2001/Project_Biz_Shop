package org.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
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

@ApplicationScoped
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Inject
    private UserDaoInterface userDao;

    @Inject
    private ProductDaoInterface productDao;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Zmiana z @PostConstruct na obserwowanie eventu inicjalizacji aplikacji
    @Transactional
    public void initializeData(@Observes @Initialized(ApplicationScoped.class) Object init) {
        logger.info("Starting data initialization...");

        try {
            // Dodaj małe opóźnienie aby upewnić się, że wszystko jest gotowe
            Thread.sleep(1000);

            initializeUsers();
            initializeProducts();
            logger.info("Data initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during data initialization", e);
        }
    }

    private void initializeUsers() {
        logger.info("Initializing users...");

        try {
            // Sprawdź czy admin już istnieje
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
                logger.info("Admin user created: username=admin, password=admin");
            } else {
                logger.info("Admin user already exists, skipping creation");
            }

            // Sprawdź czy zwykły użytkownik już istnieje
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
                logger.info("Regular user created: username=user, password=user");
            } else {
                logger.info("Regular user already exists, skipping creation");
            }

            // Dodaj dodatkowego użytkownika testowego
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
                logger.info("Test user created: username=testuser, password=test123");
            }
        } catch (Exception e) {
            logger.error("Error initializing users", e);
        }
    }

    private void initializeProducts() {
        logger.info("Initializing products...");

        try {
            // Sprawdź czy produkty już istnieją
            if (productDao.findAll().isEmpty()) {
                createSampleProducts();
                logger.info("Sample products created");
            } else {
                logger.info("Products already exist, skipping creation");
            }
        } catch (Exception e) {
            logger.error("Error initializing products", e);
        }
    }

    private void createSampleProducts() {
        try {
            // Elektronika - produkty z dobrą dostępnością
            createProduct("Laptop Dell Inspiron 15", new BigDecimal("2999.99"),
                    "Wydajny laptop z procesorem Intel Core i5, 8GB RAM, 512GB SSD. Idealny do pracy i rozrywki.",
                    "Elektronika", 15,  // Zwiększona ilość
                    "https://example.com/images/laptop-dell.jpg");

            createProduct("Smartfon Samsung Galaxy S23", new BigDecimal("3299.00"),
                    "Najnowszy smartfon Samsung z aparatem 200MP i ekranem Dynamic AMOLED. Wodoszczelny i szybki.",
                    "Elektronika", 25,  // Zwiększona ilość
                    "https://example.com/images/samsung-s23.jpg");

            createProduct("Słuchawki Sony WH-1000XM5", new BigDecimal("1299.99"),
                    "Bezprzewodowe słuchawki z aktywną redukcją szumów. Komfortowe noszenie przez cały dzień.",
                    "Elektronika", 30,  // Zwiększona ilość
                    "https://example.com/images/sony-headphones.jpg");

            createProduct("Monitor Dell 27\" 4K", new BigDecimal("1599.99"),
                    "Monitor 4K UHD 27 cali z technologią IPS i portem USB-C. Perfekcyjny dla profesjonalistów.",
                    "Elektronika", 12,
                    "https://example.com/images/monitor-dell.jpg");

            createProduct("Klawiatura mechaniczna Logitech", new BigDecimal("499.99"),
                    "Mechaniczna klawiatura gamingowa z podświetleniem RGB i przełącznikami Cherry MX.",
                    "Elektronika", 40,
                    "https://example.com/images/keyboard-logitech.jpg");

            // Dom i ogród
            createProduct("Odkurzacz bezprzewodowy Dyson", new BigDecimal("1899.00"),
                    "Potężny odkurzacz bezprzewodowy z technologią cyklonową. Długi czas pracy na jednym ładowaniu.",
                    "Dom i ogród", 8,
                    "https://example.com/images/dyson-vacuum.jpg");

            createProduct("Ekspres do kawy DeLonghi", new BigDecimal("899.99"),
                    "Automatyczny ekspres do kawy z młynkiem i spieniaczem. Przygotuje idealną kawę jednym naciśnięciem.",
                    "Dom i ogród", 20,  // Zwiększona ilość
                    "https://example.com/images/delonghi-coffee.jpg");

            createProduct("Robot kuchenny KitchenAid", new BigDecimal("1599.00"),
                    "Profesjonalny robot kuchenny z misą 4.8L i akcesoriami. Niezawodny pomocnik w kuchni.",
                    "Dom i ogród", 10,
                    "https://example.com/images/kitchenaid-mixer.jpg");

            // Odzież
            createProduct("Kurtka zimowa North Face", new BigDecimal("799.00"),
                    "Ciepła kurtka zimowa z membraną Gore-Tex. Wiatroszczelna i wodoodporna.",
                    "Odzież", 35,  // Zwiększona ilość
                    "https://example.com/images/northface-jacket.jpg");

            createProduct("Jeansy Levi's 501", new BigDecimal("299.99"),
                    "Klasyczne jeansy Levi's w kolorze indygo. Kultowy krój straight fit.",
                    "Odzież", 50,  // Zwiększona ilość
                    "https://example.com/images/levis-jeans.jpg");

            createProduct("Bluza z kapturem Nike", new BigDecimal("249.99"),
                    "Komfortowa bluza z kapturem z bawełny organicznej. Dostępna w różnych kolorach.",
                    "Odzież", 60,
                    "https://example.com/images/nike-hoodie.jpg");

            // Sport
            createProduct("Rower górski Trek", new BigDecimal("3999.00"),
                    "Profesjonalny rower górski z amortyzacją i 21 biegami. Aluminiowa rama i komponenty Shimano.",
                    "Sport", 8,  // Ograniczona dostępność - drogi produkt
                    "https://example.com/images/trek-bike.jpg");

            createProduct("Mata do jogi Manduka", new BigDecimal("299.00"),
                    "Profesjonalna mata do jogi z naturalnego kauczuku. Antypoślizgowa powierzchnia.",
                    "Sport", 45,  // Zwiększona ilość
                    "https://example.com/images/yoga-mat.jpg");

            createProduct("Hantle regulowane 20kg", new BigDecimal("599.99"),
                    "Para hantli regulowanych od 2.5kg do 20kg. Kompaktowe rozwiązanie do domowej siłowni.",
                    "Sport", 25,
                    "https://example.com/images/adjustable-dumbbells.jpg");

            createProduct("Buty do biegania Adidas", new BigDecimal("449.99"),
                    "Profesjonalne buty do biegania z technologią Boost. Maksymalny komfort i zwrot energii.",
                    "Sport", 40,
                    "https://example.com/images/adidas-running.jpg");

            // Książki
            createProduct("\"Clean Code\" - Robert Martin", new BigDecimal("89.99"),
                    "Klasyczna książka o pisaniu czystego kodu. Must-have dla każdego programisty.",
                    "Książki", 75,  // Duża dostępność
                    "https://example.com/images/clean-code.jpg");

            createProduct("\"Sapiens\" - Yuval Harari", new BigDecimal("49.99"),
                    "Bestseller o historii ludzkości. Fascynująca podróż przez ewolucję człowieka.",
                    "Książki", 100, // Bardzo duża dostępność
                    "https://example.com/images/sapiens.jpg");

            createProduct("\"Design Patterns\" - Gang of Four", new BigDecimal("119.99"),
                    "Fundamentalna książka o wzorcach projektowych w programowaniu obiektowym.",
                    "Książki", 30,
                    "https://example.com/images/design-patterns.jpg");

            // Zabawki
            createProduct("LEGO Creator Expert", new BigDecimal("899.99"),
                    "Zaawansowany zestaw LEGO dla dorosłych kolekcjonerów. Ponad 2000 elementów.",
                    "Zabawki", 15,
                    "https://example.com/images/lego-creator.jpg");

            createProduct("Puzzle 1000 elementów", new BigDecimal("59.99"),
                    "Piękne puzzle krajobrazowe 1000 elementów. Idealny sposób na relaks.",
                    "Zabawki", 80,
                    "https://example.com/images/puzzle-landscape.jpg");

            // Produkty testowe z różną dostępnością
            createProduct("Produkt testowy - duża dostępność", new BigDecimal("19.99"),
                    "Produkt testowy z dużą dostępnością w magazynie.",
                    "Różne", 999,
                    "https://example.com/images/test-high-stock.jpg");

            createProduct("Produkt testowy - mała dostępność", new BigDecimal("99.99"),
                    "Produkt testowy z małą dostępnością w magazynie.",
                    "Różne", 3,
                    "https://example.com/images/test-low-stock.jpg");

            createProduct("Produkt testowy - średnia dostępność", new BigDecimal("49.99"),
                    "Produkt testowy ze średnią dostępnością w magazynie.",
                    "Różne", 15,
                    "https://example.com/images/test-medium-stock.jpg");

        } catch (Exception e) {
            logger.error("Error creating sample products", e);
        }
    }

    private void createProduct(String name, BigDecimal price, String description,
                               String category, Integer stock, String imageUrl) {
        try {
            Product product = new Product();
            product.setName(name);
            product.setPrice(price);
            product.setDescription(description);
            product.setCategory(category);
            product.setStockQuantity(stock);  // Teraz zawsze ustawiamy konkretną wartość
            product.setImageUrl(imageUrl);
            product.setActive(true);

            productDao.save(product);
            logger.debug("Created product: {} with stock: {}", name, stock);
        } catch (Exception e) {
            logger.error("Error creating product: {}", name, e);
        }
    }
}