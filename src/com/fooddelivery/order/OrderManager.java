package com.fooddelivery.order;

import com.fooddelivery.common.*;
import com.fooddelivery.customer.Customer;
import com.fooddelivery.delivery.*;
import com.fooddelivery.food.*;
import com.fooddelivery.restaurant.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/** Manages food orders, including creation, updates, deletion and retrieval. */
public class OrderManager {

  // Objects used to access files, food records and restaurant records.
  private final FileStore files;
  private final FoodManager foods;
  private final RestaurantManager restaurants;

  // Receives the required manager objects through the constructor.
  public OrderManager(FileStore files, FoodManager foods, RestaurantManager restaurants) {
    this.files = files;
    this.foods = foods;
    this.restaurants = restaurants;
  }

  // READ: convert the saved text into an ordinary list of FoodOrder objects.
  public ArrayList<FoodOrder> readRecords() throws IOException {
    try {
      // Deserialize the JSON data into an array of FoodOrder objects.
      FoodOrder[] records = files.json.fromJson(files.read("orders.json"), FoodOrder[].class);
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      // Convert JSON/runtime errors into an IOException.
      throw new IOException("Cannot read orders.json. Restore your backup.", e);
    }
  }

  // Finds an order by its ID. Returns null if it does not exist.
  public FoodOrder findOrder(String id) throws IOException {
    for (FoodOrder item : readRecords()) {
      if (item.getId().equals(id)) return item;
    }
    return null;
  }

