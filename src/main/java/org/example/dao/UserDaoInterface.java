package org.example.dao;

import org.example.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDaoInterface {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void save(User user);
    void update(User user);
    void delete(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}