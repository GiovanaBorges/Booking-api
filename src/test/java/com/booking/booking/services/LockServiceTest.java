package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import com.booking.booking.exceptions.ApiException;

@ExtendWith(MockitoExtension.class)
public class LockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private LockService lockService;

    @Test
    void shouldExecuteActionWhenKeyDoesNotExist() {

        // GIVEN
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(
                eq("idempotency:test-key"),
                eq("LOCK"),
                any(Duration.class))).thenReturn(true);

        // WHEN
        String result = lockService.execute("test-key", () -> "OK");

        // THEN
        assertEquals("OK", result);

        verify(redisTemplate).opsForValue();
        verify(valueOps).setIfAbsent(
                eq("idempotency:test-key"),
                eq("LOCK"),
                any(Duration.class));
    }

    @Test
    void shouldThrowConflictWhenKeyAlreadyExists() {
        // GIVEN
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(
                eq("idempotency:test-key"),
                eq("LOCK"),
                any(Duration.class))).thenReturn(false);

        // WHEN
        ApiException ex = assertThrows(ApiException.class,
                () -> lockService.execute("test-key", () -> "FAIL"));

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Request already processed", ex.getMessage());

        verify(redisTemplate).opsForValue();
        verify(valueOps).setIfAbsent(
                eq("idempotency:test-key"),
                eq("LOCK"),
                any(Duration.class));
    }

    @Test
    void shouldExecuteActionWhenKeyIsNull() {

        String result = lockService.execute(null, () -> "OK");

        assertEquals("OK", result);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldExecuteActionWhenKeyIsBlank() {

        String result = lockService.execute("   ", () -> "OK");

        assertEquals("OK", result);
        verifyNoInteractions(redisTemplate);
    }
}
