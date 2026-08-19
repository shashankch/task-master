package com.taskmaster.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail for ResourceNotFoundException")
    void handleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Task", "id", "123");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/tasks/123"));

        ProblemDetail result = exceptionHandler.handleResourceNotFound(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).contains("Task with id '123' not found");
        assertThat(result.getTitle()).isEqualTo("Resource Not Found");
        assertThat(result.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should return 409 ProblemDetail for ConflictException")
    void handleConflict() {
        ConflictException ex = new ConflictException("Email already registered");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/auth/register"));

        ProblemDetail result = exceptionHandler.handleConflict(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).isEqualTo("Email already registered");
        assertThat(result.getTitle()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail for BadRequestException")
    void handleBadRequest() {
        BadRequestException ex = new BadRequestException("Invalid status transition");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("PATCH", "/api/v1/tasks/1/status"));

        ProblemDetail result = exceptionHandler.handleBadRequest(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("Invalid status transition");
        assertThat(result.getTitle()).isEqualTo("Bad Request");
    }
}
