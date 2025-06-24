package org.example.service;

import org.example.dao.CartDaoInterface;
import org.example.dao.ProductDaoInterface;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.model.Role;
import org.example.model.User;
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
class CartServiceTest {

    @Mock
    private CartDaoInterface cartDao;

    @Mock
    private ProductDaoInterface productDao;

    @Mock
    private AuthServiceInterface authService;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private CartItem testCartItem;

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
        testProduct.setCategory("Test Category");

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setUser(testUser);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(1);
    }

    @Test
    void testGetCartItems() {
        // Given
        List<CartItem> expectedItems = Arrays.asList(testCartItem);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(expectedItems);

        // When
        List<CartItem> result = cartService.getCartItems(testUser);

        // Then
        assertEquals(1, result.size());
        assertEquals(testCartItem, result.get(0));
        verify(cartDao).findByUserWithProducts(testUser);
    }

    @Test
    void testGetCartItemsWithNullUser() {
        // When
        List<CartItem> result = cartService.getCartItems(null);

        // Then
        assertTrue(result.isEmpty());
        verify(cartDao, never()).findByUserWithProducts(any());
    }

    @Test
    void testAddToCartSuccess() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartDao.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.empty());

        // When
        cartService.addToCart(1L, 1);

        // Then
        verify(cartDao).save(any(CartItem.class));
    }

    @Test
    void testAddToCartWithoutLogin() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.addToCart(1L, 1);
        });

        assertEquals("Musisz być zalogowany, aby dodać produkt do koszyka", exception.getMessage());
        verify(cartDao, never()).save(any());
    }

    @Test
    void testAddToCartNonExistentProduct() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.addToCart(testUser, 999L, 1);
        });

        assertEquals("Produkt nie został znaleziony", exception.getMessage());
        verify(cartDao, never()).save(any());
    }

    @Test
    void testAddToCartInactiveProduct() {
        // Given
        testProduct.setActive(false);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.addToCart(testUser, 1L, 1);
        });

        assertEquals("Produkt nie jest dostępny", exception.getMessage());
        verify(cartDao, never()).save(any());
    }

    @Test
    void testAddToCartOutOfStock() {
        // Given
        testProduct.setStockQuantity(0);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.addToCart(testUser, 1L, 1);
        });

        assertEquals("Produkt jest obecnie niedostępny", exception.getMessage());
        verify(cartDao, never()).save(any());
    }

    @Test
    void testAddToCartProductAlreadyExists() {
        // Given
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartDao.findByUserAndProduct(testUser, testProduct)).thenReturn(Optional.of(testCartItem));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.addToCart(testUser, 1L, 1);
        });

        assertEquals("Produkt już znajduje się w koszyku", exception.getMessage());
        verify(cartDao, never()).save(any());
        verify(cartDao, never()).update(any());
    }

    @Test
    void testRemoveFromCartSuccess() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartDao.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testCartItem));

        // When
        cartService.removeFromCart(1L);

        // Then
        verify(cartDao).delete(1L);
    }

    @Test
    void testRemoveFromCartWithoutLogin() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.removeFromCart(1L);
        });

        assertEquals("Musisz być zalogowany", exception.getMessage());
        verify(cartDao, never()).delete(any());
    }

    @Test
    void testRemoveFromCartItemNotFound() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(cartDao.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.removeFromCart(1L);
        });

        assertEquals("Element koszyka nie został znaleziony lub nie należy do Ciebie", exception.getMessage());
        verify(cartDao, never()).delete(any());
    }

    @Test
    void testClearCart() {
        // When
        cartService.clearCart(testUser);

        // Then
        verify(cartDao).deleteByUser(testUser);
    }

    @Test
    void testClearCartWithNullUser() {
        // When
        cartService.clearCart(null);

        // Then
        verify(cartDao, never()).deleteByUser(any());
    }

    @Test
    void testGetCartTotal() {
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(cartItems);

        // When
        BigDecimal total = cartService.getCartTotal(testUser);

        // Then
        assertEquals(new BigDecimal("99.99"), total);
    }

    @Test
    void testGetCartTotalWithMultipleItems() {
        // Given
        Product product2 = new Product();
        product2.setPrice(new BigDecimal("49.99"));

        CartItem cartItem2 = new CartItem();
        cartItem2.setProduct(product2);
        cartItem2.setQuantity(1);

        List<CartItem> cartItems = Arrays.asList(testCartItem, cartItem2);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(cartItems);

        // When
        BigDecimal total = cartService.getCartTotal(testUser);

        // Then
        assertEquals(new BigDecimal("149.98"), total);
    }

    @Test
    void testGetCartTotalWithNullUser() {
        // When
        BigDecimal total = cartService.getCartTotal(null);

        // Then
        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void testGetCartItemCount() {
        // Given
        when(cartDao.countByUser(testUser)).thenReturn(3);

        // When
        int count = cartService.getCartItemCount(testUser);

        // Then
        assertEquals(3, count);
        verify(cartDao).countByUser(testUser);
    }

    @Test
    void testGetCartItemCountWithNullUser() {
        // When
        int count = cartService.getCartItemCount(null);

        // Then
        assertEquals(0, count);
        verify(cartDao, never()).countByUser(any());
    }

    @Test
    void testIsCartEmpty() {
        // Given
        when(cartDao.countByUser(testUser)).thenReturn(0);

        // When
        boolean isEmpty = cartService.isCartEmpty(testUser);

        // Then
        assertTrue(isEmpty);
    }

    @Test
    void testIsCartNotEmpty() {
        // Given
        when(cartDao.countByUser(testUser)).thenReturn(2);

        // When
        boolean isEmpty = cartService.isCartEmpty(testUser);

        // Then
        assertFalse(isEmpty);
    }

    @Test
    void testHasProductInCart() {
        // Given
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartDao.existsByUserAndProduct(testUser, testProduct)).thenReturn(true);

        // When
        boolean hasProduct = cartService.hasProductInCart(testUser, 1L);

        // Then
        assertTrue(hasProduct);
    }

    @Test
    void testHasProductInCartReturnsFalse() {
        // Given
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartDao.existsByUserAndProduct(testUser, testProduct)).thenReturn(false);

        // When
        boolean hasProduct = cartService.hasProductInCart(testUser, 1L);

        // Then
        assertFalse(hasProduct);
    }

    @Test
    void testCanPlaceOrderTrue() {
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(cartItems);

        // When
        boolean canPlace = cartService.canPlaceOrder(testUser);

        // Then
        assertTrue(canPlace);
    }

    @Test
    void testCanPlaceOrderEmptyCart() {
        // Given
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(Arrays.asList());

        // When
        boolean canPlace = cartService.canPlaceOrder(testUser);

        // Then
        assertFalse(canPlace);
    }

    @Test
    void testCanPlaceOrderInactiveProduct() {
        // Given
        testProduct.setActive(false);
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(cartItems);

        // When
        boolean canPlace = cartService.canPlaceOrder(testUser);

        // Then
        assertFalse(canPlace);
    }

    @Test
    void testCanPlaceOrderInsufficientStock() {
        // Given
        testProduct.setStockQuantity(0);
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(cartItems);

        // When
        boolean canPlace = cartService.canPlaceOrder(testUser);

        // Then
        assertFalse(canPlace);
    }

    @Test
    void testSynchronizeCartWithStock() {
        // Given
        testProduct.setStockQuantity(0); // Out of stock
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartDao.findByUserWithProducts(testUser)).thenReturn(cartItems);

        // When
        cartService.synchronizeCartWithStock(testUser);

        // Then
        verify(cartDao).delete(testCartItem.getId());
    }

    @Test
    void testSynchronizeCartWithStockNullUser() {
        // When
        cartService.synchronizeCartWithStock(null);

        // Then
        verify(cartDao, never()).findByUserWithProducts(any());
        verify(cartDao, never()).delete(any());
    }
}