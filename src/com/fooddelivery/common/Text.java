package com.fooddelivery.common;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

public final class Text {
  private Text() {}

  public static String required(String value, String label, int max) {
    if (value == null || value.trim().isEmpty())
      throw new IllegalArgumentException(label + " is required.");
    String result = value.trim();
    if (result.length() > max) throw new IllegalArgumentException(label + " is too long.");
    return result;
  }

  public static String email(String value) {
    String email = required(value, "Email", 150).toLowerCase(Locale.ROOT);
    if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
      throw new IllegalArgumentException("Enter a valid email.");
    return email;
  }

  public static String phone(String value) {
    String phone = required(value, "Phone", 16);
    if (!phone.matches("[+]?[0-9]{9,15}"))
      throw new IllegalArgumentException("Phone must contain 9 to 15 digits (optional +).");
    return phone;
  }

  public static String id(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }

  public static long cents(String value) {
    try {
      long result = new BigDecimal(value).movePointRight(2).longValueExact();
      if (result <= 0 || result > 10_000_000) throw new NumberFormatException();
      return result;
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          "Enter a price from 0.01 to 100000.00 (maximum 2 decimals).");
    }
  }
}
