package com.taskmaster.shared.exception;

import com.taskmaster.shared.dto.ValidationError;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler translating domain and infrastructure exceptions to standard RFC 7807 ProblemDetail envelopes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/not-found"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        log.warn("Static resource or endpoint not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/not-found"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, WebRequest request) {
        log.warn("Conflict error: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Conflict");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/conflict"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockFailure(ObjectOptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Optimistic locking conflict on concurrent modification: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The resource was modified by another transaction. Please reload and retry."
        );
        problemDetail.setTitle("Concurrent Modification Conflict");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/optimistic-lock-conflict"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "A resource with the provided unique field already exists."
        );
        problemDetail.setTitle("Duplicate Resource");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/data-integrity"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/bad-request"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error on request: {}", ex.getMessage());
        List<ValidationError> errors = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    return new ValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()
                    );
                }
                return new ValidationError(
                    error.getObjectName(),
                    error.getDefaultMessage(),
                    null
                );
            })
            .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed with " + errors.size() + " error(s)"
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/validation-failed"));
        problemDetail.setProperty("violations", errors);
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex, WebRequest request) {
        log.warn("Forbidden access: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Access Forbidden");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/forbidden"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "You do not have permission to perform this action."
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/access-denied"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password"
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/unauthorized"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            ex.getMessage()
        );
        problemDetail.setTitle("Rate Limit Exceeded");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/rate-limit-exceeded"));
        enrichProblemDetail(problemDetail, request);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled internal server error: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal error occurred. Please contact support."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://taskmaster.dev/errors/internal-error"));
        enrichProblemDetail(problemDetail, request);
        return problemDetail;
    }

    private void enrichProblemDetail(ProblemDetail problemDetail, WebRequest request) {
        problemDetail.setProperty("timestamp", Instant.now().toString());
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
    }
}
