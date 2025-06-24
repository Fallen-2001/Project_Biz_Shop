package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.OrderStatus;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderDaoImpl implements OrderDaoInterface {

    @PersistenceContext(unitName = "shopPU")
    private EntityManager em;

    @Override
    public Optional<Order> findById(Long id) {
        // Eager fetch z wszystkimi relacjami
        TypedQuery<Order> query = em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "WHERE o.id = :id", Order.class);
        query.setParameter("id", id);

        try {
            Order order = query.getSingleResult();
            return Optional.of(order);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Order> findByUser(User user) {
        // Eager fetch z orderItems i produktami, posortowane po dacie
        TypedQuery<Order> query = em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "WHERE o.user = :user " +
                        "ORDER BY o.orderDate DESC", Order.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        TypedQuery<Order> query = em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "WHERE o.status = :status " +
                        "ORDER BY o.orderDate DESC", Order.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    @Override
    public List<Order> findAll() {
        // Dla admina - wszystkie zamówienia z eager loading
        TypedQuery<Order> query = em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "ORDER BY o.orderDate DESC", Order.class);
        return query.getResultList();
    }

    @Override
    @Transactional
    public void save(Order order) {
        em.persist(order);
        em.flush();
    }

    @Override
    @Transactional
    public void update(Order order) {
        em.merge(order);
        em.flush();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Order order = em.find(Order.class, id);
        if (order != null) {
            em.remove(order);
            em.flush();
        }
    }
}