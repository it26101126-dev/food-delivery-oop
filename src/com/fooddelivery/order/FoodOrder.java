package com.fooddelivery.order;

import java.util.List;

public class FoodOrder {
  private String id;

  public String getId() {
    return id;
  }

  private final String customerId,
      restaurantId,
      customerName,
      address,
      phone,
      status,
      createdAt,
      requestKey;
  private final long deliveryCents;
  private final List<OrderLine> items;

  public FoodOrder(
      String id,
      String customerId,
      String restaurantId,
      String customerName,
      String address,
      String phone,
      String status,
      String createdAt,
      String requestKey,
      long deliveryCents,
      List<OrderLine> items) {
    this.id = id;
    this.customerId = customerId;
    this.restaurantId = restaurantId;
    this.customerName = customerName;
    this.address = address;
    this.phone = phone;
    this.status = status;
    this.createdAt = createdAt;
    this.requestKey = requestKey;
    this.deliveryCents = deliveryCents;
    this.items = List.copyOf(items);
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getRestaurantId() {
    return restaurantId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getAddress() {
    return address;
  }

  public String getPhone() {
    return phone;
  }

  public String getStatus() {
    return status;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public long getDeliveryCents() {
    return deliveryCents;
  }

  public List<OrderLine> getItems() {
    return items;
  }

  public long getSubtotalCents() {
    long total = 0;
    for (OrderLine line : items) total += line.getTotalCents();
    return total;
  }

  public long getTotalCents() {
    return getSubtotalCents() + deliveryCents;
  }

  public String getLabel() {
    return "Order " + getId();
  }
}
