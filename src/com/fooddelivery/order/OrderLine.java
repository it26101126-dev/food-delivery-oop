package com.fooddelivery.order;

/** Stores the details of one food item in an order. */
public class OrderLine {
  private final String foodId;
  private final String name;
  private final int quantity;
  private final long unitCents;

  // Initializes the food item's details.
  public OrderLine(String foodId, String name, int quantity, long unitCents) {
    this.foodId = foodId;
    this.name = name;
    this.quantity = quantity;
    this.unitCents = unitCents;
  }

  // Getter methods return the values of the private fields.
  public String getFoodId() {
    return foodId;
  }

  public String getName() {
    return name;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getUnitCents() {
    return unitCents;
  }

  // Calculates the total price for this item.
  public long getTotalCents() {
    return quantity*unitCents;
  }
}
