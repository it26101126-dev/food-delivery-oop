package com.fooddelivery.review;

// Represents one review submitted by a customer for an order.
public class Review {

  // Unique ID used to find, edit, or delete this review.
  private String id;

  public String getId() {
    return id;
  }

  private String customerId;   // Identifies the customer who wrote the review.
  private String orderId;      // Identifies the order being reviewed.
  private String restaurantId; // Identifies the restaurant that fulfilled the order.
  private int rating;          // Stores the number of stars.
  private String comment;      // Stores the customer's written feedback.
  private String createdAt;    // Stores when the review was first created.

  // Runs when ReviewManager creates a new Review object.
  public Review(
      String id,
      String customerId,
      String orderId,
      String restaurantId,
      int rating,
      String comment,
      String createdAt) {

    // 'this' refers to the fields of the current Review object.
    this.id = id;
    this.customerId = customerId;
    this.orderId = orderId;
    this.restaurantId = restaurantId;
    this.rating = rating;
    this.comment = comment;
    this.createdAt = createdAt;
  }

  // Getters allow other classes to read the private fields.
  public String getCustomerId() {
    return customerId;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getRestaurantId() {
    return restaurantId;
  }

  public int getRating() {
    return rating;
  }

  public String getComment() {
    return comment;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  // Returns the review comment as its label.
  public String getLabel() {
    return comment;
  }
}
