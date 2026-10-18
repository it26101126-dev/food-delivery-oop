package com.fooddelivery.common;

public class Problem extends RuntimeException {
  private final int status;

  public Problem(int status, String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
