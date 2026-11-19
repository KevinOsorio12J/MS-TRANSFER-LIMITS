package com.kodea.transferlimits.api;

import com.kodea.transferlimits.domain.LimitViolation;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(LimitViolation.class)
  ProblemDetail handleLimitViolation(LimitViolation exception) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    detail.setType(URI.create("https://kodea.cloud/problems/" + exception.code().toLowerCase()));
    detail.setTitle("Transfer limit rejected");
    detail.setProperty("code", exception.code());
    return detail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
    detail.setType(URI.create("https://kodea.cloud/problems/invalid-request"));
    detail.setTitle("Invalid request");
    detail.setProperty("code", "INVALID_REQUEST");
    return detail;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid path or enum value.");
    detail.setType(URI.create("https://kodea.cloud/problems/invalid-parameter"));
    detail.setTitle("Invalid parameter");
    detail.setProperty("code", "INVALID_PARAMETER");
    return detail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleMalformedBody(HttpMessageNotReadableException exception) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is malformed.");
    detail.setType(URI.create("https://kodea.cloud/problems/malformed-request"));
    detail.setTitle("Malformed request");
    detail.setProperty("code", "MALFORMED_REQUEST");
    return detail;
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ProblemDetail handleMethodValidation(HandlerMethodValidationException exception) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
    detail.setType(URI.create("https://kodea.cloud/problems/invalid-request"));
    detail.setTitle("Invalid request");
    detail.setProperty("code", "INVALID_REQUEST");
    return detail;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    detail.setType(URI.create("https://kodea.cloud/problems/internal-error"));
    detail.setTitle("Internal server error");
    detail.setProperty("code", "INTERNAL_ERROR");
    return detail;
  }
}
