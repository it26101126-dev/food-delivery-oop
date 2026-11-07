package com.fooddelivery.delivery;

import com.fooddelivery.common.*;
import com.fooddelivery.order.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class DeliveryManager {

  private final FileStore files;
  private final OrderManager orders;

  public DeliveryManager(FileStore files, OrderManager orders) {
    this.files = files;
    this.orders = orders;
  }

  // READ: convert the saved text into an ordinary list of Delivery objects.
  public ArrayList<Delivery> readRecords() throws IOException {
    try {
      Delivery[] records = files.json.fromJson(files.read("deliveries.json"), Delivery[].class);
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read deliveries.json. Restore your backup.", e);
    }
  }

  public Delivery findDelivery(String id) throws IOException {
    for (Delivery item : readRecords()) {
      if (item.getId().equals(id)) return item;
    }
    return null;
  }

  // CREATE: add the new object, then save the list.
  public void addRecord(Delivery item) throws IOException {
    ArrayList<Delivery> items = readRecords();
    if (findDelivery(item.getId()) != null)
      throw new IllegalArgumentException("ID already exists.");
    items.add(item);
    files.write("deliveries.json", files.json.toJson(items));
  }

  // UPDATE: find the matching ID and replace that object.
  private void replaceRecord(Delivery item) throws IOException {
    ArrayList<Delivery> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(item.getId())) {
        items.set(i, item);
        files.write("deliveries.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  // DELETE: remove the matching object and save the remaining list.
  private void removeRecord(String id) throws IOException {
    ArrayList<Delivery> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        files.write("deliveries.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  public List<Delivery> getDeliveries() throws IOException {
    return readRecords();
  }

  private Delivery save(String id, String orderId, String name, String phone, String status)
      throws IOException {
    FoodOrder order = orders.findOrder(orderId);
    if (order == null || !order.getStatus().equals("READY"))
      throw new IllegalArgumentException("The order must be ready for pickup.");
    Delivery old = id == null ? null : findDelivery(id);
    if (id != null && old == null) throw new Problem(404, "Delivery not found.");
    if (old != null && !old.getOrderId().equals(orderId))
      throw new IllegalArgumentException("A delivery's order cannot be changed.");
    for (Delivery d : readRecords())
      if (d.getOrderId().equals(orderId) && !d.getId().equals(id))
        throw new IllegalArgumentException("This order already has a delivery.");
    String current = old == null ? "ASSIGNED" : old.getStatus();
    boolean allowed = false;
    if (current.equals(status)) allowed = true;
    if (current.equals("ASSIGNED") && "PICKED_UP".equals(status)) allowed = true;
    if (current.equals("PICKED_UP") && "DELIVERED".equals(status)) allowed = true;
    if (old == null && !"ASSIGNED".equals(status)) allowed = false;
    if (!allowed) throw new IllegalArgumentException("Move delivery forward one stage at a time.");
    Delivery delivery =
        new Delivery(
            id == null ? Text.id("D") : id,
            orderId,
            Text.required(name, "Courier name", 100),
            Text.phone(phone),
            status,
            Instant.now().toString());
    if (id == null) addRecord(delivery);
    else replaceRecord(delivery);
    return delivery;
  }

  public void deleteAssignment(String id) throws IOException {
    Delivery d = findDelivery(id);
    if (d == null) throw new Problem(404, "Delivery not found.");
    if (!d.getStatus().equals("ASSIGNED"))
      throw new IllegalArgumentException(
          "Only a delivery that has not been picked up can be removed.");
    removeRecord(id);
  }

  public Delivery assignDelivery(String orderId, String name, String phone, String status)
      throws IOException {
    return save(null, orderId, name, phone, status);
  }

  public Delivery updateDeliveryStatus(
      String id, String orderId, String name, String phone, String status) throws IOException {
    return save(id, orderId, name, phone, status);
  }
}
