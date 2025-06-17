package org.example.service;

import org.example.dao.ProductDaoInterface;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductDaoInterface productDao;

    @Mock
    private AuthServiceInterface authService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setDescription("Test description");
        testProduct.setCategory("Test Category");
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("user");
        regularUser.setRole(Role.USER);
    }

    @Test
    void testGetAllProducts() {
        // Given
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productDao.findAll()).thenReturn(expectedProducts);

        // When
        List<Product> actualProducts = productService.getAllProducts();

        // Then
        assertEquals(expectedProducts, actualProducts);
        verify(productDao, times(1)).findAll();
    }

    @Test
    void testGetActiveProducts() {
        // Given
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productDao.findActiveProducts()).thenReturn(expectedProducts);

        // When
        List<Product> actualProducts = productService.getActiveProducts();

        // Then
        assertEquals(expectedProducts, actualProducts);
        verify(productDao, times(1)).findActiveProducts();
    }

    @Test
    void testGetProductById() {
        // Given
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        Optional<Product> result = productService.getProductById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testProduct, result.get());
        verify(productDao, times(1)).findById(1L);
    }

    @Test
    void testGetProductByIdNotFound() {
        // Given
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Product> result = productService.getProductById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(productDao, times(1)).findById(999L);
    }

    @Test
    void testAddProductAsAdmin() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("49.99"));

        // When
        productService.addProduct(newProduct);

        // Then
        verify(productDao, times(1)).save(newProduct);
        assertTrue(newProduct.isActive()); // Zmienione z getActive() na isActive()
        assertEquals(Integer.valueOf(0), newProduct.getStockQuantity());
    }

    @Test
    void testAddProductAsRegularUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("49.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(newProduct);
        });

        assertEquals("Tylko administrator może wykonać tę operację", exception.getMessage());
        verify(productDao, never()).save(any());
    }

    @Test
    void testAddProductNotLoggedIn() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("49.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(newProduct);
        });

        assertEquals("Musisz być zalogowany", exception.getMessage());
        verify(productDao, never()).save(any());
    }

    @Test
    void testAddProductValidation() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        // Brak nazwy i ceny

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Nazwa produktu jest wymagana", exception.getMessage());
        verify(productDao, never()).save(any());
    }

    @Test
    void testUpdateProductAsAdmin() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        testProduct.setName("Updated Product");

        // When
        productService.updateProduct(testProduct);

        // Then
        verify(productDao, times(1)).update(testProduct);
    }

    @Test
    void testUpdateProductNotFound() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        Product nonExistentProduct = new Product();
        nonExistentProduct.setId(999L);
        nonExistentProduct.setName("Non-existent");
        nonExistentProduct.setPrice(new BigDecimal("99.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.updateProduct(nonExistentProduct);
        });

        assertEquals("Produkt nie został znaleziony", exception.getMessage());
        verify(productDao, never()).update(any());
    }

    @Test
    void testDeleteProductAsAdmin() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productDao, times(1)).delete(1L);
    }

    @Test
    void testSearchProducts() {
        // Given
        String searchTerm = "test";
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productDao.searchByName(searchTerm)).thenReturn(expectedProducts);

        // When
        List<Product> actualProducts = productService.searchProducts(searchTerm);

        // Then
        assertEquals(expectedProducts, actualProducts);
        verify(productDao, times(1)).searchByName(searchTerm);
    }

    @Test
    void testSearchProductsEmptyTerm() {
        // Given
        String searchTerm = "";
        List<Product> activeProducts = Arrays.asList(testProduct);
        when(productDao.findActiveProducts()).thenReturn(activeProducts);

        // When
        List<Product> actualProducts = productService.searchProducts(searchTerm);

        // Then
        assertEquals(activeProducts, actualProducts);
        verify(productDao, times(1)).findActiveProducts();
        verify(productDao, never()).searchByName(anyString());
    }

    @Test
    void testGetProductsByCategory() {
        // Given
        String category = "Electronics";
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productDao.findByCategory(category)).thenReturn(expectedProducts);

        // When
        List<Product> actualProducts = productService.getProductsByCategory(category);

        // Then
        assertEquals(expectedProducts, actualProducts);
        verify(productDao, times(1)).findByCategory(category);
    }

    @Test
    void testUpdateStock() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));
        Integer newStock = 20;

        // When
        productService.updateStock(1L, newStock);

        // Then
        assertEquals(newStock, testProduct.getStockQuantity());
        verify(productDao, times(1)).update(testProduct);
    }

    @Test
    void testUpdateStockNegativeValue() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Integer negativeStock = -5;

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.updateStock(1L, negativeStock);
        });

        assertEquals("Stan magazynowy musi być liczbą dodatnią", exception.getMessage());
        verify(productDao, never()).update(any());
    }

    @Test
    void testDeactivateProduct() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        productService.deactivateProduct(1L);

        // Then
        assertFalse(testProduct.isActive());
        verify(productDao, times(1)).update(testProduct);
    }

    @Test
    void testActivateProduct() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        testProduct.setActive(false);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        productService.activateProduct(1L);

        // Then
        assertTrue(testProduct.isActive());
        verify(productDao, times(1)).update(testProduct);
    }

    @Test
    void testValidateProductInvalidName() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName(""); // Pusta nazwa
        invalidProduct.setPrice(new BigDecimal("99.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Nazwa produktu jest wymagana", exception.getMessage());
    }

    @Test
    void testValidateProductInvalidPrice() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName("Valid Name");
        invalidProduct.setPrice(new BigDecimal("-10.00")); // Ujemna cena

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Cena produktu musi być większa od 0", exception.getMessage());
    }

    @Test
    void testValidateProductNullPrice() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName("Valid Name");
        // Brak ceny

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Cena produktu jest wymagana", exception.getMessage());
    }
}