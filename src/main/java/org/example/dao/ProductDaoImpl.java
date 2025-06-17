package org.example.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.example.model.Product;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductDaoImpl implements ProductDaoInterface {

    @PersistenceContext(unitName = "shopPU")
    private EntityManager em;

    @Override
    public Optional<Product> findById(Long id) {
        Product product = em.find(Product.class, id);
        return Optional.ofNullable(product);
    }

    @Override
    public List<Product> findAll() {
        TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p", Product.class);
        return query.getResultList();
    }

    @Override
    public List<Product> findByCategory(String category) {
        TypedQuery<Product> query = em.createQuery(
                "SELECT p FROM Product p WHERE p.category = :category", Product.class);
        query.setParameter("category", category);
        return query.getResultList();
    }

    @Override
    public List<Product> findActiveProducts() {
        TypedQuery<Product> query = em.createQuery(
                "SELECT p FROM Product p WHERE p.active = true", Product.class);
        return query.getResultList();
    }

    @Override
    public List<Product> searchByName(String name) {
        TypedQuery<Product> query = em.createQuery(
                "SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(:name)", Product.class);
        query.setParameter("name", "%" + name + "%");
        return query.getResultList();
    }

    @Override
    @Transactional
    public void save(Product product) {
        em.persist(product);
    }

    @Override
    @Transactional
    public void update(Product product) {
        em.merge(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = em.find(Product.class, id);
        if (product != null) {
            em.remove(product);
        }
    }
}
