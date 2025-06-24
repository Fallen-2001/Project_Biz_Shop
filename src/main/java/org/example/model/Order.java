package org.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @SequenceGenerator(name = "order_seq", sequenceName = "order_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // Zmienione na EAGER żeby zawsze ładować użytkownika
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_address", length = 1000)
    private String shippingAddress;

    // Konstruktory
    public Order() {
        this.orderDate = LocalDateTime.now();
        this.orderItems = new ArrayList<>();
    }

    public Order(User user, String shippingAddress) {
        this();
        this.user = user;
        this.shippingAddress = shippingAddress;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<OrderItem> getOrderItems() {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems != null ? orderItems : new ArrayList<>();
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    // Pomocnicze metody
    public void addOrderItem(OrderItem orderItem) {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void removeOrderItem(OrderItem orderItem) {
        if (orderItems != null) {
            orderItems.remove(orderItem);
            orderItem.setOrder(null);
        }
    }

    public void calculateTotalAmount() {
        if (orderItems == null || orderItems.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            return;
        }

        this.totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // NOWE METODY POMOCNICZE

    /**
     * Zwraca liczbę produktów w zamówieniu
     */
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    /**
     * Zwraca łączną ilość wszystkich produktów
     */
    public int getTotalQuantity() {
        if (orderItems == null) return 0;
        return orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    /**
     * Sprawdza czy zamówienie można anulować
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /**
     * Sprawdza czy zamówienie jest aktywne (nie anulowane ani dostarczone)
     */
    public boolean isActive() {
        return status != OrderStatus.CANCELLED && status != OrderStatus.DELIVERED;
    }

    /**
     * Sprawdza czy zamówienie zostało dostarczone
     */
    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED;
    }

    /**
     * Sprawdza czy zamówienie jest w trakcie realizacji
     */
    public boolean isInProgress() {
        return status == OrderStatus.CONFIRMED || status == OrderStatus.SHIPPED;
    }

    /**
     * Zwraca opis statusu zamówienia po polsku
     */
    public String getStatusDescription() {
        return status != null ? status.getDisplayName() : "Nieznany";
    }

    /**
     * Sprawdza czy zamówienie jest puste
     */
    public boolean isEmpty() {
        return orderItems == null || orderItems.isEmpty();
    }

    /**
     * Znajdź pozycję zamówienia po produkcie
     */
    public OrderItem findItemByProduct(Product product) {
        if (orderItems == null || product == null) return null;

        return orderItems.stream()
                .filter(item -> item.getProduct() != null &&
                        Objects.equals(item.getProduct().getId(), product.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sprawdza czy zamówienie zawiera określony produkt
     */
    public boolean containsProduct(Product product) {
        return findItemByProduct(product) != null;
    }

    /**
     * Zwraca sformatowaną datę zamówienia
     */
    public String getFormattedOrderDate() {
        if (orderDate == null) return "";
        return orderDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Zwraca sformatowaną kwotę zamówienia
     */
    public String getFormattedTotalAmount() {
        if (totalAmount == null) return "0,00 zł";
        return String.format("%.2f zł", totalAmount);
    }

    /**
     * Sprawdza integralność danych zamówienia
     */
    public boolean isValid() {
        return user != null &&
                orderDate != null &&
                status != null &&
                totalAmount != null &&
                totalAmount.compareTo(BigDecimal.ZERO) >= 0 &&
                orderItems != null &&
                !orderItems.isEmpty();
    }

    /**
     * Waliduje czy kwota zamówienia się zgadza z pozycjami
     */
    public boolean validateTotalAmount() {
        if (orderItems == null || orderItems.isEmpty()) {
            return totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0;
        }

        BigDecimal calculatedTotal = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalAmount != null && totalAmount.compareTo(calculatedTotal) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Order{id=%d, userId=%d, status=%s, orderDate=%s, totalAmount=%s, itemsCount=%d}",
                id,
                user != null ? user.getId() : null,
                status,
                orderDate,
                totalAmount,
                getItemCount());
    }
}