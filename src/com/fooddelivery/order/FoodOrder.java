package com.fooddelivery.order;

import java.util.List;

/** Stores the details of a food order. */
public class FoodOrder {
  // Unique order identifier.
  private String id;

  public String getId() {
    return id;
  }

  // Order and customer details.
  private final String customerId,restaurantId,customerName,address,phone,status,createdAt,requestKey;
  // Monetary values are stored in cents.
  private final long deliveryCents;
  private final List<OrderLine> items;

  // Initializes all order details.
  public FoodOrder(String id, String customerId, String restaurantId, String customerName, String address, String phone, String status, String createdAt, String requestKey, long deliveryCents,List<OrderLine> items){
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
    // Makes an unmodifiable copy of the supplied list.
    this.items = List.copyOf(items);
  }

  // Getter methods provide access to the private fields.
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

  // Adds the total price of every order line to calculate the subtotal.
  public long getSubtotalCents() {
    long total = 0;
    for(OrderLine line : items) total += line.getTotalCents();
    return total;
  }

  // Adds the delivery charge to the food subtotal.
  public long getTotalCents() {
    return getSubtotalCents()+deliveryCents;
  }

  // Returns a readable order label.
  public String getLabel() {
    return "Order " + getId();
  }
}
