package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.dao.UserDao;
import org.example.model.User;

@ApplicationScoped
public class AuthService {
    @Inject
    private UserDao userDao;

    public User login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}