package com.fooddelivery.delivery;

public class Delivery {
  private String id;

  public String getId() {
    return id;
  }

  private String orderId;
  private String driverName;
  private String driverPhone;
  private String status;
  private String updatedAt;

  public Delivery(
      String id,
      String orderId,
      String driverName,
      String driverPhone,
      String status,
      String updatedAt) {
    this.id = id;
    this.orderId = orderId;
    this.driverName = driverName;
    this.driverPhone = driverPhone;
    this.status = status;
    this.updatedAt = updatedAt;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getDriverName() {
    return driverName;
  }

  public String getDriverPhone() {
    return driverPhone;
  }

  public String getStatus() {
    return status;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public String getLabel() {
    return driverName;
  }
}
