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
                "SELECT c FROM CartItem c WHERE c.user = :user", CartItem.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public Optional<CartItem> findByUserAndProduct(User user, Product product) {
        TypedQuery<CartItem> query = em.createQuery(
                "SELECT c FROM CartItem c WHERE c.user = :user AND c.product = :product", CartItem.class);
        query.setParameter("user", user);
        query.setParameter("product", product);
        return query.getResultStream().findFirst();
    }

    @Override
    @Transactional
    public void save(CartItem cartItem) {
        em.persist(cartItem);
    }

    @Override
    @Transactional
    public void update(CartItem cartItem) {
        em.merge(cartItem);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CartItem cartItem = em.find(CartItem.class, id);
        if (cartItem != null) {
            em.remove(cartItem);
        }
    }

    @Override
    @Transactional
    public void deleteByUser(User user) {
        em.createQuery("DELETE FROM CartItem c WHERE c.user = :user")
                .setParameter("user", user)
                .executeUpdate();
    }

    @Override
    public int countByUser(User user) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM CartItem c WHERE c.user = :user", Long.class);
        query.setParameter("user", user);
        return query.getSingleResult().intValue();
    }
}