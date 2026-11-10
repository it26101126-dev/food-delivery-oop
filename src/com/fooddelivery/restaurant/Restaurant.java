package com.fooddelivery.restaurant;

// Restaurant model class to store and manage restaurant details
public class Restaurant {
  private String id;

  // Returns the unique restaurant ID
  public String getId() {
    return id;
  }

  // Private attributes to store restaurant information
  private String name;
  private String cuisine;
  private String address;
  private String phone;
  private String image;
  private boolean open;

  // Constructor to initialize all restaurant details
  public Restaurant(
      String id,
      String name,
      String cuisine,
      String address,
      String phone,
      String image,
      boolean open) {
    this.id = id;
    this.name = name;
    this.cuisine = cuisine;
    this.address = address;
    this.phone = phone;
    this.image = image;
    this.open = open;
  }

  // Getter methods to access private restaurant attributes
  public String getName() {
    return name;
  }
  public String getCuisine() {
    return cuisine;
  }
  public String getAddress() {
    return address;
  }
  public String getPhone() {
    return phone;
  }
  public String getImage() {
    return image;
  }

  // Checks whether the restaurant is open or closed
  public boolean isOpen() {
    return open;
  }

  // Returns the restaurant name as a display label
  public String getLabel() {
    return name;
  }
}
