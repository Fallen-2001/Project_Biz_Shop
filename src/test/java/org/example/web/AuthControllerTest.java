package org.example.web;

import org.example.model.Role;
import org.example.model.User;
import org.example.service.AuthServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthServiceInterface authService;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        // Set up controller properties
        authController.setUsername("testuser");
        authController.setPassword("password123");
    }

    @Test
    void testSuccessfulLoginAsUser() {
        // Given
        when(authService.login("testuser", "password123")).thenReturn(testUser);

        // When
        String result = authController.login();

        // Then
        assertEquals("/user/products.xhtml?faces-redirect=true", result);
        verify(authService).login("testuser", "password123");
    }

    @Test
    void testSuccessfulLoginAsAdmin() {
        // Given
        when(authService.login("testuser", "password123")).thenReturn(adminUser);

        // When
        String result = authController.login();

        // Then
        assertEquals("/admin/products.xhtml?faces-redirect=true", result);
        verify(authService).login("testuser", "password123");
    }

    @Test
    void testFailedLogin() {
        // Given
        when(authService.login("testuser", "password123")).thenReturn(null);

        // When
        String result = authController.login();

        // Then
        assertEquals("/login.xhtml?error=true&faces-redirect=true", result);
        verify(authService).login("testuser", "password123");
    }

    @Test
    void testLoginWithException() {
        // Given
        when(authService.login("testuser", "password123")).thenThrow(new RuntimeException("Database error"));

        // When
        String result = authController.login();

        // Then
        assertEquals("/login.xhtml?error=true&faces-redirect=true", result);
        verify(authService).login("testuser", "password123");
    }

    @Test
    void testLogoutWithUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(testUser);

        // When
        String result = authController.logout();

        // Then
        assertEquals("/login.xhtml?faces-redirect=true", result);
        verify(authService).getCurrentUser();
        verify(authService).logout();
    }

    @Test
    void testLogoutWithoutUser() {
        // Given
        when(authService.getCurrentUser()).thenReturn(null);

        // When
        String result = authController.logout();

        // Then
        assertEquals("/login.xhtml?faces-redirect=true", result);
        verify(authService).getCurrentUser();
        verify(authService).logout();
    }

    @Test
    void testGetUsername() {
        // Given
        authController.setUsername("testuser");

        // When
        String username = authController.getUsername();

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void testSetUsername() {
        // When
        authController.setUsername("newuser");

        // Then
        assertEquals("newuser", authController.getUsername());
    }

    @Test
    void testGetPassword() {
        // Given
        authController.setPassword("testpassword");

        // When
        String password = authController.getPassword();

        // Then
        assertEquals("testpassword", password);
    }

    @Test
    void testSetPassword() {
        // When
        authController.setPassword("newpassword");

        // Then
        assertEquals("newpassword", authController.getPassword());
    }

    @Test
    void testLoginWithNullCredentials() {
        // Given
        authController.setUsername(null);
        authController.setPassword(null);
        when(authService.login(null, null)).thenReturn(null);

        // When
        String result = authController.login();

        // Then
        assertEquals("/login.xhtml?error=true&faces-redirect=true", result);
        verify(authService).login(null, null);
    }

    @Test
    void testLoginWithEmptyCredentials() {
        // Given
        authController.setUsername("");
        authController.setPassword("");
        when(authService.login("", "")).thenReturn(null);

        // When
        String result = authController.login();

        // Then
        assertEquals("/login.xhtml?error=true&faces-redirect=true", result);
        verify(authService).login("", "");
    }

    @Test
    void testLoginFlow() {
        // Test complete login flow

        // 1. Initial state - no credentials
        authController.setUsername(null);
        authController.setPassword(null);
        assertNull(authController.getUsername());
        assertNull(authController.getPassword());

        // 2. Set credentials
        authController.setUsername("testuser");
        authController.setPassword("password123");
        assertEquals("testuser", authController.getUsername());
        assertEquals("password123", authController.getPassword());

        // 3. Successful login
        when(authService.login("testuser", "password123")).thenReturn(testUser);
        String loginResult = authController.login();
        assertEquals("/user/products.xhtml?faces-redirect=true", loginResult);

        // 4. Logout
        when(authService.getCurrentUser()).thenReturn(testUser);
        String logoutResult = authController.logout();
        assertEquals("/login.xhtml?faces-redirect=true", logoutResult);

        // Verify all interactions
        verify(authService).login("testuser", "password123");
        verify(authService).getCurrentUser();
        verify(authService).logout();
    }

    @Test
    void testAdminRedirection() {
        // Given admin credentials
        authController.setUsername("admin");
        authController.setPassword("adminpass");

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        when(authService.login("admin", "adminpass")).thenReturn(admin);

        // When
        String result = authController.login();

        // Then
        assertEquals("/admin/products.xhtml?faces-redirect=true", result);
        verify(authService).login("admin", "adminpass");
    }

    @Test
    void testUserRedirection() {
        // Given user credentials
        authController.setUsername("user");
        authController.setPassword("userpass");

        // Create regular user
        User user = new User();
        user.setUsername("user");
        user.setRole(Role.USER);

        when(authService.login("user", "userpass")).thenReturn(user);

        // When
        String result = authController.login();

        // Then
        assertEquals("/user/products.xhtml?faces-redirect=true", result);
        verify(authService).login("user", "userpass");
    }

    @Test
    void testMultipleLoginAttempts() {
        // First attempt - failure
        authController.setUsername("user");
        authController.setPassword("wrongpass");
        when(authService.login("user", "wrongpass")).thenReturn(null);

        String firstResult = authController.login();
        assertEquals("/login.xhtml?error=true&faces-redirect=true", firstResult);

        // Second attempt - success
        authController.setPassword("correctpass");
        when(authService.login("user", "correctpass")).thenReturn(testUser);

        String secondResult = authController.login();
        assertEquals("/user/products.xhtml?faces-redirect=true", secondResult);

        // Verify both attempts
        verify(authService).login("user", "wrongpass");
        verify(authService).login("user", "correctpass");
    }

    @Test
    void testAuthServiceIntegration() {
        // Test that controller properly delegates to auth service

        // Test login delegation
        when(authService.login("test", "pass")).thenReturn(testUser);
        authController.setUsername("test");
        authController.setPassword("pass");
        authController.login();
        verify(authService).login("test", "pass");

        // Test logout delegation
        authController.logout();
        verify(authService).logout();
        verify(authService).getCurrentUser();
    }
}