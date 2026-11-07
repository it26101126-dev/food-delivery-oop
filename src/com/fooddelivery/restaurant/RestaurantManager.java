package com.fooddelivery.restaurant;

import com.fooddelivery.common.*;
import com.fooddelivery.food.*;
import java.io.IOException;
import java.util.*;

public class RestaurantManager {

  private final FileStore files;

  public RestaurantManager(FileStore files) {
    this.files = files;
  }

  // READ: convert the saved text into an ordinary list of Restaurant objects.
  public ArrayList<Restaurant> readRecords() throws IOException {
    try {
      Restaurant[] records =
          files.json.fromJson(files.read("restaurants.json"), Restaurant[].class);
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read restaurants.json. Restore your backup.", e);
    }
  }

  public Restaurant findRestaurant(String id) throws IOException {
    for (Restaurant item : readRecords()) {
      if (item.getId().equals(id)) return item;
    }
    return null;
  }

  // CREATE: add the new object, then save the list.
  public void addRecord(Restaurant item) throws IOException {
    ArrayList<Restaurant> items = readRecords();
    if (findRestaurant(item.getId()) != null)
      throw new IllegalArgumentException("ID already exists.");
    items.add(item);
    files.write("restaurants.json", files.json.toJson(items));
  }

  // UPDATE: find the matching ID and replace that object.
  private void replaceRecord(Restaurant item) throws IOException {
    ArrayList<Restaurant> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(item.getId())) {
        items.set(i, item);
        files.write("restaurants.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  // DELETE: remove the matching object and save the remaining list.
  private void removeRecord(String id) throws IOException {
    ArrayList<Restaurant> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        files.write("restaurants.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  public List<Restaurant> getRestaurants() throws IOException {
    return readRecords();
  }

  private Restaurant save(
      String id,
      String name,
      String cuisine,
      String address,
      String phone,
      boolean open,
      String image)
      throws IOException {
    if (!List.of("rice", "burger", "pizza", "salad").contains(image))
      throw new IllegalArgumentException("Choose a cover image.");
    Restaurant item =
        new Restaurant(
            id == null ? Text.id("R") : id,
            Text.required(name, "Name", 100),
            Text.required(cuisine, "Cuisine", 60),
            Text.required(address, "Address", 250),
            Text.phone(phone),
            image,
            open);
    if (id == null) addRecord(item);
    else replaceRecord(item);
    return item;
  }

  public void deleteRestaurant(String id, FoodManager foods) throws IOException {
    for (FoodItem food : foods.readRecords())
      if (food.getRestaurantId().equals(id))
        throw new IllegalArgumentException(
            "Remove this restaurant's menu items first, or mark the restaurant closed.");
    removeRecord(id);
  }

  public Restaurant addRestaurant(
      String name, String cuisine, String address, String phone, boolean open, String image)
      throws IOException {
    return save(null, name, cuisine, address, phone, open, image);
  }

  public Restaurant updateRestaurant(
      String id,
      String name,
      String cuisine,
      String address,
      String phone,
      boolean open,
      String image)
      throws IOException {
    return save(id, name, cuisine, address, phone, open, image);
  }
}
