package com.fooddelivery.order;

import com.fooddelivery.common.*;
import com.fooddelivery.customer.Customer;
import com.fooddelivery.delivery.*;
import com.fooddelivery.food.*;
import com.fooddelivery.restaurant.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class OrderManager {

  private final FileStore files;
  private final FoodManager foods;
  private final RestaurantManager restaurants;

  public OrderManager(FileStore files, FoodManager foods, RestaurantManager restaurants) {
    this.files = files;
    this.foods = foods;
    this.restaurants = restaurants;
  }

  // READ: convert the saved text into an ordinary list of FoodOrder objects.
  public ArrayList<FoodOrder> readRecords() throws IOException {
    try {
      FoodOrder[] records = files.json.fromJson(files.read("orders.json"), FoodOrder[].class);
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read orders.json. Restore your backup.", e);
    }
  }

  public FoodOrder findOrder(String id) throws IOException {
    for (FoodOrder item : readRecords()) {
      if (item.getId().equals(id)) return item;
    }
    return null;
  }

  // CREATE: add the new object, then save the list.
  public void addRecord(FoodOrder item) throws IOException {
    ArrayList<FoodOrder> items = readRecords();
    if (findOrder(item.getId()) != null) throw new IllegalArgumentException("ID already exists.");
    items.add(item);
    files.write("orders.json", files.json.toJson(items));
  }

  // UPDATE: find the matching ID and replace that object.
  private void replaceRecord(FoodOrder item) throws IOException {
    ArrayList<FoodOrder> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(item.getId())) {
        items.set(i, item);
        files.write("orders.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  // DELETE: remove the matching object and save the remaining list.
  private void removeRecord(String id) throws IOException {
    ArrayList<FoodOrder> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        files.write("orders.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  public static class CartItem {
    public String foodId;
    public int quantity;
  }

  public String status(FoodOrder order) throws IOException {
    for (Delivery delivery : getDeliveryRecords()) {
      if (delivery.getOrderId().equals(order.getId())) {
        if (delivery.getStatus().equals("DELIVERED")) return "DELIVERED";
        if (delivery.getStatus().equals("PICKED_UP")) return "OUT_FOR_DELIVERY";
      }
    }
    return order.getStatus();
  }

  public FoodOrder placeOrder(
      Customer customer,
      List<CartItem> cart,
      String address,
      String phone,
      String key,
      Long quotedTotalCents)
      throws IOException {
    key = Text.required(key, "Request key", 100);
    for (FoodOrder old : readRecords()) {
      if (old.getCustomerId().equals(customer.getId()) && old.getRequestKey().equals(key))
        return old;
    }
    if (cart == null || cart.isEmpty() || cart.size() > 30)
      throw new IllegalArgumentException("Your basket needs 1 to 30 different items.");
    address = Text.required(address, "Delivery address", 250);
    phone = Text.phone(phone);
    List<OrderLine> lines = new ArrayList<>();
    List<String> seen = new ArrayList<>();
    String restaurantId = null;
    for (CartItem item : cart) {
      if (item == null || item.quantity < 1 || item.quantity > 20)
        throw new IllegalArgumentException("Quantity must be between 1 and 20.");
      if (seen.contains(item.foodId)) throw new IllegalArgumentException("Duplicate basket item.");
      seen.add(item.foodId);
      FoodItem food = foods.findFood(item.foodId);
      if (food == null || !food.isAvailable())
        throw new IllegalArgumentException(
            "A basket item is no longer available. Please update your basket.");
      if (restaurantId == null) restaurantId = food.getRestaurantId();
      if (!restaurantId.equals(food.getRestaurantId()))
        throw new IllegalArgumentException("Please order from one restaurant at a time.");
      lines.add(new OrderLine(food.getId(), food.getName(), item.quantity, food.getPriceCents()));
    }
    Restaurant restaurant = restaurants.findRestaurant(restaurantId);
    if (restaurant == null || !restaurant.isOpen())
      throw new IllegalArgumentException("This restaurant is currently closed.");
    FoodOrder order =
        new FoodOrder(
            Text.id("O"),
            customer.getId(),
            restaurantId,
            customer.getName(),
            address,
            phone,
            "PLACED",
            Instant.now().toString(),
            key,
            15000,
            lines);
    if (quotedTotalCents == null || quotedTotalCents != order.getTotalCents())
      throw new Problem(
          409, "Menu prices changed. Refresh the basket and check the new total before ordering.");
    addRecord(order);
    return order;
  }

  public FoodOrder owned(String id, Customer user) throws IOException {
    FoodOrder order = findOrder(id);
    if (order == null) throw new Problem(404, "Order not found.");
    if (!user.getRole().equals("MANAGER") && !order.getCustomerId().equals(user.getId()))
      throw new Problem(403, "This order belongs to another customer.");
    return order;
  }

  public void updateOrderStatus(String id, Customer user, String next) throws IOException {
    FoodOrder order = owned(id, user);
    String current = status(order);
    if (next == null) throw new IllegalArgumentException("Choose a status.");
    boolean allowed = false;
    if (next.equals("CANCELLED")) {
      if (current.equals("PLACED") || current.equals("CONFIRMED")) allowed = true;
    }
    if (user.getRole().equals("MANAGER")) {
      if (current.equals("PLACED") && next.equals("CONFIRMED")) allowed = true;
      if (current.equals("CONFIRMED") && next.equals("PREPARING")) allowed = true;
      if (current.equals("PREPARING") && next.equals("READY")) allowed = true;
    }
    if (!allowed) throw new IllegalArgumentException("This status change is not allowed.");
    replaceRecord(
        new FoodOrder(
            order.getId(),
            order.getCustomerId(),
            order.getRestaurantId(),
            order.getCustomerName(),
            order.getAddress(),
            order.getPhone(),
            next,
            order.getCreatedAt(),
            order.getRequestKey(),
            order.getDeliveryCents(),
            order.getItems()));
  }

  public void deleteOrder(String id, Customer user) throws IOException {
    FoodOrder order = owned(id, user);
    if (!status(order).equals("CANCELLED"))
      throw new IllegalArgumentException("Only cancelled orders can be removed.");
    removeRecord(id);
  }

  public boolean hasActiveOrders(String customerId) throws IOException {
    for (FoodOrder order : readRecords())
      if (order.getCustomerId().equals(customerId)) {
        String s = status(order);
        if (!s.equals("DELIVERED") && !s.equals("CANCELLED")) return true;
      }
    return false;
  }

  public List<Delivery> getDeliveryRecords() throws IOException {
    try {
      return Arrays.asList(files.json.fromJson(files.read("deliveries.json"), Delivery[].class));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read deliveries.json", e);
    }
  }

  public void cancelOrder(String id, Customer user) throws IOException {
    updateOrderStatus(id, user, "CANCELLED");
  }

  public List<FoodOrder> getOrders(Customer user) throws IOException {
    List<FoodOrder> results = new ArrayList<>();
    for (FoodOrder order : readRecords()) {
      if (user.getRole().equals("MANAGER") || order.getCustomerId().equals(user.getId()))
        results.add(order);
    }
    Collections.reverse(results);
    return results;
  }
}
