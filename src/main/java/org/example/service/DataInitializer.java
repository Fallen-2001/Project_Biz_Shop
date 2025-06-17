package org.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
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

    @PostConstruct
    @Transactional
    public void initializeData() {
        logger.info("Starting data initialization...");

        try {
            initializeUsers();
            initializeProducts();
            logger.info("Data initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during data initialization", e);
        }
    }

    private void initializeUsers() {
        logger.info("Initializing users...");

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
    }

    private void initializeProducts() {
        logger.info("Initializing products...");

        // Sprawdź czy produkty już istnieją
        if (productDao.findAll().isEmpty()) {
            createSampleProducts();
            logger.info("Sample products created");
        } else {
            logger.info("Products already exist, skipping creation");
        }
    }

    private void createSampleProducts() {
        // Elektronika
        createProduct("Laptop Dell Inspiron 15", new BigDecimal("2999.99"),
                "Wydajny laptop z procesorem Intel Core i5, 8GB RAM, 512GB SSD",
                "Elektronika", 5,
                "https://example.com/images/laptop-dell.jpg");

        createProduct("Smartfon Samsung Galaxy S23", new BigDecimal("3299.00"),
                "Najnowszy smartfon Samsung z aparatem 200MP i ekranem Dynamic AMOLED",
                "Elektronika", 8,
                "https://example.com/images/samsung-s23.jpg");

        createProduct("Słuchawki Sony WH-1000XM5", new BigDecimal("1299.99"),
                "Bezprzewodowe słuchawki z aktywną redukcją szumów",
                "Elektronika", 12,
                "https://example.com/images/sony-headphones.jpg");

        createProduct("Monitor Dell 27\" 4K", new BigDecimal("1599.99"),
                "Monitor 4K UHD 27 cali z technologią IPS i portem USB-C",
                "Elektronika", 6,
                "https://example.com/images/monitor-dell.jpg");

        createProduct("Klawiatura mechaniczna Logitech", new BigDecimal("449.00"),
                "Klawiatura mechaniczna RGB z przełącznikami Cherry MX",
                "Elektronika", 15,
                "https://example.com/images/keyboard-logitech.jpg");

        // Dom i ogród
        createProduct("Odkurzacz bezprzewodowy Dyson", new BigDecimal("1899.00"),
                "Potężny odkurzacz bezprzewodowy z technologią cyklonową",
                "Dom i ogród", 4,
                "https://example.com/images/dyson-vacuum.jpg");

        createProduct("Ekspres do kawy DeLonghi", new BigDecimal("899.99"),
                "Automatyczny ekspres do kawy z młynkiem i spieniaczem",
                "Dom i ogród", 7,
                "https://example.com/images/delonghi-coffee.jpg");

        createProduct("Zestaw garnków Tefal", new BigDecimal("399.99"),
                "5-częściowy zestaw garnków z powłoką nieprzywierającą",
                "Dom i ogród", 10,
                "https://example.com/images/tefal-pots.jpg");

        // Odzież
        createProduct("Kurtka zimowa North Face", new BigDecimal("799.00"),
                "Ciepła kurtka zimowa z membraną Gore-Tex",
                "Odzież", 20,
                "https://example.com/images/northface-jacket.jpg");

        createProduct("Jeansy Levi's 501", new BigDecimal("299.99"),
                "Klasyczne jeansy Levi's w kolorze indygo",
                "Odzież", 25,
                "https://example.com/images/levis-jeans.jpg");

        createProduct("Buty sportowe Nike Air Max", new BigDecimal("549.00"),
                "Wygodne buty sportowe z technologią Air Max",
                "Odzież", 18,
                "https://example.com/images/nike-shoes.jpg");

        // Sport
        createProduct("Rower górski Trek", new BigDecimal("3999.00"),
                "Profesjonalny rower górski z amortyzacją i 21 biegami",
                "Sport", 3,
                "https://example.com/images/trek-bike.jpg");

        createProduct("Mata do jogi Manduka", new BigDecimal("299.00"),
                "Profesjonalna mata do jogi z naturalnego kauczuku",
                "Sport", 15,
                "https://example.com/images/yoga-mat.jpg");

        createProduct("Hantle regulowane 20kg", new BigDecimal("599.99"),
                "Zestaw regulowanych hantli od 5 do 20kg każdy",
                "Sport", 8,
                "https://example.com/images/dumbbells.jpg");

        // Książki
        createProduct("\"Clean Code\" - Robert Martin", new BigDecimal("89.99"),
                "Klasyczna książka o pisaniu czystego kodu",
                "Książki", 30,
                "https://example.com/images/clean-code.jpg");

        createProduct("\"Sapiens\" - Yuval Harari", new BigDecimal("49.99"),
                "Bestseller o historii ludzkości",
                "Książki", 25,
                "https://example.com/images/sapiens.jpg");

        // Zabawki
        createProduct("LEGO Creator Expert", new BigDecimal("899.00"),
                "Duży zestaw LEGO dla zaawansowanych budowniczych",
                "Zabawki", 12,
                "https://example.com/images/lego-creator.jpg");

        createProduct("Puzzle 1000 elementów", new BigDecimal("39.99"),
                "Piękne puzzle krajobrazowe 1000 elementów",
                "Zabawki", 50,
                "https://example.com/images/puzzle-1000.jpg");
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
        logger.debug("Created product: {}", name);
    }
}