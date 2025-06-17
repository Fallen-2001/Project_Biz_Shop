
package org.example.service;

import org.example.dao.UserDaoInterface;
import org.example.model.Role;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDaoInterface userDao;

    // Usuń @InjectMocks i utwórz manualnie
    private AuthServiceImpl authService;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // Manualne utworzenie serwisu z mock'iem
        authService = new AuthServiceImpl();

        // Użyj refleksji lub setter do wstrzyknięcia mock'a
        // Albo zmień AuthServiceImpl żeby miał setter
        setUserDao(authService, userDao);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Test
    void testSuccessfulLogin() {
        // Given
        String username = "testuser";
        String password = "password123";
        when(userDao.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        User result = authService.login(username, password);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser, authService.getCurrentUser());
        verify(userDao, times(1)).findByUsername(username);
    }

    @Test
    void testLoginWithWrongPassword() {
        // Given
        String username = "testuser";
        String wrongPassword = "wrongpassword";
        when(userDao.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        User result = authService.login(username, wrongPassword);

        // Then
        assertNull(result);
        assertNull(authService.getCurrentUser());
        verify(userDao, times(1)).findByUsername(username);
    }

    @Test
    void testLoginWithNonExistentUser() {
        // Given
        String username = "nonexistent";
        String password = "password123";
        when(userDao.findByUsername(username)).thenReturn(Optional.empty());

        // When
        User result = authService.login(username, password);

        // Then
        assertNull(result);
        assertNull(authService.getCurrentUser());
        verify(userDao, times(1)).findByUsername(username);
    }

    @Test
    void testLoginWithNullCredentials() {
        // When
        User result1 = authService.login(null, "password");
        User result2 = authService.login("username", null);
        User result3 = authService.login(null, null);

        // Then
        assertNull(result1);
        assertNull(result2);
        assertNull(result3);
        verify(userDao, never()).findByUsername(anyString());
    }

    @Test
    void testSuccessfulRegistration() throws Exception {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("newuser@example.com");

        when(userDao.existsByUsername("newuser")).thenReturn(false);
        when(userDao.existsByEmail("newuser@example.com")).thenReturn(false);

        // When
        User result = authService.register(newUser);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals(Role.USER, result.getRole());
        verify(userDao, times(1)).save(newUser);
    }

    @Test
    void testRegistrationWithExistingUsername() {
        // Given
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("password123");

        when(userDao.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            authService.register(newUser);
        });

        assertEquals("Nazwa użytkownika już istnieje", exception.getMessage());
        verify(userDao, never()).save(any());
    }

    @Test
    void testIsUsernameAvailable() {
        // Given
        when(userDao.existsByUsername("available")).thenReturn(false);
        when(userDao.existsByUsername("taken")).thenReturn(true);

        // When & Then
        assertTrue(authService.isUsernameAvailable("available"));
        assertFalse(authService.isUsernameAvailable("taken"));
        assertFalse(authService.isUsernameAvailable(null));
        assertFalse(authService.isUsernameAvailable(""));
    }

    @Test
    void testIsLoggedIn() {
        // Initially not logged in
        assertFalse(authService.isLoggedIn());

        // After setting current user
        authService.setCurrentUser(testUser);
        assertTrue(authService.isLoggedIn());

        // After logout
        authService.logout();
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void testIsAdmin() {
        // Given
        User adminUser = new User();
        adminUser.setRole(Role.ADMIN);

        // Not logged in
        assertFalse(authService.isAdmin());

        // Regular user
        authService.setCurrentUser(testUser);
        assertFalse(authService.isAdmin());

        // Admin user
        authService.setCurrentUser(adminUser);
        assertTrue(authService.isAdmin());
    }

    @Test
    void testIsUser() {
        // Given
        User adminUser = new User();
        adminUser.setRole(Role.ADMIN);

        // Not logged in
        assertFalse(authService.isUser());

        // Regular user
        authService.setCurrentUser(testUser);
        assertTrue(authService.isUser());

        // Admin user
        authService.setCurrentUser(adminUser);
        assertFalse(authService.isUser());
    }

    // Helper method do wstrzykiwania UserDao
    private void setUserDao(AuthServiceImpl service, UserDaoInterface userDao) {
        try {
            var field = AuthServiceImpl.class.getDeclaredField("userDao");
            field.setAccessible(true);
            field.set(service, userDao);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject userDao", e);
        }
    }
}