  // CREATE: add the new object, then save the list.
  public void addRecord(FoodOrder item) throws IOException {
    ArrayList<FoodOrder> items = readRecords();
    // Prevent duplicate order IDs.
    if (findOrder(item.getId()) != null) throw new IllegalArgumentException("ID already exists.");
    items.add(item);
    // Serialize the updated list and save it in orders.json.
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
    for (int i=0; i < items.size(); i++) {
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        files.write("orders.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  // Represents one selected food item and its quantity in the cart.
  public static class CartItem {
    public String foodId;
    public int quantity;
  }

  // Returns the delivery status when available; otherwise returns the order status.
  public String status(FoodOrder order) throws IOException {
    for (Delivery delivery : getDeliveryRecords()) {
      if (delivery.getOrderId().equals(order.getId())) {
        if (delivery.getStatus().equals("DELIVERED")) return "DELIVERED";
        if (delivery.getStatus().equals("PICKED_UP")) return "OUT_FOR_DELIVERY";
      }
    }
    return order.getStatus();
  }

  // Validates the cart and customer details, creates the order and saves it.
  public FoodOrder placeOrder(Customer customer, List<CartItem> cart, String address, String phone, String key, Long quotedTotalCents)
      throws IOException {
    key = Text.required(key, "Request key", 100);
    // Return the existing order if this customer already used the same request key.
    for (FoodOrder old : readRecords()) {
      if (old.getCustomerId().equals(customer.getId()) && old.getRequestKey().equals(key))
        return old;
    }
    // Check that the cart contains between 1 and 30 items.
    if (cart == null || cart.isEmpty() || cart.size() > 30)
      throw new IllegalArgumentException("Your basket needs 1 to 30 different items.");
    address = Text.required(address, "Delivery address", 250);
    phone = Text.phone(phone);
    List<OrderLine> lines = new ArrayList<>();
    List<String> seen = new ArrayList<>();
    String restaurantId = null;
    for (CartItem item : cart) {
      // Validate each cart item and its quantity.
      if (item == null || item.quantity < 1 || item.quantity > 20)
        throw new IllegalArgumentException("Quantity must be between 1 and 20.");
      // Prevent the same food item from being added more than once.
      if (seen.contains(item.foodId)) throw new IllegalArgumentException("Duplicate basket item.");
      seen.add(item.foodId);
      // Find the food record and check that it is available.
      FoodItem food = foods.findFood(item.foodId);
      if (food == null || !food.isAvailable())
        throw new IllegalArgumentException(
            "A basket item is no longer available. Please update your basket.");
      // Set the restaurant ID from the first item and require all items to match it.
      if (restaurantId == null) restaurantId = food.getRestaurantId();
      if (!restaurantId.equals(food.getRestaurantId()))
        throw new IllegalArgumentException("Please order from one restaurant at a time.");
      // Store the selected food details and current unit price in an OrderLine.
      lines.add(new OrderLine(food.getId(), food.getName(), item.quantity, food.getPriceCents()));
    }
    // Check that the restaurant exists and is open.
    Restaurant restaurant = restaurants.findRestaurant(restaurantId);
    if (restaurant == null || !restaurant.isOpen())
      throw new IllegalArgumentException("This restaurant is currently closed.");
    // Create the order with a generated ID, timestamp, initial status and delivery fee.
    FoodOrder order=new FoodOrder(Text.id("O"),customer.getId(),restaurantId,customer.getName(),address,phone,"PLACED",Instant.now().toString(),key,15000,lines);
    // Compare the current total with the amount quoted to the customer.
    if (quotedTotalCents == null || quotedTotalCents != order.getTotalCents())
      throw new Problem(409, "Menu prices changed. Refresh the basket and check the new total before ordering.");
    addRecord(order);
    return order;
  }

  // Finds an order and checks whether the user is allowed to access it.
  public FoodOrder owned(String id, Customer user) throws IOException {
    FoodOrder order = findOrder(id);
    if (order == null) throw new Problem(404, "Order not found.");
    // Managers can access all orders; customers can access only their own orders.
    if (!user.getRole().equals("MANAGER") && !order.getCustomerId().equals(user.getId()))
      throw new Problem(403, "This order belongs to another customer.");
    return order;
  }

  // Validates and updates the order status according to the user's role.
  public void updateOrderStatus(String id, Customer user, String next) throws IOException {
    FoodOrder order = owned(id, user);
    String current = status(order);
    if (next == null) throw new IllegalArgumentException("Choose a status.");
    boolean allowed = false;
    // Orders can be cancelled only while they are PLACED or CONFIRMED.
    if (next.equals("CANCELLED")) {
      if (current.equals("PLACED") || current.equals("CONFIRMED")) allowed = true;
    }
    // Managers can move orders through the preparation stages.
    if (user.getRole().equals("MANAGER")) {
      if (current.equals("PLACED") && next.equals("CONFIRMED")) allowed = true;
      if (current.equals("CONFIRMED") && next.equals("PREPARING")) allowed = true;
      if (current.equals("PREPARING") && next.equals("READY")) allowed = true;
    }
    if (!allowed) throw new IllegalArgumentException("This status change is not allowed.");
    // Replace the order with a new object containing the updated status.
    replaceRecord(new FoodOrder(
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

  // Removes an order only if its current status is CANCELLED.
  public void deleteOrder(String id, Customer user) throws IOException {
    FoodOrder order = owned(id, user);
    if (!status(order).equals("CANCELLED"))
      throw new IllegalArgumentException("Only cancelled orders can be removed.");
    removeRecord(id);
  }

  // Checks whether the customer has any order that is not delivered or cancelled.
  public boolean hasActiveOrders(String customerId) throws IOException {
    for (FoodOrder order : readRecords())
      if (order.getCustomerId().equals(customerId)) {
        String s = status(order);
        if (!s.equals("DELIVERED") && !s.equals("CANCELLED")) return true;
      }
    return false;
  }

  // Reads delivery records from deliveries.json.
  public List<Delivery> getDeliveryRecords() throws IOException {
    try {
      return Arrays.asList(files.json.fromJson(files.read("deliveries.json"), Delivery[].class));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read deliveries.json", e);
    }
  }

  // Uses the status update method to cancel the order.
  public void cancelOrder(String id, Customer user) throws IOException {
    updateOrderStatus(id, user, "CANCELLED");
  }

  // Returns all orders for a manager, or only the user's orders for a customer.
  public List<FoodOrder> getOrders(Customer user) throws IOException {
    List<FoodOrder> results = new ArrayList<>();
    for (FoodOrder order : readRecords()) {
      if (user.getRole().equals("MANAGER") || order.getCustomerId().equals(user.getId()))
        results.add(order);
    }
    // Reverse the list so the most recently added orders appear first.
    Collections.reverse(results);
    return results;
  }
}
