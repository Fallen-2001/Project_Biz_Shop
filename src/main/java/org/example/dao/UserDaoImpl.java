package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserDaoImpl implements UserDaoInterface {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    @PersistenceContext(unitName = "shopPU")
    private EntityManager em;

    @Override
    public Optional<User> findById(Long id) {
        logger.debug("Finding user by id: {}", id);
        try {
            User user = em.find(User.class, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by id: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        logger.debug("Finding user by username: {}", username);
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            return query.getResultStream().findFirst();
        } catch (Exception e) {
            logger.error("Error finding user by username: {}", username, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        logger.debug("Finding user by email: {}", email);
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            return query.getResultStream().findFirst();
        } catch (Exception e) {
            logger.error("Error finding user by email: {}", email, e);
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        logger.debug("Finding all users");
        try {
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding all users", e);
            throw new RuntimeException("Failed to retrieve users", e);
        }
    }

    @Override
    @Transactional
    public void save(User user) {
        logger.debug("Saving user: {}", user.getUsername());
        try {
            em.persist(user);
            em.flush();
            logger.info("User saved successfully: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error saving user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }

    @Override
    @Transactional
    public void update(User user) {
        logger.debug("Updating user: {}", user.getUsername());
        try {
            em.merge(user);
            em.flush();
            logger.info("User updated successfully: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error updating user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        logger.debug("Deleting user with id: {}", id);
        try {
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
                em.flush();
                logger.info("User deleted successfully: {}", id);
            } else {
                logger.warn("User not found for deletion: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting user: {}", id, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        logger.debug("Checking if username exists: {}", username);
        try {
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
            query.setParameter("username", username);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            logger.error("Error checking username existence: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        logger.debug("Checking if email exists: {}", email);
        try {
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
            query.setParameter("email", email);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            logger.error("Error checking email existence: {}", email, e);
            return false;
        }
    }
}