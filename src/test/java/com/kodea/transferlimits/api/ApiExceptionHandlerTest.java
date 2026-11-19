package com.kodea.transferlimits.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kodea.transferlimits.domain.LimitViolation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapsLimitViolationsToRfc9457ProblemDetails() {
    var detail =
        handler.handleLimitViolation(new LimitViolation("DAILY_LIMIT_EXCEEDED", "Too much."));
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), detail.getStatus());
    assertEquals("DAILY_LIMIT_EXCEEDED", detail.getProperties().get("code"));
  }

  @Test
  void mapsInvalidBodyFieldsToProblemDetails() {
    var detail =
        handler.handleValidation(org.mockito.Mockito.mock(MethodArgumentNotValidException.class));
    assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
    assertEquals("INVALID_REQUEST", detail.getProperties().get("code"));
  }

  @Test
  void mapsInvalidEnumParametersToProblemDetails() {
    var exception =
        new MethodArgumentTypeMismatchException(
            "unknown",
            com.kodea.transferlimits.domain.TransferChannel.class,
            "channel",
            null,
            new IllegalArgumentException("unknown channel"));
    var detail = handler.handleTypeMismatch(exception);
    assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
    assertEquals("INVALID_PARAMETER", detail.getProperties().get("code"));
  }

  @Test
  void mapsMalformedJsonToProblemDetails() {
    var exception =
        new HttpMessageNotReadableException("bad json", Mockito.mock(HttpInputMessage.class));
    var detail = handler.handleMalformedBody(exception);
    assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
    assertEquals("MALFORMED_REQUEST", detail.getProperties().get("code"));
  }

  @Test
  void mapsMethodValidationFailuresToProblemDetails() {
    var detail =
        handler.handleMethodValidation(Mockito.mock(HandlerMethodValidationException.class));
    assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
    assertEquals("INVALID_REQUEST", detail.getProperties().get("code"));
  }

  @Test
  void hidesUnexpectedExceptionDetails() {
    var detail = handler.handleUnexpected(new IllegalStateException("secret"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), detail.getStatus());
    assertEquals("An unexpected error occurred.", detail.getDetail());
  }
}
