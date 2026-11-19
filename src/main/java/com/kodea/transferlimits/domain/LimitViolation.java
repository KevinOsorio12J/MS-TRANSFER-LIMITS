package com.kodea.transferlimits.domain;

public class LimitViolation extends RuntimeException {
  private final String code;

  public LimitViolation(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
