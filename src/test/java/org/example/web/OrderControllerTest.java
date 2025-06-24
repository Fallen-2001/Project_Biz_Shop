package org.example.web;

import org.example.model.*;
import org.example.service.AuthServiceInterface;
import org.example.service.CartService;
import org.example.service.CartServiceInterface;
import org.example.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartServiceInterface cartService;

    @Mock
    private AuthServiceInterface authService;

    @Mock
    private CartController cartController;

    @InjectMocks
    private OrderController orderController;

    private User testUser;
    private User adminUser;
    private Order testOrder;
    private List<Order> testOrders;
    private Product testProduct;
    private OrderItem testOrderItem;

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

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(2);
        testOrderItem.setPrice(new BigDecimal("99.99"));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setStatus(OrderStatus.CONFIRMED);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setTotalAmount(new BigDecimal("199.98"));
        testOrder.setShippingAddress("Test Address 123, Test City");
        testOrder.addOrderItem(testOrderItem);

        testOrders = Arrays.asList(testOrder);
    }

    @Test
    void testInit() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderService.getUserOrders(testUser)).thenReturn(testOrders);

        // When
        orderController.init();

        // Then
        verify(orderService).getUserOrders(testUser);
        assertEquals(1, orderController.getUserOrders().size());
    }

    @Test
    void testInitWithNullUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        orderController.init();

        // Then
        verify(orderService, never()).getUserOrders(any());
        assertTrue(orderController.getUserOrders().isEmpty());
    }

    @Test
    void testLoadOrders() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderService.getUserOrders(testUser)).thenReturn(testOrders);

        // When
        orderController.loadOrders();

        // Then
        verify(orderService).getUserOrders(testUser);
        assertEquals(testOrders, orderController.getUserOrders());
    }

    @Test
    void testLoadOrdersWithException() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderService.getUserOrders(testUser)).thenThrow(new RuntimeException("Database error"));

        // When
        orderController.loadOrders();

        // Then
        assertTrue(orderController.getUserOrders().isEmpty());
    }

    @Test
    void testPlaceOrderSuccess() throws Exception {
        // Given
        String shippingAddress = "Test Address 123, Test City";
        orderController.setShippingAddress(shippingAddress);

        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);
        when(cartService instanceof CartService).thenReturn(true);

        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        doNothing().when(mockCartService).synchronizeCartWithStock(testUser);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(true);

        when(orderService.createOrderFromCart(testUser, shippingAddress)).thenReturn(testOrder);
        when(orderService.getUserOrders(testUser)).thenReturn(testOrders);
        doNothing().when(cartController).clearCartSilently();

        // When
        String result = orderController.placeOrder();

        // Then
        assertEquals("/user/orders.xhtml?faces-redirect=true&orderPlaced=true", result);
        assertNull(orderController.getShippingAddress()); // Should be cleared
        verify(orderService).createOrderFromCart(testUser, shippingAddress);
        verify(cartController).clearCartSilently();
        verify(orderService).getUserOrders(testUser);
    }

    @Test
    void testPlaceOrderNotLoggedIn() {
        // Given
        when(authService.isLoggedIn()).thenReturn(false);

        // When
        String result = orderController.placeOrder();

        // Then
        assertEquals("/login.xhtml?faces-redirect=true", result);
        verify(orderService, never()).createOrderFromCart(any(), any());
    }

    @Test
    void testPlaceOrderEmptyAddress() {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        orderController.setShippingAddress("");

        // When
        String result = orderController.placeOrder();

        // Then
        assertNull(result); // Should stay on same page
        verify(orderService, never()).createOrderFromCart(any(), any());
    }

    @Test
    void testPlaceOrderShortAddress() {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        orderController.setShippingAddress("Short");

        // When
        String result = orderController.placeOrder();

        // Then
        assertNull(result); // Should stay on same page
        verify(orderService, never()).createOrderFromCart(any(), any());
    }

    @Test
    void testPlaceOrderEmptyCart() {
        // Given
        orderController.setShippingAddress("Test Address 123, Test City");
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(true);

        // When
        String result = orderController.placeOrder();

        // Then
        assertEquals("/user/cart.xhtml?faces-redirect=true", result);
        verify(orderService, never()).createOrderFromCart(any(), any());
    }

    @Test
    void testPlaceOrderCannotPlace() {
        // Given
        orderController.setShippingAddress("Test Address 123, Test City");
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);
        when(cartService instanceof CartService).thenReturn(true);

        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        doNothing().when(mockCartService).synchronizeCartWithStock(testUser);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(false);

        // When
        String result = orderController.placeOrder();

        // Then
        assertEquals("/user/cart.xhtml?faces-redirect=true", result);
        verify(orderService, never()).createOrderFromCart(any(), any());
    }

    @Test
    void testPlaceOrderWithException() throws Exception {
        // Given
        String shippingAddress = "Test Address 123, Test City";
        orderController.setShippingAddress(shippingAddress);

        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);
        when(cartService instanceof CartService).thenReturn(true);

        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        doNothing().when(mockCartService).synchronizeCartWithStock(testUser);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(true);

        when(orderService.createOrderFromCart(testUser, shippingAddress))
                .thenThrow(new Exception("Order creation failed"));

        // When
        String result = orderController.placeOrder();

        // Then
        assertNull(result); // Should stay on same page
        verify(orderService).createOrderFromCart(testUser, shippingAddress);
    }

    @Test
    void testCancelOrderSuccess() throws Exception {
        // Given
        doNothing().when(orderService).cancelOrder(1L);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderService.getUserOrders(testUser)).thenReturn(Arrays.asList());

        // When
        orderController.cancelOrder(1L);

        // Then
        verify(orderService).cancelOrder(1L);
        verify(orderService).getUserOrders(testUser); // Should reload orders
    }

    @Test
    void testCancelOrderWithException() throws Exception {
        // Given
        doThrow(new Exception("Cannot cancel order")).when(orderService).cancelOrder(1L);

        // When
        orderController.cancelOrder(1L);

        // Then
        verify(orderService).cancelOrder(1L);
    }

    @Test
    void testCanCancelOrderAsOwner() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        boolean canCancel = orderController.canCancelOrder(testOrder);

        // Then
        assertTrue(canCancel);
    }

    @Test
    void testCanCancelOrderAsAdmin() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);

        // When
        boolean canCancel = orderController.canCancelOrder(testOrder);

        // Then
        assertTrue(canCancel);
    }

    @Test
    void testCanCancelOrderNotOwner() {
        // Given
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setRole(Role.USER);
        when(authService.getCurrentUser()).thenReturn(otherUser);

        // When
        boolean canCancel = orderController.canCancelOrder(testOrder);

        // Then
        assertFalse(canCancel);
    }

    @Test
    void testCanCancelOrderAlreadyCancelled() {
        // Given
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        boolean canCancel = orderController.canCancelOrder(testOrder);

        // Then
        assertFalse(canCancel);
    }

    @Test
    void testCanCancelOrderNullOrder() {
        // When
        boolean canCancel = orderController.canCancelOrder(null);

        // Then
        assertFalse(canCancel);
    }

    @Test
    void testCanCancelOrderNullUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        boolean canCancel = orderController.canCancelOrder(testOrder);

        // Then
        assertFalse(canCancel);
    }

    @Test
    void testIsCheckoutValid() {
        // Given - valid checkout
        orderController.setShippingAddress("Test Address 123, Test City");
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);
        when(cartService instanceof CartService).thenReturn(true);

        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(true);

        // When
        boolean isValid = orderController.isCheckoutValid();

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsCheckoutValidNoUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        boolean isValid = orderController.isCheckoutValid();

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsCheckoutValidEmptyCart() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(true);

        // When
        boolean isValid = orderController.isCheckoutValid();

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsCheckoutValidShortAddress() {
        // Given
        orderController.setShippingAddress("Short");
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);

        // When
        boolean isValid = orderController.isCheckoutValid();

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateCheckout() {
        // Given
        orderController.setShippingAddress("Test Address 123, Test City");
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);
        when(cartService instanceof CartService).thenReturn(true);

        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        doNothing().when(mockCartService).synchronizeCartWithStock(testUser);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(true);

        // When
        orderController.validateCheckout();

        // Then
        verify(mockCartService).synchronizeCartWithStock(testUser);
        verify(mockCartService).canPlaceOrder(testUser);
    }

    @Test
    void testValidateCheckoutNoUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        orderController.validateCheckout();

        // Then
        // Should not throw exception, just return early
    }

    @Test
    void testPrepopulateShippingAddress() {
        // Given
        testUser.setAddress("User's Address 456, User City");
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        orderController.prepopulateShippingAddress();

        // Then
        assertEquals("User's Address 456, User City", orderController.getShippingAddress());
    }

    @Test
    void testPrepopulateShippingAddressEmptyUserAddress() {
        // Given
        testUser.setAddress("");
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        orderController.prepopulateShippingAddress();

        // Then
        assertNull(orderController.getShippingAddress());
    }

    @Test
    void testPrepopulateShippingAddressNullUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        orderController.prepopulateShippingAddress();

        // Then
        assertNull(orderController.getShippingAddress());
    }

    @Test
    void testGetOrderStatusIcon() {
        assertEquals("⏳", orderController.getOrderStatusIcon(OrderStatus.PENDING));
        assertEquals("✅", orderController.getOrderStatusIcon(OrderStatus.CONFIRMED));
        assertEquals("🚛", orderController.getOrderStatusIcon(OrderStatus.SHIPPED));
        assertEquals("📦", orderController.getOrderStatusIcon(OrderStatus.DELIVERED));
        assertEquals("❌", orderController.getOrderStatusIcon(OrderStatus.CANCELLED));
        assertEquals("❓", orderController.getOrderStatusIcon(null));
    }

    @Test
    void testGetOrderStatusText() {
        assertEquals("Oczekujące", orderController.getOrderStatusText(OrderStatus.PENDING));
        assertEquals("Potwierdzone", orderController.getOrderStatusText(OrderStatus.CONFIRMED));
        assertEquals("Wysłane", orderController.getOrderStatusText(OrderStatus.SHIPPED));
        assertEquals("Dostarczone", orderController.getOrderStatusText(OrderStatus.DELIVERED));
        assertEquals("Anulowane", orderController.getOrderStatusText(OrderStatus.CANCELLED));
        assertEquals("Nieznany", orderController.getOrderStatusText(null));
    }

    @Test
    void testGetOrderStatusClass() {
        assertEquals("status-pending", orderController.getOrderStatusClass(OrderStatus.PENDING));
        assertEquals("status-confirmed", orderController.getOrderStatusClass(OrderStatus.CONFIRMED));
        assertEquals("status-shipped", orderController.getOrderStatusClass(OrderStatus.SHIPPED));
        assertEquals("status-delivered", orderController.getOrderStatusClass(OrderStatus.DELIVERED));
        assertEquals("status-cancelled", orderController.getOrderStatusClass(OrderStatus.CANCELLED));
        assertEquals("status-unknown", orderController.getOrderStatusClass(null));
    }

    @Test
    void testGetOrdersDebugInfo() {
        // Given
        orderController.setUserOrders(testOrders);
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        String debugInfo = orderController.getOrdersDebugInfo();

        // Then
        assertNotNull(debugInfo);
        assertTrue(debugInfo.contains("Orders loaded: 1"));
        assertTrue(debugInfo.contains("Current user: testuser"));
    }

    @Test
    void testForceReloadOrders() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderService.getUserOrders(testUser)).thenReturn(testOrders);

        // When
        orderController.forceReloadOrders();

        // Then
        verify(orderService).getUserOrders(testUser);
        assertEquals(testOrders, orderController.getUserOrders());
    }

    @Test
    void testGetUserOrdersAutoLoad() {
        // Given - orders not loaded initially
        orderController.setUserOrders(Arrays.asList());
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(orderService.getUserOrders(testUser)).thenReturn(testOrders);

        // When
        List<Order> orders = orderController.getUserOrders();

        // Then
        assertEquals(testOrders, orders);
        verify(orderService).getUserOrders(testUser);
    }

    @Test
    void testGetCheckoutValid() {
        // Given
        orderController.setShippingAddress("Test Address 123, Test City");
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);
        when(cartService instanceof CartService).thenReturn(true);

        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(true);

        // When
        boolean isValid = orderController.getCheckoutValid();

        // Then
        assertTrue(isValid);
    }

    @Test
    void testGettersAndSetters() {
        // Test shipping address
        orderController.setShippingAddress("Test Address");
        assertEquals("Test Address", orderController.getShippingAddress());

        // Test user orders
        orderController.setUserOrders(testOrders);
        assertEquals(testOrders, orderController.getUserOrders());
    }

    @Test
    void testCompleteOrderFlow() {
        // Test complete order placement flow

        // 1. Setup initial state
        orderController.setShippingAddress("Test Address 123, Test City");
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.isCartEmpty(testUser)).thenReturn(false);

        // 2. Validate checkout
        assertTrue(orderController.isCheckoutValid());

        // 3. Place order (mock the CartService properly)
        when(cartService instanceof CartService).thenReturn(true);
        CartService mockCartService = mock(CartService.class);
        when(cartService).thenReturn(mockCartService);
        doNothing().when(mockCartService).synchronizeCartWithStock(testUser);
        when(mockCartService.canPlaceOrder(testUser)).thenReturn(true);

        try {
            when(orderService.createOrderFromCart(testUser, "Test Address 123, Test City")).thenReturn(testOrder);
            when(orderService.getUserOrders(testUser)).thenReturn(testOrders);
            doNothing().when(cartController).clearCartSilently();

            String result = orderController.placeOrder();
            assertEquals("/user/orders.xhtml?faces-redirect=true&orderPlaced=true", result);

            // 4. Verify order appears in list
            assertEquals(1, orderController.getUserOrders().size());

            // 5. Test cancellation
            orderController.cancelOrder(1L);

            // Verify all interactions
            verify(orderService).createOrderFromCart(testUser, "Test Address 123, Test City");
            verify(orderService).cancelOrder(1L);
            verify(cartController).clearCartSilently();

        } catch (Exception e) {
            fail("Exception during order flow: " + e.getMessage());
        }
    }
}