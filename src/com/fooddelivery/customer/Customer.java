package com.fooddelivery.customer;

public class Customer {
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
