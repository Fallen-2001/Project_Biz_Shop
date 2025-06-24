package org.example.service;

import org.example.dao.UserDaoInterface;
import org.example.model.Role;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserDaoInterface userDao;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Test
    void testSuccessfulLogin() {
        // Given
        when(userDao.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = authService.login("testuser", "password123");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(testUser, authService.getCurrentUser());
        verify(userDao).findByUsername("testuser");
    }

    @Test
    void testLoginWithInvalidPassword() {
        // Given
        when(userDao.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = authService.login("testuser", "wrongpassword");

        // Then
        assertNull(result);
        assertNull(authService.getCurrentUser());
    }

    @Test
    void testLoginWithNonExistentUser() {
        // Given
        when(userDao.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        User result = authService.login("nonexistent", "password123");

        // Then
        assertNull(result);
        assertNull(authService.getCurrentUser());
    }

    @Test
    void testLoginWithNullCredentials() {
        // When & Then
        assertNull(authService.login(null, "password"));
        assertNull(authService.login("username", null));
        assertNull(authService.login(null, null));
    }

    @Test
    void testSuccessfulRegistration() throws Exception {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("new@example.com");

        when(userDao.existsByUsername("newuser")).thenReturn(false);
        when(userDao.existsByEmail("new@example.com")).thenReturn(false);
        doNothing().when(userDao).save(any(User.class));

        // When
        User result = authService.register(newUser);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals(Role.USER, result.getRole());
        assertTrue(passwordEncoder.matches("password123", result.getPassword()));
        verify(userDao).save(newUser);
    }

    @Test
    void testRegistrationWithExistingUsername() {
        // Given
        User newUser = new User();
        newUser.setUsername("existinguser");
        newUser.setPassword("password123");

        when(userDao.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            authService.register(newUser);
        });

        assertEquals("Nazwa użytkownika już istnieje", exception.getMessage());
        verify(userDao, never()).save(any());
    }

    @Test
    void testRegistrationWithExistingEmail() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("existing@example.com");

        when(userDao.existsByUsername("newuser")).thenReturn(false);
        when(userDao.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            authService.register(newUser);
        });

        assertEquals("Email już jest używany", exception.getMessage());
        verify(userDao, never()).save(any());
    }

    @Test
    void testRegistrationWithInvalidData() {
        // Given
        User invalidUser = new User();
        invalidUser.setUsername("ab"); // Too short
        invalidUser.setPassword("123"); // Too short

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            authService.register(invalidUser);
        });

        assertTrue(exception.getMessage().contains("musi mieć co najmniej"));
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
        assertFalse(authService.isUsernameAvailable("   "));
    }

    @Test
    void testIsEmailAvailable() {
        // Given
        when(userDao.existsByEmail("available@test.com")).thenReturn(false);
        when(userDao.existsByEmail("taken@test.com")).thenReturn(true);

        // When & Then
        assertTrue(authService.isEmailAvailable("available@test.com"));
        assertFalse(authService.isEmailAvailable("taken@test.com"));
        assertTrue(authService.isEmailAvailable(null)); // Email is optional
        assertTrue(authService.isEmailAvailable(""));
        assertTrue(authService.isEmailAvailable("   "));
    }

    @Test
    void testLogout() {
        // Given
        authService.setCurrentUser(testUser);
        assertTrue(authService.isLoggedIn());

        // When
        authService.logout();

        // Then
        assertFalse(authService.isLoggedIn());
        assertNull(authService.getCurrentUser());
    }

    @Test
    void testIsLoggedIn() {
        // When no user is set
        assertFalse(authService.isLoggedIn());

        // When user is set
        authService.setCurrentUser(testUser);
        assertTrue(authService.isLoggedIn());
    }

    @Test
    void testIsAdmin() {
        // Given admin user
        User admin = new User();
        admin.setRole(Role.ADMIN);

        // When no user
        assertFalse(authService.isAdmin());

        // When regular user
        authService.setCurrentUser(testUser);
        assertFalse(authService.isAdmin());

        // When admin user
        authService.setCurrentUser(admin);
        assertTrue(authService.isAdmin());
    }

    @Test
    void testIsUser() {
        // Given admin user
        User admin = new User();
        admin.setRole(Role.ADMIN);

        // When no user
        assertFalse(authService.isUser());

        // When regular user
        authService.setCurrentUser(testUser);
        assertTrue(authService.isUser());

        // When admin user
        authService.setCurrentUser(admin);
        assertFalse(authService.isUser());
    }

    @Test
    void testSetCurrentUser() {
        // When
        authService.setCurrentUser(testUser);

        // Then
        assertEquals(testUser, authService.getCurrentUser());
        assertTrue(authService.isLoggedIn());
    }
}