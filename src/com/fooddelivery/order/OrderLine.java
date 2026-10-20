package com.fooddelivery.order;

public class OrderLine {
  private final String foodId;
  private final String name;
  private final int quantity;
  private final long unitCents;

  public OrderLine(String foodId, String name, int quantity, long unitCents) {
    this.foodId = foodId;
    this.name = name;
    this.quantity = quantity;
    this.unitCents = unitCents;
  }

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

  public long getTotalCents() {
    return quantity * unitCents;
  }
}
