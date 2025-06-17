package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Role;
import org.example.model.User;
import java.util.List;

@ApplicationScoped
public class UserDao {
    @PersistenceContext
    private EntityManager em;

    // zamiast bd
    public User findByUsername(String username) {
        if ("admin".equals(username)) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRole(Role.ADMIN);
            return admin;
        }
        return null;
    }

    public void save(User user) {
        System.out.println("saved user: " + user.getUsername());
    }
}