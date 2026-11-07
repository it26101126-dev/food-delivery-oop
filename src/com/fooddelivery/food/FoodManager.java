package com.fooddelivery.food;

import com.fooddelivery.common.*;
import com.fooddelivery.restaurant.*;
import java.io.IOException;
import java.util.*;

public class FoodManager {

  private final FileStore files;
  private final RestaurantManager restaurants;

  public FoodManager(FileStore files, RestaurantManager restaurants) {
    this.files = files;
    this.restaurants = restaurants;
  }

  // READ: convert the saved text into an ordinary list of FoodItem objects.
  public ArrayList<FoodItem> readRecords() throws IOException {
    try {
      FoodItem[] records = files.json.fromJson(files.read("foods.json"), FoodItem[].class);
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read foods.json. Restore your backup.", e);
    }
  }

  public FoodItem findFood(String id) throws IOException {
    for (FoodItem item : readRecords()) {
      if (item.getId().equals(id)) return item;
    }
    return null;
  }

  // CREATE: add the new object, then save the list.
  public void addRecord(FoodItem item) throws IOException {
    ArrayList<FoodItem> items = readRecords();
    if (findFood(item.getId()) != null) throw new IllegalArgumentException("ID already exists.");
    items.add(item);
    files.write("foods.json", files.json.toJson(items));
  }

  // UPDATE: find the matching ID and replace that object.
  private void replaceRecord(FoodItem item) throws IOException {
    ArrayList<FoodItem> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(item.getId())) {
        items.set(i, item);
        files.write("foods.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  // DELETE: remove the matching object and save the remaining list.
  private void removeRecord(String id) throws IOException {
    ArrayList<FoodItem> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        files.write("foods.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  private FoodItem save(
      String id,
      String restaurantId,
      String name,
      String category,
      String description,
      String price,
      String image,
      boolean available)
      throws IOException {
    if (restaurants.findRestaurant(restaurantId) == null)
      throw new IllegalArgumentException("Choose an existing restaurant.");
    if (!List.of("rice", "burger", "pizza", "salad").contains(image))
      throw new IllegalArgumentException("Choose a food photo.");
    FoodItem food =
        new FoodItem(
            id == null ? Text.id("F") : id,
            restaurantId,
            Text.required(name, "Food name", 100),
            Text.required(category, "Category", 50),
            Text.required(description, "Description", 300),
            Text.cents(price),
            image,
            available);
    if (id == null) addRecord(food);
    else replaceRecord(food);
    return food;
  }

  public void deleteFood(String id) throws IOException {
    removeRecord(id);
  }

  public FoodItem addFood(
      String restaurantId,
      String name,
      String category,
      String description,
      String price,
      String image,
      boolean available)
      throws IOException {
    return save(null, restaurantId, name, category, description, price, image, available);
  }

  public FoodItem updateFood(
      String id,
      String restaurantId,
      String name,
      String category,
      String description,
      String price,
      String image,
      boolean available)
      throws IOException {
    return save(id, restaurantId, name, category, description, price, image, available);
  }

  public List<FoodItem> getFoods() throws IOException {
    return readRecords();
  }
}
