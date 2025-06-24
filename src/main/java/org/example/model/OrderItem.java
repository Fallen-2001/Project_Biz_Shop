package org.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @SequenceGenerator(name = "orderitem_seq", sequenceName = "orderitem_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER) // Zmienione na EAGER żeby zawsze ładować produkt
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;

    // Konstruktory
    public OrderItem() {}

    public OrderItem(Product product, Integer quantity, BigDecimal price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    /**
     * Zwraca łączną wartość tej pozycji (cena * ilość)
     */
    public BigDecimal getSubtotal() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Zwraca łączną wartość tej pozycji - alias dla getSubtotal()
     */
    public BigDecimal getTotalPrice() {
        return getSubtotal();
    }

    /**
     * Sprawdza czy pozycja jest poprawna
     */
    public boolean isValid() {
        return product != null &&
                quantity != null && quantity > 0 &&
                price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Zwraca nazwę produktu (bezpieczna wersja)
     */
    public String getProductName() {
        return product != null ? product.getName() : "Nieznany produkt";
    }

    /**
     * Zwraca kategorię produktu (bezpieczna wersja)
     */
    public String getProductCategory() {
        return product != null ? product.getCategory() : "Nieznana kategoria";
    }

    /**
     * Sprawdza czy produkt jest nadal aktywny
     */
    public boolean isProductActive() {
        return product != null && product.isActive();
    }

    /**
     * Sprawdza czy produkt jest nadal dostępny w żądanej ilości
     */
    public boolean isProductAvailable() {
        return product != null && product.isAvailable(quantity);
    }

    /**
     * Zwraca sformatowaną cenę jednostkową
     */
    public String getFormattedPrice() {
        if (price == null) return "0,00 zł";
        return String.format("%.2f zł", price);
    }

    /**
     * Zwraca sformatowaną cenę łączną
     */
    public String getFormattedSubtotal() {
        BigDecimal subtotal = getSubtotal();
        return String.format("%.2f zł", subtotal);
    }

    /**
     * Porównuje cenę z aktualną ceną produktu
     */
    public boolean isPriceUpToDate() {
        if (product == null || price == null) return false;
        return price.compareTo(product.getPrice()) == 0;
    }

    /**
     * Zwraca różnicę między ceną historyczną a aktualną
     */
    public BigDecimal getPriceDifference() {
        if (product == null || price == null) return BigDecimal.ZERO;
        return product.getPrice().subtract(price);
    }

    /**
     * Klonuje pozycję zamówienia (bez ID i zamówienia)
     */
    public OrderItem clone() {
        OrderItem cloned = new OrderItem();
        cloned.setProduct(this.product);
        cloned.setQuantity(this.quantity);
        cloned.setPrice(this.price);
        return cloned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("OrderItem{id=%d, productName='%s', quantity=%d, price=%s, subtotal=%s}",
                id,
                getProductName(),
                quantity,
                price,
                getSubtotal());
    }
}