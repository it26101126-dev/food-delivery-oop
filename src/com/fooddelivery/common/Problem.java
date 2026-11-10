package com.fooddelivery.common;

// Keep an error message together with the response status code.
public class Problem extends RuntimeException {
  private final int status;

  // Save the status code and pass the message to the parent error class.
  public Problem(int status, String message) {
    super(message);
    this.status = status;
  }

  // Return the status code for this error.
  public int getStatus() {
    return status;
  }
}
