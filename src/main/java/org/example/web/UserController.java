package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.dao.UserDaoInterface;
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

    @Inject
    private UserDaoInterface userDao;

    private User newUser = new User();
    private String confirmPassword;

    public String register() {
        logger.debug("Attempting to register user: {}", newUser.getUsername());

        try {
            // Walidacja po stronie serwera
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

            return "/login.xhtml?registered=true&faces-redirect=true";

        } catch (Exception e) {
            logger.error("Error during registration for user: {}", newUser.getUsername(), e);
            addErrorMessage("Błąd podczas rejestracji: " + e.getMessage());
            return null;
        }
    }

    public String updateProfile() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            addErrorMessage("Musisz być zalogowany");
            return "/login.xhtml?faces-redirect=true";
        }

        try {
            // Walidacja danych
            if (!validateProfileUpdate(currentUser)) {
                return null;
            }

            // Znormalizuj dane
            if (currentUser.getFirstName() != null) {
                currentUser.setFirstName(currentUser.getFirstName().trim());
            }
            if (currentUser.getLastName() != null) {
                currentUser.setLastName(currentUser.getLastName().trim());
            }
            if (currentUser.getEmail() != null) {
                currentUser.setEmail(currentUser.getEmail().trim());
            }
            if (currentUser.getPhone() != null) {
                currentUser.setPhone(currentUser.getPhone().trim());
            }
            if (currentUser.getAddress() != null) {
                currentUser.setAddress(currentUser.getAddress().trim());
            }

            // Aktualizuj użytkownika w bazie danych
            userDao.update(currentUser);

            // Aktualizuj sesję
            authService.setCurrentUser(currentUser);

            addInfoMessage("Profil został zaktualizowany pomyślnie!");
            logger.info("Profile updated successfully for user: {}", currentUser.getUsername());

            return null; // Pozostań na tej samej stronie

        } catch (Exception e) {
            logger.error("Error updating profile for user: {}", currentUser.getUsername(), e);
            addErrorMessage("Błąd podczas aktualizacji profilu: " + e.getMessage());
            return null;
        }
    }

    public void checkUsernameAvailability() {
        if (newUser.getUsername() != null && !newUser.getUsername().trim().isEmpty()) {
            try {
                boolean available = authService.isUsernameAvailable(newUser.getUsername().trim());
                if (!available) {
                    addErrorMessage("Nazwa użytkownika jest już zajęta");
                }
            } catch (Exception e) {
                logger.error("Error checking username availability", e);
                addErrorMessage("Błąd podczas sprawdzania nazwy użytkownika");
            }
        }
    }

    public void checkEmailAvailability() {
        if (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) {
            try {
                boolean available = authService.isEmailAvailable(newUser.getEmail().trim());
                if (!available) {
                    addErrorMessage("Adres email jest już używany");
                }
            } catch (Exception e) {
                logger.error("Error checking email availability", e);
                addErrorMessage("Błąd podczas sprawdzania adresu email");
            }
        }
    }

    public String changePassword() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            addErrorMessage("Musisz być zalogowany");
            return "/login.xhtml?faces-redirect=true";
        }

        try {
            // Tutaj można dodać logikę zmiany hasła
            addInfoMessage("Funkcja zmiany hasła będzie dostępna wkrótce");
            logger.info("Password change requested for user: {}", currentUser.getUsername());

            return null;

        } catch (Exception e) {
            logger.error("Error changing password for user: {}", currentUser.getUsername(), e);
            addErrorMessage("Błąd podczas zmiany hasła: " + e.getMessage());
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
        } else if (newUser.getUsername().trim().length() > 50) {
            addErrorMessage("Nazwa użytkownika nie może być dłuższa niż 50 znaków");
            valid = false;
        } else {
            try {
                if (!authService.isUsernameAvailable(newUser.getUsername().trim())) {
                    addErrorMessage("Nazwa użytkownika jest już zajęta");
                    valid = false;
                }
            } catch (Exception e) {
                logger.error("Error checking username availability during validation", e);
                addErrorMessage("Błąd podczas sprawdzania nazwy użytkownika");
                valid = false;
            }
        }

        if (newUser.getPassword() == null || newUser.getPassword().isEmpty()) {
            addErrorMessage("Hasło jest wymagane");
            valid = false;
        } else if (newUser.getPassword().length() < 6) {
            addErrorMessage("Hasło musi mieć co najmniej 6 znaków");
            valid = false;
        } else if (newUser.getPassword().length() > 100) {
            addErrorMessage("Hasło nie może być dłuższe niż 100 znaków");
            valid = false;
        }

        if (confirmPassword == null || !confirmPassword.equals(newUser.getPassword())) {
            addErrorMessage("Hasła nie są identyczne");
            valid = false;
        }

        if (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) {
            if (!isValidEmail(newUser.getEmail().trim())) {
                addErrorMessage("Nieprawidłowy format adresu email");
                valid = false;
            } else {
                try {
                    if (!authService.isEmailAvailable(newUser.getEmail().trim())) {
                        addErrorMessage("Adres email jest już używany");
                        valid = false;
                    }
                } catch (Exception e) {
                    logger.error("Error checking email availability during validation", e);
                    addErrorMessage("Błąd podczas sprawdzania adresu email");
                    valid = false;
                }
            }
        }

        return valid;
    }

    private boolean validateProfileUpdate(User user) {
        boolean valid = true;

        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
            if (user.getFirstName().trim().length() < 2) {
                addErrorMessage("Imię musi mieć co najmniej 2 znaki");
                valid = false;
            } else if (user.getFirstName().trim().length() > 50) {
                addErrorMessage("Imię nie może być dłuższe niż 50 znaków");
                valid = false;
            }
        }

        if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
            if (user.getLastName().trim().length() < 2) {
                addErrorMessage("Nazwisko musi mieć co najmniej 2 znaki");
                valid = false;
            } else if (user.getLastName().trim().length() > 50) {
                addErrorMessage("Nazwisko nie może być dłuższe niż 50 znaków");
                valid = false;
            }
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!isValidEmail(user.getEmail().trim())) {
                addErrorMessage("Nieprawidłowy format adresu email");
                valid = false;
            }
        }

        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!isValidPhone(user.getPhone().trim())) {
                addErrorMessage("Nieprawidłowy format numeru telefonu");
                valid = false;
            }
        }

        if (user.getAddress() != null && !user.getAddress().trim().isEmpty()) {
            if (user.getAddress().trim().length() < 10) {
                addErrorMessage("Adres musi mieć co najmniej 10 znaków");
                valid = false;
            } else if (user.getAddress().trim().length() > 500) {
                addErrorMessage("Adres nie może być dłuższy niż 500 znaków");
                valid = false;
            }
        }

        return valid;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Podstawowa walidacja email
        return email.contains("@") &&
                email.contains(".") &&
                email.indexOf("@") > 0 &&
                email.lastIndexOf(".") > email.indexOf("@") &&
                email.length() >= 5 &&
                email.length() <= 100;
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // Usuń wszystkie znaki specjalne i sprawdź czy pozostałe to cyfry
        String cleanPhone = phone.replaceAll("[\\s\\-\\+\\(\\)\\.]", "");
        return cleanPhone.length() >= 9 &&
                cleanPhone.length() <= 15 &&
                cleanPhone.matches("\\d+");
    }

    // Metody pomocnicze dla UI
    public String getUserInitials() {
        User user = getCurrentUser();
        if (user == null) return "?";

        StringBuilder initials = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials.append(user.getFirstName().charAt(0));
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials.append(user.getLastName().charAt(0));
        }

        return initials.length() > 0 ? initials.toString().toUpperCase() : user.getUsername().substring(0, 1).toUpperCase();
    }

    public String getUserDisplayName() {
        User user = getCurrentUser();
        if (user == null) return "Gość";

        if (user.getFirstName() != null && user.getLastName() != null &&
                !user.getFirstName().isEmpty() && !user.getLastName().isEmpty()) {
            return user.getFirstName() + " " + user.getLastName();
        }

        return user.getUsername();
    }

    public boolean hasCompleteProfile() {
        User user = getCurrentUser();
        if (user == null) return false;

        return user.getFirstName() != null && !user.getFirstName().trim().isEmpty() &&
                user.getLastName() != null && !user.getLastName().trim().isEmpty() &&
                user.getEmail() != null && !user.getEmail().trim().isEmpty();
    }

    public int getProfileCompletionPercentage() {
        User user = getCurrentUser();
        if (user == null) return 0;

        int completed = 0;
        int total = 6;

        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) completed++;
        if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) completed++;
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) completed++;
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) completed++;
        if (user.getAddress() != null && !user.getAddress().trim().isEmpty()) completed++;
        completed++; // Username jest zawsze wypełniony

        return (completed * 100) / total;
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