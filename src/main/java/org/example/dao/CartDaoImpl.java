package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.example.model.CartItem;
import org.example.model.User;
import org.example.model.Product;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CartDaoImpl implements CartDaoInterface {

    @PersistenceContext(unitName = "shopPU")
    private EntityManager em;

    @Override
    public List<CartItem> findByUser(User user) {
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT c FROM CartItem c " +
                        "JOIN FETCH c.product " +  // Eager fetch product
                        "WHERE c.user = :user " +
                        "ORDER BY c.id", CartItem.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public Optional<CartItem> findByUserAndProduct(User user, Product product) {
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT c FROM CartItem c " +
                        "JOIN FETCH c.product " +  // Eager fetch product
                        "WHERE c.user = :user AND c.product = :product", CartItem.class);
        query.setParameter("user", user);
        query.setParameter("product", product);
        return query.getResultStream().findFirst();
    }

    @Override
    @Transactional
    public void save(CartItem cartItem) {
        em.persist(cartItem);
        em.flush(); // Wymuszenie zapisu do bazy
    }

    @Override
    @Transactional
    public void update(CartItem cartItem) {
        em.merge(cartItem);
        em.flush(); // Wymuszenie zapisu do bazy
    }

    @Override
    @Transactional
    public CartItem updateAndRefresh(CartItem cartItem) {
        CartItem updated = em.merge(cartItem);
        em.flush();
        em.refresh(updated); // Odśwież z bazy danych
        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CartItem cartItem = em.find(CartItem.class, id);
        if (cartItem != null) {
            em.remove(cartItem);
            em.flush(); // Wymuszenie usunięcia z bazy
        }
    }

    @Override
    @Transactional
    public void deleteByUser(User user) {
        em.createQuery("DELETE FROM CartItem c WHERE c.user = :user")
                .setParameter("user", user)
                .executeUpdate();
        em.flush(); // Wymuszenie usunięcia z bazy
    }

    @Override
    public int countByUser(User user) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM CartItem c WHERE c.user = :user", Long.class);
        query.setParameter("user", user);
        return query.getSingleResult().intValue();
    }

    @Override
    public Optional<CartItem> findByIdAndUser(Long id, User user) {
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT c FROM CartItem c " +
                        "JOIN FETCH c.product " +
                        "WHERE c.id = :id AND c.user = :user", CartItem.class);
        query.setParameter("id", id);
        query.setParameter("user", user);
        return query.getResultStream().findFirst();
    }

    @Override
    public List<CartItem> findByUserWithProducts(User user) {
        // Explicit eager loading with product details
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT DISTINCT c FROM CartItem c " +
                        "JOIN FETCH c.product p " +
                        "WHERE c.user = :user " +
                        "ORDER BY c.id", CartItem.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    // Dodatkowe metody pomocnicze

    @Override
    public List<CartItem> findStaleCartItems(int daysOld) {
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT c FROM CartItem c " +
                        "JOIN FETCH c.product " +
                        "WHERE c.id IN (" +
                        "   SELECT ci.id FROM CartItem ci " +
                        "   WHERE ci.id < :cutoffId" +
                        ")", CartItem.class);

        // Proste podejście - elementy starsze niż określona liczba ID
        long cutoffId = System.currentTimeMillis() / 1000 - (daysOld * 24 * 60 * 60);
        query.setParameter("cutoffId", cutoffId);
        return query.getResultList();
    }

    @Override
    @Transactional
    public void deleteStaleCartItems(int daysOld) {
        long cutoffId = System.currentTimeMillis() / 1000 - (daysOld * 24 * 60 * 60);
        em.createQuery("DELETE FROM CartItem c WHERE c.id < :cutoffId")
                .setParameter("cutoffId", cutoffId)
                .executeUpdate();
        em.flush();
    }

    @Override
    public List<CartItem> findCartItemsWithInactiveProducts() {
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT c FROM CartItem c " +
                        "JOIN FETCH c.product p " +
                        "WHERE p.active = false OR p.stockQuantity = 0", CartItem.class);
        return query.getResultList();
    }

    @Override
    @Transactional
    public int removeCartItemsWithInactiveProducts() {
        return em.createQuery(
                        "DELETE FROM CartItem c WHERE c.product.active = false OR c.product.stockQuantity = 0")
                .executeUpdate();
    }

    @Override
    public boolean existsByUserAndProduct(User user, Product product) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM CartItem c WHERE c.user = :user AND c.product = :product", Long.class);
        query.setParameter("user", user);
        query.setParameter("product", product);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public Optional<CartItem> findWithLock(Long id) {
        CartItem cartItem = em.find(CartItem.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(cartItem);
    }
}