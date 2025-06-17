package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@RequestScoped
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Inject
    private AuthServiceInterface authService;

    private User newUser = new User();
    private String confirmPassword;

    public String register() {
        logger.debug("Attempting to register user: {}", newUser.getUsername());

        try {
            // Walidacja
            if (!validateRegistration()) {
                return null;
            }

            // Ustaw domyślną rolę
            newUser.setRole(Role.USER);

            // Zarejestruj użytkownika
            User registeredUser = authService.register(newUser);

            // Wyczyść formularz
            newUser = new User();
            confirmPassword = null;

            addInfoMessage("Rejestracja przebiegła pomyślnie! Możesz się teraz zalogować.");
            logger.info("User registered successfully: {}", registeredUser.getUsername());

            return "/login.xhtml?faces-redirect=true";

        } catch (Exception e) {
            logger.error("Error during registration for user: {}", newUser.getUsername(), e);
            addErrorMessage("Błąd podczas rejestracji: " + e.getMessage());
            return null;
        }
    }

    public void checkUsernameAvailability() {
        if (newUser.getUsername() != null && !newUser.getUsername().trim().isEmpty()) {
            boolean available = authService.isUsernameAvailable(newUser.getUsername().trim());
            if (!available) {
                addErrorMessage("Nazwa użytkownika jest już zajęta");
            }
        }
    }

    public void checkEmailAvailability() {
        if (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) {
            boolean available = authService.isEmailAvailable(newUser.getEmail().trim());
            if (!available) {
                addErrorMessage("Adres email jest już używany");
            }
        }
    }

    public String updateProfile() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            addErrorMessage("Musisz być zalogowany");
            return "/login.xhtml?faces-redirect=true";
        }

        try {
            // Tutaj można dodać logikę aktualizacji profilu
            // Na razie tylko informacja
            addInfoMessage("Profil zostanie zaktualizowany (funkcja w rozwoju)");
            logger.info("Profile update requested for user: {}", currentUser.getUsername());

            return null;

        } catch (Exception e) {
            logger.error("Error updating profile for user: {}", currentUser.getUsername(), e);
            addErrorMessage("Błąd podczas aktualizacji profilu: " + e.getMessage());
            return null;
        }
    }

    private boolean validateRegistration() {
        boolean valid = true;

        // Sprawdź czy wszystkie wymagane pola są wypełnione
        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            addErrorMessage("Nazwa użytkownika jest wymagana");
            valid = false;
        } else if (newUser.getUsername().trim().length() < 3) {
            addErrorMessage("Nazwa użytkownika musi mieć co najmniej 3 znaki");
            valid = false;
        } else if (!authService.isUsernameAvailable(newUser.getUsername().trim())) {
            addErrorMessage("Nazwa użytkownika jest już zajęta");
            valid = false;
        }

        if (newUser.getPassword() == null || newUser.getPassword().isEmpty()) {
            addErrorMessage("Hasło jest wymagane");
            valid = false;
        } else if (newUser.getPassword().length() < 6) {
            addErrorMessage("Hasło musi mieć co najmniej 6 znaków");
            valid = false;
        }

        if (confirmPassword == null || !confirmPassword.equals(newUser.getPassword())) {
            addErrorMessage("Hasła nie są identyczne");
            valid = false;
        }

        if (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) {
            if (!newUser.getEmail().contains("@") || !newUser.getEmail().contains(".")) {
                addErrorMessage("Nieprawidłowy format adresu email");
                valid = false;
            } else if (!authService.isEmailAvailable(newUser.getEmail().trim())) {
                addErrorMessage("Adres email jest już używany");
                valid = false;
            }
        }

        return valid;
    }

    // Gettery i settery
    public User getNewUser() {
        return newUser;
    }

    public void setNewUser(User newUser) {
        this.newUser = newUser;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return authService.isLoggedIn();
    }

    public boolean isAdmin() {
        return authService.isAdmin();
    }

    public boolean isUser() {
        return authService.isUser();
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