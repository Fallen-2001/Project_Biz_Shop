package org.example.web;

import org.example.model.Product;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.example.service.CartServiceInterface;
import org.example.service.ProductServiceInterface;
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
class ProductControllerTest {

    @Mock
    private ProductServiceInterface productService;

    @Mock
    private CartServiceInterface cartService;

    @Mock
    private AuthServiceInterface authService;

    @InjectMocks
    private ProductController productController;

    private Product testProduct;
    private User testUser;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setDescription("Test description");
        testProduct.setCategory("Electronics");
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        testProducts = Arrays.asList(testProduct);

        // Initialize controller
        productController.init();
    }

    @Test
    void testInitLoadsProducts() {
        // Given
        when(productService.getActiveProducts()).thenReturn(testProducts);

        // When
        productController.init();

        // Then
        verify(productService).getActiveProducts();
    }

    @Test
    void testLoadProductsWithoutFilters() {
        // Given
        when(productService.getActiveProducts()).thenReturn(testProducts);

        // When
        productController.loadProducts();

        // Then
        verify(productService).getActiveProducts();
        verify(productService, never()).searchProducts(any());
        verify(productService, never()).getProductsByCategory(any());
    }

    @Test
    void testLoadProductsWithSearchTerm() {
        // Given
        productController.setSearchTerm("Test");
        when(productService.searchProducts("Test")).thenReturn(testProducts);

        // When
        productController.loadProducts();

        // Then
        verify(productService).searchProducts("Test");
        verify(productService, never()).getActiveProducts();
        verify(productService, never()).getProductsByCategory(any());
    }

    @Test
    void testLoadProductsWithCategory() {
        // Given
        productController.setSelectedCategory("Electronics");
        when(productService.getProductsByCategory("Electronics")).thenReturn(testProducts);

        // When
        productController.loadProducts();

        // Then
        verify(productService).getProductsByCategory("Electronics");
        verify(productService, never()).getActiveProducts();
        verify(productService, never()).searchProducts(any());
    }

    @Test
    void testLoadProductsWithBothFilters() {
        // Given - search term takes precedence
        productController.setSearchTerm("Test");
        productController.setSelectedCategory("Electronics");
        when(productService.searchProducts("Test")).thenReturn(testProducts);

        // When
        productController.loadProducts();

        // Then
        verify(productService).searchProducts("Test");
        verify(productService, never()).getProductsByCategory(any());
        verify(productService, never()).getActiveProducts();
    }

    @Test
    void testAddToCartWithProductSuccess() throws Exception {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        String result = productController.addToCart(testProduct);

        // Then
        assertNull(result); // Should stay on same page
        verify(cartService).addToCart(1L, 1);
    }

    @Test
    void testAddToCartWithIdSuccess() throws Exception {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        String result = productController.addToCart(1L);

        // Then
        assertNull(result); // Should stay on same page
        verify(cartService).addToCart(1L, 1);
    }

    @Test
    void testAddToCartNotLoggedIn() {
        // Given
        when(authService.isLoggedIn()).thenReturn(false);

        // When
        String result = productController.addToCart(1L);

        // Then
        assertEquals("/login.xhtml?faces-redirect=true", result);
        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    void testAddToCartProductNotFound() {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(999L)).thenReturn(Optional.empty());

        // When
        String result = productController.addToCart(999L);

        // Then
        assertNull(result);
        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    void testAddToCartInactiveProduct() {
        // Given
        testProduct.setActive(false);
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));

        // When
        String result = productController.addToCart(1L);

        // Then
        assertNull(result);
        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    void testAddToCartOutOfStock() {
        // Given
        testProduct.setStockQuantity(0);
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));

        // When
        String result = productController.addToCart(1L);

        // Then
        assertNull(result);
        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    void testAddToCartWithCustomQuantity() throws Exception {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        String result = productController.addToCart(1L, 3);

        // Then
        assertNull(result);
        verify(cartService).addToCart(1L, 3);
    }

    @Test
    void testAddToCartWithExcessiveQuantity() {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        // testProduct has stock of 10, try to add 15

        // When
        String result = productController.addToCart(1L, 15);

        // Then
        assertNull(result);
        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    void testAddToCartWithException() throws Exception {
        // Given
        when(authService.isLoggedIn()).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        when(authService.getCurrentUser()).thenReturn(testUser);
        doThrow(new Exception("Cart error")).when(cartService).addToCart(anyLong(), anyInt());

        // When
        String result = productController.addToCart(1L);

        // Then
        assertNull(result);
        verify(cartService).addToCart(1L, 1);
    }

    @Test
    void testViewProductDetails() {
        // Given
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));

        // When
        String result = productController.viewProductDetails(1L);

        // Then
        assertEquals("/user/product-details.xhtml?faces-redirect=true&productId=1", result);
        assertEquals(testProduct, productController.getSelectedProduct());
        verify(productService).getProductById(1L);
    }

    @Test
    void testViewProductDetailsNotFound() {
        // Given
        when(productService.getProductById(999L)).thenReturn(Optional.empty());

        // When
        String result = productController.viewProductDetails(999L);

        // Then
        assertNull(result);
        verify(productService).getProductById(999L);
    }

    @Test
    void testViewProductDetailsWithException() {
        // Given
        when(productService.getProductById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        String result = productController.viewProductDetails(1L);

        // Then
        assertNull(result);
        verify(productService).getProductById(1L);
    }

    @Test
    void testSearch() {
        // Given
        productController.setSearchTerm("Test");
        when(productService.searchProducts("Test")).thenReturn(testProducts);

        // When
        productController.search();

        // Then
        verify(productService).searchProducts("Test");
    }

    @Test
    void testFilterByCategory() {
        // Given
        productController.setSelectedCategory("Electronics");
        when(productService.getProductsByCategory("Electronics")).thenReturn(testProducts);

        // When
        productController.filterByCategory();

        // Then
        verify(productService).getProductsByCategory("Electronics");
    }

    @Test
    void testClearFilters() {
        // Given
        productController.setSearchTerm("Test");
        productController.setSelectedCategory("Electronics");
        productController.setSelectedQuantity(5);
        when(productService.getActiveProducts()).thenReturn(testProducts);

        // When
        productController.clearFilters();

        // Then
        assertNull(productController.getSearchTerm());
        assertNull(productController.getSelectedCategory());
        assertEquals(Integer.valueOf(1), productController.getSelectedQuantity());
        verify(productService).getActiveProducts();
    }

    @Test
    void testGetStockStatusText() {
        // Test different stock levels
        testProduct.setStockQuantity(15);
        assertTrue(productController.getStockStatusText(testProduct).contains("Dostępny"));

        testProduct.setStockQuantity(8);
        assertTrue(productController.getStockStatusText(testProduct).contains("Ograniczona dostępność"));

        testProduct.setStockQuantity(3);
        assertTrue(productController.getStockStatusText(testProduct).contains("Ostatnie sztuki"));

        testProduct.setStockQuantity(0);
        assertTrue(productController.getStockStatusText(testProduct).contains("Wyprzedany"));

        testProduct.setActive(false);
        assertTrue(productController.getStockStatusText(testProduct).contains("niedostępny"));
    }

    @Test
    void testGetStockStatusClass() {
        // Test different stock levels
        testProduct.setStockQuantity(15);
        assertEquals("stock-high", productController.getStockStatusClass(testProduct));

        testProduct.setStockQuantity(8);
        assertEquals("stock-medium", productController.getStockStatusClass(testProduct));

        testProduct.setStockQuantity(3);
        assertEquals("stock-low", productController.getStockStatusClass(testProduct));

        testProduct.setStockQuantity(0);
        assertEquals("stock-out", productController.getStockStatusClass(testProduct));

        testProduct.setActive(false);
        assertEquals("stock-out", productController.getStockStatusClass(testProduct));
    }

    @Test
    void testIsProductAvailable() {
        // Active product with stock
        assertTrue(productController.isProductAvailable(testProduct));

        // Inactive product
        testProduct.setActive(false);
        assertFalse(productController.isProductAvailable(testProduct));

        // Active but no stock
        testProduct.setActive(true);
        testProduct.setStockQuantity(0);
        assertFalse(productController.isProductAvailable(testProduct));

        // Null stock
        testProduct.setStockQuantity(null);
        assertFalse(productController.isProductAvailable(testProduct));
    }

    @Test
    void testGetMaxQuantityForProduct() {
        // Normal stock
        assertEquals(10, productController.getMaxQuantityForProduct(testProduct));

        // High stock (should be limited to 10)
        testProduct.setStockQuantity(20);
        assertEquals(10, productController.getMaxQuantityForProduct(testProduct));

        // Low stock
        testProduct.setStockQuantity(3);
        assertEquals(3, productController.getMaxQuantityForProduct(testProduct));

        // No stock
        testProduct.setStockQuantity(0);
        assertEquals(0, productController.getMaxQuantityForProduct(testProduct));

        // Inactive product
        testProduct.setActive(false);
        assertEquals(0, productController.getMaxQuantityForProduct(testProduct));
    }

    @Test
    void testGetAvailableCategories() {
        // When
        List<String> categories = productController.getAvailableCategories();

        // Then
        assertNotNull(categories);
        assertTrue(categories.contains("Elektronika"));
        assertTrue(categories.contains("Odzież"));
        assertTrue(categories.contains("Dom i ogród"));
        assertTrue(categories.contains("Sport"));
        assertTrue(categories.contains("Książki"));
        assertTrue(categories.contains("Zabawki"));
        assertTrue(categories.contains("Różne"));
    }

    @Test
    void testGetRecommendedProducts() {
        // Given
        when(productService.getActiveProducts()).thenReturn(Arrays.asList(
                testProduct, new Product(), new Product(), new Product(), new Product()
        ));

        // When
        List<Product> recommended = productController.getRecommendedProducts();

        // Then
        assertNotNull(recommended);
        assertTrue(recommended.size() <= 4); // Should be limited to 4
        verify(productService).getActiveProducts();
    }

    @Test
    void testGetProductsByCategory() {
        // Given
        when(productService.getProductsByCategory("Electronics")).thenReturn(testProducts);

        // When
        List<Product> result = productController.getProductsByCategory("Electronics");

        // Then
        assertEquals(testProducts, result);
        verify(productService).getProductsByCategory("Electronics");
    }

    @Test
    void testGettersAndSetters() {
        // Test all getters and setters
        productController.setProducts(testProducts);
        assertEquals(testProducts, productController.getProducts());

        productController.setSelectedProduct(testProduct);
        assertEquals(testProduct, productController.getSelectedProduct());

        productController.setSearchTerm("test");
        assertEquals("test", productController.getSearchTerm());

        productController.setSelectedCategory("Electronics");
        assertEquals("Electronics", productController.getSelectedCategory());

        productController.setSelectedQuantity(5);
        assertEquals(Integer.valueOf(5), productController.getSelectedQuantity());
    }

    @Test
    void testGetTotalProductCount() {
        // Given
        productController.setProducts(testProducts);

        // When
        long count = productController.getTotalProductCount();

        // Then
        assertEquals(1, count);
    }

    @Test
    void testHasSearchResults() {
        // With products
        productController.setProducts(testProducts);
        assertTrue(productController.hasSearchResults());

        // Without products
        productController.setProducts(Arrays.asList());
        assertFalse(productController.hasSearchResults());

        // Null products
        productController.setProducts(null);
        assertFalse(productController.hasSearchResults());
    }

    @Test
    void testIsSearchActive() {
        // No filters
        assertFalse(productController.isSearchActive());

        // With search term
        productController.setSearchTerm("test");
        assertTrue(productController.isSearchActive());

        // Clear search term, add category
        productController.setSearchTerm(null);
        productController.setSelectedCategory("Electronics");
        assertTrue(productController.isSearchActive());

        // Empty strings should not be active
        productController.setSearchTerm("");
        productController.setSelectedCategory("");
        assertFalse(productController.isSearchActive());
    }

    @Test
    void testGetCurrentSearchInfo() {
        // No filters
        assertEquals("Wszystkie produkty", productController.getCurrentSearchInfo());

        // With search term
        productController.setSearchTerm("test");
        assertEquals("Wyniki wyszukiwania dla: 'test'", productController.getCurrentSearchInfo());

        // With category (search term cleared)
        productController.setSearchTerm(null);
        productController.setSelectedCategory("Electronics");
        assertEquals("Kategoria: Electronics", productController.getCurrentSearchInfo());

        // Search term takes precedence
        productController.setSearchTerm("test");
        assertEquals("Wyniki wyszukiwania dla: 'test'", productController.getCurrentSearchInfo());
    }
}