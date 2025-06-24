package org.example.service;

import org.example.dao.CartDaoInterface;
import org.example.dao.OrderDaoInterface;
import org.example.dao.ProductDaoInterface;
import org.example.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderDaoInterface orderDao;

    @Mock
    private CartDaoInterface cartDao;

    @Mock
    private ProductDaoInterface productDao;

    @Mock
    private CartServiceInterface cartService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthServiceInterface authService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private User adminUser;
    private Product testProduct;
    private CartItem testCartItem;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setUser(testUser);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setShippingAddress("Test Address 123");
        testOrder.setStatus(OrderStatus.CONFIRMED);
        testOrder.setTotalAmount(new BigDecimal("199.98"));
    }

    @Test
    void testCreateOrderFromCartSuccess() throws Exception {
        // Given
        String shippingAddress = "Test Address 123, 00-001 Test City";
        List<CartItem> cartItems = Arrays.asList(testCartItem);

        when(cartDao.findByUser(testUser)).thenReturn(cartItems);
        doNothing().when(orderDao).save(any(Order.class));
        doNothing().when(productDao).update(any(Product.class));
        doNothing().when(cartService).clearCart(testUser);
        doNothing().when(emailService).sendOrderConfirmation(any(Order.class));

        // When
        Order result = orderService.createOrderFromCart(testUser, shippingAddress);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(shippingAddress, result.getShippingAddress());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(1, result.getOrderItems().size());

        verify(orderDao).save(any(Order.class));
        verify(productDao).update(testProduct);
        verify(cartService).clearCart(testUser);
        verify(emailService).sendOrderConfirmation(any(Order.class));

        // Verify stock was reduced
        assertEquals(8, testProduct.getStockQuantity()); // 10 - 2
    }

    @Test
    void testCreateOrderFromCartEmptyAddress() {
        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.createOrderFromCart(testUser, "");
        });

        assertEquals("Adres dostawy jest wymagany", exception.getMessage());
        verify(orderDao, never()).save(any());
    }

    @Test
    void testCreateOrderFromCartNullAddress() {
        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.createOrderFromCart(testUser, null);
        });

        assertEquals("Adres dostawy jest wymagany", exception.getMessage());
        verify(orderDao, never()).save(any());
    }

    @Test
    void testCreateOrderFromCartEmptyCart() {
        // Given
        when(cartDao.findByUser(testUser)).thenReturn(Arrays.asList());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.createOrderFromCart(testUser, "Test Address");
        });

        assertEquals("Koszyk jest pusty", exception.getMessage());
        verify(orderDao, never()).save(any());
    }

    @Test
    void testCreateOrderFromCartInactiveProduct() {
        // Given
        testProduct.setActive(false);
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUser(testUser)).thenReturn(cartItems);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.createOrderFromCart(testUser, "Test Address");
        });

        assertEquals("Produkt " + testProduct.getName() + " nie jest już dostępny", exception.getMessage());
        verify(orderDao, never()).save(any());
    }

    @Test
    void testCreateOrderFromCartInsufficientStock() {
        // Given
        testProduct.setStockQuantity(1); // Less than required quantity (2)
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUser(testUser)).thenReturn(cartItems);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.createOrderFromCart(testUser, "Test Address");
        });

        assertTrue(exception.getMessage().contains("Niewystarczająca ilość produktu"));
        verify(orderDao, never()).save(any());
    }

    @Test
    void testCreateOrderFromCartEmailFailure() throws Exception {
        // Given
        String shippingAddress = "Test Address 123";
        List<CartItem> cartItems = Arrays.asList(testCartItem);

        when(cartDao.findByUser(testUser)).thenReturn(cartItems);
        doNothing().when(orderDao).save(any(Order.class));
        doNothing().when(productDao).update(any(Product.class));
        doNothing().when(cartService).clearCart(testUser);
        doThrow(new Exception("Email error")).when(emailService).sendOrderConfirmation(any(Order.class));

        // When - Should not throw exception, just log warning
        Order result = orderService.createOrderFromCart(testUser, shippingAddress);

        // Then
        assertNotNull(result);
        verify(orderDao).save(any(Order.class));
        verify(emailService).sendOrderConfirmation(any(Order.class));
    }

    @Test
    void testGetUserOrders() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderDao.findByUser(testUser)).thenReturn(orders);

        // When
        List<Order> result = orderService.getUserOrders(testUser);

        // Then
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderDao).findByUser(testUser);
    }

    @Test
    void testGetAllOrders() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderDao.findAll()).thenReturn(orders);

        // When
        List<Order> result = orderService.getAllOrders();

        // Then
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderDao).findAll();
    }

    @Test
    void testGetOrderById() {
        // Given
        when(orderDao.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Optional<Order> result = orderService.getOrderById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testOrder, result.get());
        verify(orderDao).findById(1L);
    }

    @Test
    void testGetOrderByIdNotFound() {
        // Given
        when(orderDao.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Order> result = orderService.getOrderById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(orderDao).findById(999L);
    }

    @Test
    void testCancelOrderByOwner() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderDao.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(2);
        testOrder.getOrderItems().add(orderItem);

        // When
        orderService.cancelOrder(1L);

        // Then
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        assertEquals(12, testProduct.getStockQuantity()); // 10 + 2 restored
        verify(orderDao).update(testOrder);
        verify(productDao).update(testProduct);
    }

    @Test
    void testCancelOrderByAdmin() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(orderDao.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(2);
        testOrder.getOrderItems().add(orderItem);

        // When
        orderService.cancelOrder(1L);

        // Then
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderDao).update(testOrder);
    }

    @Test
    void testCancelOrderNotLoggedIn() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.cancelOrder(1L);
        });

        assertEquals("Musisz być zalogowany", exception.getMessage());
        verify(orderDao, never()).update(any());
    }

    @Test
    void testCancelOrderNotFound() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.cancelOrder(999L);
        });

        assertEquals("Zamówienie nie zostało znalezione", exception.getMessage());
        verify(orderDao, never()).update(any());
    }

    @Test
    void testCancelOrderNotOwner() {
        // Given
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setRole(Role.USER);

        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(orderDao.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.cancelOrder(1L);
        });

        assertEquals("Nie masz uprawnień do anulowania tego zamówienia", exception.getMessage());
        verify(orderDao, never()).update(any());
    }

    @Test
    void testCancelOrderAlreadyCancelled() {
        // Given
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderDao.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            orderService.cancelOrder(1L);
        });

        assertEquals("Zamówienie już zostało anulowane", exception.getMessage());
        verify(orderDao, never()).update(any());
    }

    @Test
    void testGetTotalRevenue() {
        // Given
        Order order1 = new Order();
        order1.setTotalAmount(new BigDecimal("100.00"));
        order1.setStatus(OrderStatus.CONFIRMED);

        Order order2 = new Order();
        order2.setTotalAmount(new BigDecimal("200.00"));
        order2.setStatus(OrderStatus.DELIVERED);

        Order cancelledOrder = new Order();
        cancelledOrder.setTotalAmount(new BigDecimal("50.00"));
        cancelledOrder.setStatus(OrderStatus.CANCELLED);

        when(orderDao.findAll()).thenReturn(Arrays.asList(order1, order2, cancelledOrder));

        // When
        BigDecimal revenue = orderService.getTotalRevenue();

        // Then
        assertEquals(new BigDecimal("300.00"), revenue); // Cancelled order excluded
    }

    @Test
    void testGetTotalOrdersCount() {
        // Given
        when(orderDao.findAll()).thenReturn(Arrays.asList(testOrder, new Order(), new Order()));

        // When
        long count = orderService.getTotalOrdersCount();

        // Then
        assertEquals(3, count);
    }

    @Test
    void testGetCompletedOrdersCount() {
        // Given
        Order order1 = new Order();
        order1.setStatus(OrderStatus.CONFIRMED);

        Order order2 = new Order();
        order2.setStatus(OrderStatus.DELIVERED);

        Order cancelledOrder = new Order();
        cancelledOrder.setStatus(OrderStatus.CANCELLED);

        when(orderDao.findAll()).thenReturn(Arrays.asList(order1, order2, cancelledOrder));

        // When
        long count = orderService.getCompletedOrdersCount();

        // Then
        assertEquals(2, count); // Cancelled order excluded
    }

    @Test
    void testCreateOrderCalculatesCorrectTotal() throws Exception {
        // Given
        Product product2 = new Product();
        product2.setId(2L);
        product2.setPrice(new BigDecimal("50.00"));
        product2.setStockQuantity(5);
        product2.setActive(true);

        CartItem cartItem2 = new CartItem();
        cartItem2.setUser(testUser);
        cartItem2.setProduct(product2);
        cartItem2.setQuantity(1);

        List<CartItem> cartItems = Arrays.asList(testCartItem, cartItem2);
        when(cartDao.findByUser(testUser)).thenReturn(cartItems);

        doNothing().when(orderDao).save(any(Order.class));
        doNothing().when(productDao).update(any(Product.class));
        doNothing().when(cartService).clearCart(testUser);
        doNothing().when(emailService).sendOrderConfirmation(any(Order.class));

        // When
        Order result = orderService.createOrderFromCart(testUser, "Test Address");

        // Then
        // testCartItem: 2 * 99.99 = 199.98
        // cartItem2: 1 * 50.00 = 50.00
        // Total: 249.98
        assertEquals(new BigDecimal("249.98"), result.getTotalAmount());
        assertEquals(2, result.getOrderItems().size());
    }
}