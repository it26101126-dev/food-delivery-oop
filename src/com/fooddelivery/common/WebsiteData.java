package com.fooddelivery.common;

import com.fooddelivery.customer.*;
import com.fooddelivery.delivery.*;
import com.fooddelivery.food.*;
import com.fooddelivery.order.*;
import com.fooddelivery.restaurant.*;
import com.fooddelivery.review.*;
import java.io.IOException;
import java.util.*;

// Shared presentation helper: turn model data into the fields used by JavaScript.
// Business rules stay in the six Manager classes. These maps are only JSON responses.
public class WebsiteData {
  public static Map<String, Object> customer(Customer customer) {
    return Map.of(
        "id",
        customer.getId(),
        "name",
        customer.getName(),
        "email",
        customer.getEmail(),
        "phone",
        customer.getPhone(),
        "address",
        customer.getAddress(),
        "role",
        customer.getRole());
  }

  public static List<Map<String, Object>> foods(FoodManager foods, RestaurantManager restaurants)
      throws IOException {
    List<Map<String, Object>> result = new ArrayList<>();
    for (FoodItem food : foods.getFoods()) {
      Restaurant restaurant = restaurants.findRestaurant(food.getRestaurantId());
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("id", food.getId());
      data.put("name", food.getName());
      data.put("restaurantId", food.getRestaurantId());
      data.put("restaurantName", restaurant == null ? "Restaurant" : restaurant.getName());
      data.put("category", food.getCategory());
      data.put("description", food.getDescription());
      data.put("priceCents", food.getPriceCents());
      data.put("image", food.getImage());
      data.put("available", food.isAvailable());
      data.put("orderable", food.isAvailable() && restaurant != null && restaurant.isOpen());
      result.add(data);
    }
    return result;
  }

  public static List<Map<String, Object>> reviews(ReviewManager reviews, CustomerManager customers)
      throws IOException {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Review r : reviews.getReviews()) {
      Customer author = customers.findCustomer(r.getCustomerId());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", r.getId());
      item.put("customerId", r.getCustomerId());
      item.put("orderId", r.getOrderId());
      item.put("restaurantId", r.getRestaurantId());
      item.put("rating", r.getRating());
      item.put("comment", r.getComment());
      item.put("createdAt", r.getCreatedAt());
      item.put("author", author == null ? "Former customer" : author.getName());
      result.add(item);
    }
    Collections.reverse(result);
    return result;
  }

  public static Map<String, Object> order(
      FoodOrder order, OrderManager orders, RestaurantManager restaurants) throws IOException {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", order.getId());
    data.put("customerId", order.getCustomerId());
    data.put("customerName", order.getCustomerName());
    data.put("restaurantId", order.getRestaurantId());
    data.put("status", orders.status(order));
    List<Map<String, Object>> lines = new ArrayList<>();
    for (OrderLine line : order.getItems()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("foodId", line.getFoodId());
      row.put("name", line.getName());
      row.put("quantity", line.getQuantity());
      row.put("unitCents", line.getUnitCents());
      row.put("totalCents", line.getTotalCents());
      lines.add(row);
    }
    data.put("items", lines);
    data.put("address", order.getAddress());
    data.put("phone", order.getPhone());
    data.put("createdAt", order.getCreatedAt());
    data.put("subtotalCents", order.getSubtotalCents());
    data.put("deliveryCents", order.getDeliveryCents());
    data.put("totalCents", order.getTotalCents());
    data.put("paymentMethod", "Cash on delivery");
    Restaurant restaurant = restaurants.findRestaurant(order.getRestaurantId());
    data.put("restaurantName", restaurant == null ? "Restaurant" : restaurant.getName());
    for (Delivery d : orders.getDeliveryRecords())
      if (d.getOrderId().equals(order.getId())) data.put("delivery", d);
    return data;
  }

  public static List<Object> orders(
      List<FoodOrder> records, OrderManager orders, RestaurantManager restaurants)
      throws IOException {
    List<Object> result = new ArrayList<>();
    for (FoodOrder record : records) result.add(order(record, orders, restaurants));
    return result;
  }
}
