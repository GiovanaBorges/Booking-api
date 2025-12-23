package com.booking.booking.config;


import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.booking.booking.repositories.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;


@ControllerAdvice
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    private final IdempotencyRepository repo;
    private final ObjectMapper mapper;

    public IdempotencyResponseAdvice(IdempotencyRepository repo,
                                     ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType contentType,
                                  Class converterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        HttpServletRequest http = ((ServletServerHttpRequest) request).getServletRequest();

        if (!"POST".equals(http.getMethod())) return body;

        String key = (String) http.getAttribute("IDEMP_KEY");
        if (key == null) return body;

        try {
            String json = mapper.writeValueAsString(body);
            repo.save(key, json);
        } catch (Exception ignored) {}

        return body;
    }
}
