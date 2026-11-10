package com.fooddelivery.customer;

// Store the details for one customer account.
public class Customer {
  // Keep the customer ID private and provide it through a getter.
  private String id;

  public String getId() {
    return id;
  }

  private String name;
  private String email;
  private String phone;
  private String address;
  private String passwordHash;
  private String role;

  // Set all customer details when creating a customer.
  public Customer(
      String id,
      String name,
      String email,
      String phone,
      String address,
      String passwordHash,
      String role) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  // These methods let other classes read private customer details.
  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddress() {
    return address;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getRole() {
    return role;
  }

  public String getLabel() {
    return name;
  }
}
