package org.example.dao;

import org.example.model.Role;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDaoImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<User> userQuery;

    @Mock
    private TypedQuery<Long> longQuery;

    @InjectMocks
    private UserDaoImpl userDao;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("hashedpassword");
        testUser.setRole(Role.USER);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Test
    void testFindByIdSuccess() {
        // Given
        when(entityManager.find(User.class, 1L)).thenReturn(testUser);

        // When
        Optional<User> result = userDao.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(entityManager).find(User.class, 1L);
    }

    @Test
    void testFindByIdNotFound() {
        // Given
        when(entityManager.find(User.class, 999L)).thenReturn(null);

        // When
        Optional<User> result = userDao.findById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(entityManager).find(User.class, 999L);
    }

    @Test
    void testFindByIdNull() {
        // When
        Optional<User> result = userDao.findById(null);

        // Then
        assertFalse(result.isPresent());
        verify(entityManager, never()).find(any(), any());
    }

    @Test
    void testFindByUsernameSuccess() {
        // Given
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("username", "testuser")).thenReturn(userQuery);
        when(userQuery.getResultStream()).thenReturn(Stream.of(testUser));

        // When
        Optional<User> result = userDao.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userQuery).setParameter("username", "testuser");
    }

    @Test
    void testFindByUsernameNotFound() {
        // Given
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("username", "nonexistent")).thenReturn(userQuery);
        when(userQuery.getResultStream()).thenReturn(Stream.empty());

        // When
        Optional<User> result = userDao.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userQuery).setParameter("username", "nonexistent");
    }

    @Test
    void testFindByUsernameNull() {
        // When
        Optional<User> result = userDao.findByUsername(null);

        // Then
        assertFalse(result.isPresent());
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testFindByUsernameEmpty() {
        // When
        Optional<User> result = userDao.findByUsername("");

        // Then
        assertFalse(result.isPresent());
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testFindByUsernameWhitespace() {
        // When
        Optional<User> result = userDao.findByUsername("   ");

        // Then
        assertFalse(result.isPresent());
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testFindByEmailSuccess() {
        // Given
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("email", "test@example.com")).thenReturn(userQuery);
        when(userQuery.getResultStream()).thenReturn(Stream.of(testUser));

        // When
        Optional<User> result = userDao.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userQuery).setParameter("email", "test@example.com");
    }

    @Test
    void testFindByEmailNotFound() {
        // Given
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("email", "notfound@example.com")).thenReturn(userQuery);
        when(userQuery.getResultStream()).thenReturn(Stream.empty());

        // When
        Optional<User> result = userDao.findByEmail("notfound@example.com");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEmailNull() {
        // When
        Optional<User> result = userDao.findByEmail(null);

        // Then
        assertFalse(result.isPresent());
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testFindAll() {
        // Given
        List<User> users = Arrays.asList(testUser, new User());
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.getResultList()).thenReturn(users);

        // When
        List<User> result = userDao.findAll();

        // Then
        assertEquals(2, result.size());
        assertEquals(users, result);
        verify(userQuery).getResultList();
    }

    @Test
    void testSaveSuccess() {
        // When
        userDao.save(testUser);

        // Then
        verify(entityManager).persist(testUser);
        verify(entityManager).flush();
    }

    @Test
    void testSaveNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userDao.save(null);
        });

        assertEquals("User cannot be null", exception.getMessage());
        verify(entityManager, never()).persist(any());
    }

    @Test
    void testUpdateSuccess() {
        // When
        userDao.update(testUser);

        // Then
        verify(entityManager).merge(testUser);
        verify(entityManager).flush();
    }

    @Test
    void testUpdateNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userDao.update(null);
        });

        assertEquals("User cannot be null", exception.getMessage());
        verify(entityManager, never()).merge(any());
    }

    @Test
    void testDeleteSuccess() {
        // Given
        when(entityManager.find(User.class, 1L)).thenReturn(testUser);

        // When
        userDao.delete(1L);

        // Then
        verify(entityManager).find(User.class, 1L);
        verify(entityManager).remove(testUser);
        verify(entityManager).flush();
    }

    @Test
    void testDeleteNotFound() {
        // Given
        when(entityManager.find(User.class, 999L)).thenReturn(null);

        // When
        userDao.delete(999L);

        // Then
        verify(entityManager).find(User.class, 999L);
        verify(entityManager, never()).remove(any());
        verify(entityManager, never()).flush();
    }

    @Test
    void testDeleteNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userDao.delete(null);
        });

        assertEquals("User ID cannot be null", exception.getMessage());
        verify(entityManager, never()).find(any(), any());
    }

    @Test
    void testExistsByUsernameTrue() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("username", "testuser")).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(1L);

        // When
        boolean exists = userDao.existsByUsername("testuser");

        // Then
        assertTrue(exists);
        verify(longQuery).setParameter("username", "testuser");
    }

    @Test
    void testExistsByUsernameFalse() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("username", "nonexistent")).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(0L);

        // When
        boolean exists = userDao.existsByUsername("nonexistent");

        // Then
        assertFalse(exists);
        verify(longQuery).setParameter("username", "nonexistent");
    }

    @Test
    void testExistsByUsernameNull() {
        // When
        boolean exists = userDao.existsByUsername(null);

        // Then
        assertFalse(exists);
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testExistsByUsernameEmpty() {
        // When
        boolean exists = userDao.existsByUsername("");

        // Then
        assertFalse(exists);
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testExistsByEmailTrue() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("email", "test@example.com")).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(1L);

        // When
        boolean exists = userDao.existsByEmail("test@example.com");

        // Then
        assertTrue(exists);
        verify(longQuery).setParameter("email", "test@example.com");
    }

    @Test
    void testExistsByEmailFalse() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("email", "notfound@example.com")).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(0L);

        // When
        boolean exists = userDao.existsByEmail("notfound@example.com");

        // Then
        assertFalse(exists);
        verify(longQuery).setParameter("email", "notfound@example.com");
    }

    @Test
    void testExistsByEmailNull() {
        // When
        boolean exists = userDao.existsByEmail(null);

        // Then
        assertFalse(exists);
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testExistsByEmailEmpty() {
        // When
        boolean exists = userDao.existsByEmail("");

        // Then
        assertFalse(exists);
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    void testFindByUsernameWithTrimming() {
        // Given
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("username", "testuser")).thenReturn(userQuery);
        when(userQuery.getResultStream()).thenReturn(Stream.of(testUser));

        // When
        Optional<User> result = userDao.findByUsername("  testuser  ");

        // Then
        assertTrue(result.isPresent());
        verify(userQuery).setParameter("username", "testuser"); // Should be trimmed
    }

    @Test
    void testFindByEmailWithTrimming() {
        // Given
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("email", "test@example.com")).thenReturn(userQuery);
        when(userQuery.getResultStream()).thenReturn(Stream.of(testUser));

        // When
        Optional<User> result = userDao.findByEmail("  test@example.com  ");

        // Then
        assertTrue(result.isPresent());
        verify(userQuery).setParameter("email", "test@example.com"); // Should be trimmed
    }

    @Test
    void testExistsByUsernameWithTrimming() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("username", "testuser")).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(1L);

        // When
        boolean exists = userDao.existsByUsername("  testuser  ");

        // Then
        assertTrue(exists);
        verify(longQuery).setParameter("username", "testuser"); // Should be trimmed
    }

    @Test
    void testExistsByEmailWithTrimming() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter("email", "test@example.com")).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(1L);

        // When
        boolean exists = userDao.existsByEmail("  test@example.com  ");

        // Then
        assertTrue(exists);
        verify(longQuery).setParameter("email", "test@example.com"); // Should be trimmed
    }
}