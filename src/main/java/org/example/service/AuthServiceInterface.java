package org.example.service;

import org.example.model.User;

public interface AuthServiceInterface {
    User login(String username, String password);
    User register(User user) throws Exception;
    boolean isUsernameAvailable(String username);
    boolean isEmailAvailable(String email);
    User getCurrentUser();
    void setCurrentUser(User user);
    void logout();
}