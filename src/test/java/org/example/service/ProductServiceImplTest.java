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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceImplTest {

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
        testProduct.setCategory("Electronics");
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
        List<Product> products = Arrays.asList(testProduct);
        when(productDao.findAll()).thenReturn(products);

        // When
        List<Product> result = productService.getAllProducts();

        // Then
        assertEquals(1, result.size());
        assertEquals(testProduct, result.get(0));
        verify(productDao).findAll();
    }

    @Test
    void testGetActiveProducts() {
        // Given
        List<Product> activeProducts = Arrays.asList(testProduct);
        when(productDao.findActiveProducts()).thenReturn(activeProducts);

        // When
        List<Product> result = productService.getActiveProducts();

        // Then
        assertEquals(1, result.size());
        assertEquals(testProduct, result.get(0));
        verify(productDao).findActiveProducts();
    }

    @Test
    void testGetProductsByCategory() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productDao.findByCategory("Electronics")).thenReturn(products);

        // When
        List<Product> result = productService.getProductsByCategory("Electronics");

        // Then
        assertEquals(1, result.size());
        assertEquals(testProduct, result.get(0));
        verify(productDao).findByCategory("Electronics");
    }

    @Test
    void testGetProductsByCategoryEmpty() {
        // Given
        when(productDao.findActiveProducts()).thenReturn(Arrays.asList(testProduct));

        // When
        List<Product> result = productService.getProductsByCategory("");

        // Then
        assertEquals(1, result.size());
        verify(productDao).findActiveProducts();
        verify(productDao, never()).findByCategory(any());
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
        verify(productDao).findById(1L);
    }

    @Test
    void testGetProductByIdNotFound() {
        // Given
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Product> result = productService.getProductById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(productDao).findById(999L);
    }

    @Test
    void testGetProductByIdNull() {
        // When
        Optional<Product> result = productService.getProductById(null);

        // Then
        assertFalse(result.isPresent());
        verify(productDao, never()).findById(any());
    }

    @Test
    void testSearchProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productDao.searchByName("Test")).thenReturn(products);

        // When
        List<Product> result = productService.searchProducts("Test");

        // Then
        assertEquals(1, result.size());
        assertEquals(testProduct, result.get(0));
        verify(productDao).searchByName("Test");
    }

    @Test
    void testSearchProductsEmpty() {
        // Given
        when(productDao.findActiveProducts()).thenReturn(Arrays.asList(testProduct));

        // When
        List<Product> result = productService.searchProducts("");

        // Then
        assertEquals(1, result.size());
        verify(productDao).findActiveProducts();
        verify(productDao, never()).searchByName(any());
    }

    @Test
    void testAddProductSuccess() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("49.99"));
        // Nie ustawiamy stockQuantity - zostanie ustawione przez serwis

        // When
        productService.addProduct(newProduct);

        // Then
        verify(productDao).save(newProduct);
        assertEquals(10, newProduct.getStockQuantity()); // Sprawdź czy została ustawiona domyślna wartość
        assertEquals("Różne", newProduct.getCategory()); // Domyślna kategoria
        assertTrue(newProduct.isActive()); // Domyślnie aktywny
        assertNotNull(newProduct.getDescription()); // Domyślny opis
    }

    @Test
    void testAddProductWithExistingStockQuantity() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("49.99"));
        newProduct.setStockQuantity(25); // Ustawiamy konkretną wartość

        // When
        productService.addProduct(newProduct);

        // Then
        verify(productDao).save(newProduct);
        assertEquals(25, newProduct.getStockQuantity()); // Powinna zostać zachowana
    }

    @Test
    void testAddProductWithZeroStock() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(new BigDecimal("49.99"));
        newProduct.setStockQuantity(0); // Zero stock

        // When
        productService.addProduct(newProduct);

        // Then
        verify(productDao).save(newProduct);
        assertEquals(10, newProduct.getStockQuantity()); // Powinna zostać ustawiona domyślna wartość 10
    }

    @Test
    void testAddProductNotAdmin() {
        // Given
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Product newProduct = new Product();

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

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(newProduct);
        });

        assertEquals("Musisz być zalogowany", exception.getMessage());
        verify(productDao, never()).save(any());
    }

    @Test
    void testAddProductInvalidName() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product newProduct = new Product();
        newProduct.setName("ab"); // Too short
        newProduct.setPrice(new BigDecimal("49.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(newProduct);
        });

        assertEquals("Nazwa produktu musi mieć co najmniej 3 znaki", exception.getMessage());
        verify(productDao, never()).save(any());
    }

    @Test
    void testAddProductInvalidPrice() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product newProduct = new Product();
        newProduct.setName("Valid Product Name");
        newProduct.setPrice(BigDecimal.ZERO); // Invalid price

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(newProduct);
        });

        assertEquals("Cena produktu musi być większa od 0", exception.getMessage());
        verify(productDao, never()).save(any());
    }

    @Test
    void testUpdateProductSuccess() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        testProduct.setId(1L);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        productService.updateProduct(testProduct);

        // Then
        verify(productDao).update(testProduct);
        verify(productDao).findById(1L);
    }

    @Test
    void testUpdateProductNotFound() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        testProduct.setId(999L);
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.updateProduct(testProduct);
        });

        assertEquals("Produkt nie został znaleziony", exception.getMessage());
        verify(productDao, never()).update(any());
    }

    @Test
    void testUpdateProductNoId() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        testProduct.setId(null);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.updateProduct(testProduct);
        });

        assertEquals("ID produktu jest wymagane do aktualizacji", exception.getMessage());
        verify(productDao, never()).update(any());
    }

    @Test
    void testUpdateProductNotAdmin() {
        // Given
        when(authService.getCurrentUser()).thenReturn(regularUser);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.updateProduct(testProduct);
        });

        assertEquals("Tylko administrator może wykonać tę operację", exception.getMessage());
        verify(productDao, never()).update(any());
    }

    @Test
    void testDeleteProductSuccess() throws Exception {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productDao).delete(1L);
        verify(productDao).findById(1L);
    }

    @Test
    void testDeleteProductNotFound() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.deleteProduct(999L);
        });

        assertEquals("Produkt nie został znaleziony", exception.getMessage());
        verify(productDao, never()).delete(any());
    }

    @Test
    void testDeleteProductNullId() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.deleteProduct(null);
        });

        assertEquals("ID produktu jest wymagane", exception.getMessage());
        verify(productDao, never()).delete(any());
    }

    @Test
    void testDeleteProductNotAdmin() {
        // Given
        when(authService.getCurrentUser()).thenReturn(regularUser);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.deleteProduct(1L);
        });

        assertEquals("Tylko administrator może wykonać tę operację", exception.getMessage());
        verify(productDao, never()).delete(any());
    }

    @Test
    void testIsProductAvailable() {
        // Given
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean available = productService.isProductAvailable(1L, 5);

        // Then
        assertTrue(available);
        verify(productDao).findById(1L);
    }

    @Test
    void testIsProductAvailableInsufficientStock() {
        // Given
        testProduct.setStockQuantity(3);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean available = productService.isProductAvailable(1L, 5);

        // Then
        assertFalse(available);
    }

    @Test
    void testIsProductAvailableInactive() {
        // Given
        testProduct.setActive(false);
        when(productDao.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean available = productService.isProductAvailable(1L, 1);

        // Then
        assertFalse(available);
    }

    @Test
    void testIsProductAvailableNotFound() {
        // Given
        when(productDao.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean available = productService.isProductAvailable(999L, 1);

        // Then
        assertFalse(available);
    }

    @Test
    void testValidateProductNullName() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName(null);
        invalidProduct.setPrice(new BigDecimal("49.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Nazwa produktu jest wymagana", exception.getMessage());
    }

    @Test
    void testValidateProductNullPrice() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName("Valid Name");
        invalidProduct.setPrice(null);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Cena produktu jest wymagana", exception.getMessage());
    }

    @Test
    void testValidateProductNegativeStock() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName("Valid Name");
        invalidProduct.setPrice(new BigDecimal("49.99"));
        invalidProduct.setStockQuantity(-5);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Stan magazynowy nie może być ujemny", exception.getMessage());
    }

    @Test
    void testValidateProductTooLongName() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName("a".repeat(300)); // Too long
        invalidProduct.setPrice(new BigDecimal("49.99"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("Nazwa produktu jest za długa (max 255 znaków)", exception.getMessage());
    }

    @Test
    void testValidateProductInvalidImageUrl() {
        // Given
        when(authService.getCurrentUser()).thenReturn(adminUser);
        Product invalidProduct = new Product();
        invalidProduct.setName("Valid Name");
        invalidProduct.setPrice(new BigDecimal("49.99"));
        invalidProduct.setImageUrl("invalid-url");

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            productService.addProduct(invalidProduct);
        });

        assertEquals("URL obrazka musi zaczynać się od http:// lub https://", exception.getMessage());
    }
}