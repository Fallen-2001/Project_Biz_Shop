package org.example.service;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import org.example.dao.UserDaoInterface;
import org.example.model.Role;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Serializable;
import java.util.Optional;

@Named
@SessionScoped
public class AuthServiceImpl implements AuthServiceInterface, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Inject
    private UserDaoInterface userDao;

    // Utwórz encoder jako pole instancji z leniwą inicjalizacją
    private transient BCryptPasswordEncoder passwordEncoder;
    private User currentUser;

    // Leniwa inicjalizacja encodera
    private BCryptPasswordEncoder getPasswordEncoder() {
        if (passwordEncoder == null) {
            passwordEncoder = new BCryptPasswordEncoder();
        }
        return passwordEncoder;
    }

    @Override
    public User login(String username, String password) {
        logger.debug("Attempting login for username: {}", username);

        if (username == null || password == null) {
            logger.warn("Login attempt with null credentials");
            return null;
        }

        try {
            Optional<User> userOpt = userDao.findByUsername(username.trim());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (getPasswordEncoder().matches(password, user.getPassword())) {
                    logger.info("Successful login for user: {}", username);
                    this.currentUser = user;
                    return user;
                } else {
                    logger.warn("Invalid password for user: {}", username);
                }
            } else {
                logger.warn("User not found: {}", username);
            }
        } catch (Exception e) {
            logger.error("Error during login for user: {}", username, e);
        }

        return null;
    }

    @Override
    @Transactional
    public User register(User user) throws Exception {
        logger.debug("Attempting to register user: {}", user.getUsername());

        try {
            validateUser(user);

            if (userDao.existsByUsername(user.getUsername())) {
                throw new Exception("Nazwa użytkownika już istnieje");
            }

            if (user.getEmail() != null && !user.getEmail().trim().isEmpty() && userDao.existsByEmail(user.getEmail())) {
                throw new Exception("Email już jest używany");
            }

            // Hashowanie hasła
            String hashedPassword = getPasswordEncoder().encode(user.getPassword());
            user.setPassword(hashedPassword);

            // Domyślna rola
            if (user.getRole() == null) {
                user.setRole(Role.USER);
            }

            userDao.save(user);
            logger.info("User registered successfully: {}", user.getUsername());
            return user;

        } catch (Exception e) {
            logger.error("Error registering user: {}", user.getUsername(), e);
            // Przekaż oryginalny błąd jeśli to błąd walidacji
            if (e.getMessage().contains("już istnieje") ||
                    e.getMessage().contains("już jest używany") ||
                    e.getMessage().contains("musi mieć") ||
                    e.getMessage().contains("Nieprawidłowy")) {
                throw e;
            }
            throw new Exception("Błąd podczas rejestracji użytkownika: " + e.getMessage());
        }
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        try {
            return !userDao.existsByUsername(username.trim());
        } catch (Exception e) {
            logger.error("Error checking username availability: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true; // email nie jest wymagany
        }
        try {
            return !userDao.existsByEmail(email.trim());
        } catch (Exception e) {
            logger.error("Error checking email availability: {}", email, e);
            return false;
        }
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.debug("Current user set to: {}", user != null ? user.getUsername() : "null");
    }

    @Override
    public void logout() {
        if (currentUser != null) {
            logger.info("User logged out: {}", currentUser.getUsername());
            currentUser = null;
        }
    }

    @Override
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    @Override
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    @Override
    public boolean isUser() {
        return currentUser != null && currentUser.getRole() == Role.USER;
    }

    private void validateUser(User user) throws Exception {
        if (user == null) {
            throw new Exception("Użytkownik nie może być null");
        }

        if (user.getUsername() == null || user.getUsername().trim().length() < 3) {
            throw new Exception("Nazwa użytkownika musi mieć co najmniej 3 znaki");
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new Exception("Hasło musi mieć co najmniej 6 znaków");
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty() && !user.getEmail().contains("@")) {
            throw new Exception("Nieprawidłowy format email");
        }
    }
}