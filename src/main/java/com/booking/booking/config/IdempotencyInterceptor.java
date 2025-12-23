package com.booking.booking.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.booking.booking.repositories.IdempotencyRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyRepository repo;

    public static final String HEADER = "Idempotency-Key";

    public IdempotencyInterceptor(IdempotencyRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        if (!"POST".equals(request.getMethod())) return true;

        String key = request.getHeader(HEADER);

        if (key == null || key.isBlank()) {
            response.sendError(400, "Missing Idempotency-Key");
            return false;
        }

        Optional<String> cached = repo.get(key);

        if (cached.isPresent()) {
            response.setContentType("application/json");
            response.getWriter().write(cached.get());
            return false;
        }

        request.setAttribute("IDEMP_KEY", key);
        return true;
    }
}
