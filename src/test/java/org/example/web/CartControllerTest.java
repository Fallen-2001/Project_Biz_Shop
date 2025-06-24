package org.example.web;

import org.example.model.CartItem;
import org.example.model.Product;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.example.service.CartServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartServiceInterface cartService;

    @Mock
    private AuthServiceInterface authService;

    @InjectMocks
    private CartController cartController;

    private User testUser;
    private Product testProduct;
    private CartItem testCartItem;
    private List<CartItem> testCartItems;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

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
        testCartItem.setQuantity(1);

        testCartItems = Arrays.asList(testCartItem);
    }

    @Test
    void testInit() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        cartController.init();

        // Then
        verify(cartService).getCartItems(testUser);
        assertTrue(cartController.isCartLoaded());
        assertEquals(1, cartController.getCartItems().size());
    }

    @Test
    void testInitWithNullUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        cartController.init();

        // Then
        verify(cartService, never()).getCartItems(any());
        assertFalse(cartController.isCartLoaded());
        assertTrue(cartController.getCartItems().isEmpty());
    }

    @Test
    void testLoadCart() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        cartController.loadCart();

        // Then
        verify(cartService).getCartItems(testUser);
        assertTrue(cartController.isCartLoaded());
        assertEquals(testCartItems, cartController.getCartItems());
    }

    @Test
    void testLoadCartWithException() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenThrow(new RuntimeException("Database error"));

        // When
        cartController.loadCart();

        // Then
        assertFalse(cartController.isCartLoaded());
        assertTrue(cartController.getCartItems().isEmpty());
    }

    @Test
    void testAddToCartSuccess() throws Exception {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(cartService).addToCart(1L, 1);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        String result = cartController.addToCart(1L);

        // Then
        assertNull(result); // Should stay on same page
        verify(cartService).addToCart(1L, 1);
        verify(cartService).getCartItems(testUser); // Should reload cart
    }

    @Test
    void testAddToCartNotLoggedIn() {
        // Given
        when(authService.isLoggedIn()).thenReturn(false);

        // When
        String result = cartController.addToCart(1L);

        // Then
        assertEquals("/login.xhtml?faces-redirect=true", result);
        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    void testAddToCartWithException() throws Exception {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        doThrow(new Exception("Product already in cart")).when(cartService).addToCart(1L, 1);

        // When
        String result = cartController.addToCart(1L);

        // Then
        assertNull(result);
        verify(cartService).addToCart(1L, 1);
    }

    @Test
    void testRemoveFromCart() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(cartService).removeFromCart(1L);
        when(cartService.getCartItems(testUser)).thenReturn(Arrays.asList());

        // When
        cartController.removeFromCart(1L);

        // Then
        verify(cartService).removeFromCart(1L);
        verify(cartService).getCartItems(testUser); // Should reload cart
    }

    @Test
    void testRemoveFromCartWithException() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doThrow(new Exception("Item not found")).when(cartService).removeFromCart(1L);

        // When
        cartController.removeFromCart(1L);

        // Then
        verify(cartService).removeFromCart(1L);
    }

    @Test
    void testClearCart() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(cartService).clearCart(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(Arrays.asList());

        // When
        cartController.clearCart();

        // Then
        verify(cartService).clearCart(testUser);
        verify(cartService).getCartItems(testUser); // Should reload cart
    }

    @Test
    void testClearCartWithNullUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        cartController.clearCart();

        // Then
        verify(cartService, never()).clearCart(any());
    }

    @Test
    void testClearCartWithException() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doThrow(new RuntimeException("Database error")).when(cartService).clearCart(testUser);

        // When
        cartController.clearCart();

        // Then
        verify(cartService).clearCart(testUser);
    }

    @Test
    void testClearCartSilently() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(cartService).clearCart(testUser);

        // When
        cartController.clearCartSilently();

        // Then
        verify(cartService).clearCart(testUser);
        assertTrue(cartController.getCartItems().isEmpty());
        assertTrue(cartController.isCartLoaded());
    }

    @Test
    void testForceRefreshCart() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        cartController.forceRefreshCart();

        // Then
        assertFalse(cartController.isCartLoaded()); // Should reset loaded flag
        verify(cartService).getCartItems(testUser);
    }

    @Test
    void testGetCartTotal() {
        // Given
        cartController.setCartItems(testCartItems);
        BigDecimal expectedTotal = new BigDecimal("99.99");

        // When
        BigDecimal total = cartController.getCartTotal();

        // Then
        assertEquals(expectedTotal, total);
    }

    @Test
    void testGetCartTotalEmptyCart() {
        // Given
        cartController.setCartItems(Arrays.asList());

        // When
        BigDecimal total = cartController.getCartTotal();

        // Then
        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void testGetCartTotalNullCart() {
        // Given
        cartController.setCartItems(null);

        // When
        BigDecimal total = cartController.getCartTotal();

        // Then
        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void testGetCartItemCount() {
        // Given
        cartController.setCartItems(testCartItems);

        // When
        int count = cartController.getCartItemCount();

        // Then
        assertEquals(1, count);
    }

    @Test
    void testGetCartItemCountEmptyCart() {
        // Given
        cartController.setCartItems(Arrays.asList());

        // When
        int count = cartController.getCartItemCount();

        // Then
        assertEquals(0, count);
    }

    @Test
    void testGetCartItemCountNullCart() {
        // Given
        cartController.setCartItems(null);

        // When
        int count = cartController.getCartItemCount();

        // Then
        assertEquals(0, count);
    }

    @Test
    void testGetCartItemsCount() {
        // Given
        cartController.setCartItems(testCartItems);

        // When
        int count = cartController.getCartItemsCount();

        // Then
        assertEquals(1, count); // Should be same as getCartItemCount()
    }

    @Test
    void testIsCartEmpty() {
        // Empty cart
        cartController.setCartItems(Arrays.asList());
        assertTrue(cartController.isCartEmpty());

        // Null cart
        cartController.setCartItems(null);
        assertTrue(cartController.isCartEmpty());

        // Non-empty cart
        cartController.setCartItems(testCartItems);
        assertFalse(cartController.isCartEmpty());
    }

    @Test
    void testGoToCheckoutSuccess() {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        cartController.setCartItems(testCartItems);

        // When
        String result = cartController.goToCheckout();

        // Then
        assertEquals("/user/checkout.xhtml?faces-redirect=true", result);
    }

    @Test
    void testGoToCheckoutNotLoggedIn() {
        // Given
        when(authService.isLoggedIn()).thenReturn(false);

        // When
        String result = cartController.goToCheckout();

        // Then
        assertEquals("/login.xhtml?faces-redirect=true", result);
    }

    @Test
    void testGoToCheckoutEmptyCart() {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        cartController.setCartItems(Arrays.asList());

        // When
        String result = cartController.goToCheckout();

        // Then
        assertNull(result); // Should stay on same page
    }

    @Test
    void testGetCartItemsAutoLoad() {
        // Given - cart not loaded initially
        cartController.setCartItems(null);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        List<CartItem> items = cartController.getCartItems();

        // Then
        assertEquals(testCartItems, items);
        verify(cartService).getCartItems(testUser);
    }

    @Test
    void testGetCartItemsSafe() {
        // Given - null cart items
        cartController.setCartItems(null);

        // When
        List<CartItem> items = cartController.getCartItemsSafe();

        // Then
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void testIsCanPlaceOrder() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.canPlaceOrder(testUser)).thenReturn(true);

        // When
        boolean canPlace = cartController.isCanPlaceOrder();

        // Then
        assertTrue(canPlace);
        verify(cartService).canPlaceOrder(testUser);
    }

    @Test
    void testIsCanPlaceOrderNoUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        boolean canPlace = cartController.isCanPlaceOrder();

        // Then
        assertFalse(canPlace);
        verify(cartService, never()).canPlaceOrder(any());
    }

    @Test
    void testGetCanPlaceOrder() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.canPlaceOrder(testUser)).thenReturn(true);

        // When
        boolean canPlace = cartController.getCanPlaceOrder();

        // Then
        assertTrue(canPlace);
        verify(cartService).canPlaceOrder(testUser);
    }

    @Test
    void testSynchronizeCart() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(cartService).synchronizeCartWithStock(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        cartController.synchronizeCart();

        // Then
        verify(cartService).synchronizeCartWithStock(testUser);
        verify(cartService).getCartItems(testUser); // Should reload cart
    }

    @Test
    void testSynchronizeCartWithException() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        doThrow(new RuntimeException("Sync error")).when(cartService).synchronizeCartWithStock(testUser);

        // When
        cartController.synchronizeCart();

        // Then
        verify(cartService).synchronizeCartWithStock(testUser);
    }

    @Test
    void testHasUnavailableProducts() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.hasUnavailableProducts(testUser)).thenReturn(true);

        // When
        boolean hasUnavailable = cartController.hasUnavailableProducts();

        // Then
        assertTrue(hasUnavailable);
        verify(cartService).hasUnavailableProducts(testUser);
    }

    @Test
    void testHasUnavailableProductsNoUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        boolean hasUnavailable = cartController.hasUnavailableProducts();

        // Then
        assertFalse(hasUnavailable);
        verify(cartService, never()).hasUnavailableProducts(any());
    }

    @Test
    void testGetUnavailableProductsCount() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getUnavailableProductsCount(testUser)).thenReturn(2);

        // When
        int count = cartController.getUnavailableProductsCount();

        // Then
        assertEquals(2, count);
        verify(cartService).getUnavailableProductsCount(testUser);
    }

    @Test
    void testGetUnavailableProductsCountNoUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        int count = cartController.getUnavailableProductsCount();

        // Then
        assertEquals(0, count);
        verify(cartService, never()).getUnavailableProductsCount(any());
    }

    @Test
    void testDebugMethods() {
        // Test debug info methods
        cartController.setCartItems(testCartItems);

        String debugInfo = cartController.getCartDebugInfo();
        assertNotNull(debugInfo);
        assertTrue(debugInfo.contains("Size: 1"));

        String firstItemDebug = cartController.getFirstItemDebug();
        assertNotNull(firstItemDebug);
        assertTrue(firstItemDebug.contains("Test Product"));

        String testGetCartItems = cartController.getTestGetCartItems();
        assertNotNull(testGetCartItems);
        assertTrue(testGetCartItems.contains("returned 1 items"));
    }

    @Test
    void testDebugMethodsEmptyCart() {
        // Test debug methods with empty cart
        cartController.setCartItems(Arrays.asList());

        String debugInfo = cartController.getCartDebugInfo();
        assertTrue(debugInfo.contains("Size: 0"));

        String firstItemDebug = cartController.getFirstItemDebug();
        assertEquals("No items in cart", firstItemDebug);
    }

    @Test
    void testServiceDebugInfo() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        String serviceDebug = cartController.getServiceDebugInfo();

        // Then
        assertNotNull(serviceDebug);
        assertTrue(serviceDebug.contains("CartService returned 1 items"));
        verify(cartService).getCartItems(testUser);
    }

    @Test
    void testServiceDebugInfoNoUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        String serviceDebug = cartController.getServiceDebugInfo();

        // Then
        assertEquals("No current user", serviceDebug);
    }

    @Test
    void testLoadCartFromService() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // When
        cartController.loadCartFromService();

        // Then
        assertEquals(testCartItems, cartController.getCartItems());
        assertTrue(cartController.isCartLoaded());
        verify(cartService).getCartItems(testUser);
    }

    @Test
    void testLoadCartFromServiceWithException() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenThrow(new RuntimeException("Service error"));

        // When
        cartController.loadCartFromService();

        // Then
        verify(cartService).getCartItems(testUser);
    }

    @Test
    void testGetSessionDebugInfo() {
        // When
        String sessionDebug = cartController.getSessionDebugInfo();

        // Then
        assertNotNull(sessionDebug);
        // Cannot test specifics as it depends on JSF context
    }

    @Test
    void testCartLoadingFlow() {
        // Test complete cart loading flow

        // 1. Initial state
        assertFalse(cartController.isCartLoaded());

        // 2. Setup mocks
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getCartItems(testUser)).thenReturn(testCartItems);

        // 3. Load cart
        cartController.loadCart();
        assertTrue(cartController.isCartLoaded());
        assertEquals(1, cartController.getCartItemCount());

        // 4. Add item
        when(cartService.getCartItems(testUser)).thenReturn(Arrays.asList(testCartItem, testCartItem));
        cartController.addToCart(2L);

        // 5. Remove item
        when(cartService.getCartItems(testUser)).thenReturn(Arrays.asList(testCartItem));
        cartController.removeFromCart(1L);

        // 6. Clear cart
        when(cartService.getCartItems(testUser)).thenReturn(Arrays.asList());
        cartController.clearCart();
        assertTrue(cartController.isCartEmpty());

        // Verify all interactions
        verify(cartService, atLeast(4)).getCartItems(testUser);
        verify(cartService).removeFromCart(1L);
        verify(cartService).clearCart(testUser);
    }
}