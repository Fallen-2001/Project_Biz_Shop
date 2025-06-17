package org.example.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthService;

@Named
@RequestScoped
public class AuthController {
    private String username;
    private String password;

    @Inject
    private AuthService authService;

    public String login() {
        User user = authService.login(username, password);
        if (user != null) {
            if (user.getRole() == Role.ADMIN) {
                return "admin/products.xhtml?faces-redirect=true";
            } else {
                return "user/products.xhtml?faces-redirect=true";
            }
        }
        return "login.xhtml?error=true";
    }

    // Геттеры и сеттеры
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}