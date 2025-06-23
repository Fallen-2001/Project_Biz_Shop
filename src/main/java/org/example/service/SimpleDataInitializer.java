package org.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.dao.UserDaoInterface;
import org.example.model.User;
import org.example.model.Role;  // Używamy Role, nie UserRole
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class SimpleDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDataInitializer.class);

    @Inject
    private UserDaoInterface userDao;  // Używamy UserDao, nie UserDaoInterface

    private volatile boolean initialized = false;

    @PostConstruct
    public void init() {
        logger.info("=== SimpleDataInitializer @PostConstruct called ===");
        // Nie inicjalizujemy od razu, bo EntityManager może nie być gotowy
    }

    public synchronized void ensureDataInitialized() {
        if (!initialized) {
            logger.info("=== Starting data initialization ===");
            try {
                initializeData();
                initialized = true;
                logger.info("=== Data initialization completed successfully ===");
            } catch (Exception e) {
                logger.error("Failed to initialize data", e);
                throw new RuntimeException("Data initialization failed", e);
            }
        }
    }

    private void initializeData() {
        try {
            // Sprawdź czy admin już istnieje
            Optional<User> existingAdmin = userDao.findByUsername("admin");
            if (existingAdmin.isPresent()) {
                logger.info("Admin user already exists, skipping creation");
                return;
            }

            logger.info("Creating admin user...");

            // Stwórz użytkownika admin
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword("admin123"); // W prawdziwej aplikacji użyj BCrypt!
            adminUser.setEmail("admin@example.com");
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setRole(Role.ADMIN);  // Używamy Role.ADMIN
            adminUser.setAddress("123 Admin Street");
            adminUser.setPhone("123-456-7890");

            userDao.save(adminUser);
            logger.info("Admin user created successfully with username: admin, password: admin123");

        } catch (Exception e) {
            logger.error("Error during data initialization", e);
            throw e;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}