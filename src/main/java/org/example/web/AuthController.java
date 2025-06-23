package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.example.service.SimpleDataInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@RequestScoped
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private String username;
    private String password;

    @Inject
    private AuthServiceInterface authService;

    @Inject
    private SimpleDataInitializer dataInitializer;

    public String login() {
        logger.debug("Attempting login for username: {}", username);

        // Upewnij się, że dane są zainicjalizowane
        try {
            dataInitializer.ensureDataInitialized();
        } catch (Exception e) {
            logger.error("Failed to initialize data", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "System initialization error", "Please contact administrator"));
            return null;
        }

        try {
            User user = authService.login(username, password);
            if (user != null) {
                logger.info("Successful login for user: {}", username);

                if (user.getRole() == Role.ADMIN) {
                    return "/admin/products.xhtml?faces-redirect=true";
                } else {
                    return "/user/products.xhtml?faces-redirect=true";
                }
            } else {
                logger.warn("Failed login attempt for username: {}", username);
                addErrorMessage("Nieprawidłowa nazwa użytkownika lub hasło");
                return "/login.xhtml?error=true&faces-redirect=true";
            }
        } catch (Exception e) {
            logger.error("Error during login for username: {}", username, e);
            addErrorMessage("Wystąpił błąd podczas logowania");
            return "/login.xhtml?error=true&faces-redirect=true";
        }
    }

    public String logout() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            logger.info("User logged out: {}", currentUser.getUsername());
        }

        authService.logout();
        addInfoMessage("Zostałeś pomyślnie wylogowany");
        return "/login.xhtml?faces-redirect=true";
    }

    // Gettery i settery
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Błąd", message));
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", message));
    }
}