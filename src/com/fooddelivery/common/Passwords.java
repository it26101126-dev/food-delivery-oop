package com.fooddelivery.common;

import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Shared security helper. Business modules only call hash() and matches().
public class Passwords {
  private static final int ROUNDS = 600000;

  public static String hash(String password) {
    if (password == null || password.length() < 8 || password.length() > 128)
      throw new IllegalArgumentException("Password must have 8 to 128 characters.");
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt));
  }

  public static boolean matches(String password, String saved) {
    if (password == null || password.length() > 128 || saved == null) return false;
    String[] parts = saved.split(":");
    if (parts.length != 2) return false;
    try {
      byte[] salt = Base64.getDecoder().decode(parts[0]);
      byte[] expected = Base64.getDecoder().decode(parts[1]);
      return MessageDigest.isEqual(expected, derive(password, salt));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt) {
    PBEKeySpec key = new PBEKeySpec(password.toCharArray(), salt, ROUNDS, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Password hashing unavailable.", e);
    } finally {
      key.clearPassword();
    }
  }
}
