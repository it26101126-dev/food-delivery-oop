package com.fooddelivery.common;

import com.fooddelivery.customer.*;
import com.fooddelivery.food.*;
import com.fooddelivery.restaurant.*;
import java.nio.file.*;

public class DemoData {
  private final CustomerManager customers;

  private final RestaurantManager restaurants;
  private final FoodManager foods;
  private final Path directory;

  public DemoData(
      CustomerManager customers,
      RestaurantManager restaurants,
      FoodManager foods,
      String directory) {
    this.customers = customers;

    this.restaurants = restaurants;
    this.foods = foods;
    this.directory = Path.of(directory);
  }

  public void run(String... args) throws Exception {
    Files.createDirectories(directory);
    // Marker prevents deliberately deleted demo records from reappearing on restart.
    Path marker = directory.resolve("demo-ready.txt");
    if (Files.exists(marker)) return;
    if (customers.findCustomer("C-MANAGER") == null)
      customers.addRecord(
          new Customer(
              "C-MANAGER",
              "Demo Manager",
              "admin@fooddelivery.test",
              "0771234567",
              "Ampara",
              Passwords.hash("Admin@12345"),
              "MANAGER"));
    if (customers.findCustomer("C-DEMO") == null)
      customers.addRecord(
          new Customer(
              "C-DEMO",
              "Demo Customer",
              "customer@fooddelivery.test",
              "0777654321",
              "42 Main Street, Ampara",
              Passwords.hash("Customer@12345"),
              "CUSTOMER"));
    if (restaurants.findRestaurant("R01") == null)
      restaurants.addRecord(
          new Restaurant(
              "R01",
              "The Rice House",
              "Sri Lankan",
              "Main Street, Ampara",
              "0771111111",
              "rice",
              true));
    if (restaurants.findRestaurant("R02") == null)
      restaurants.addRecord(
          new Restaurant(
              "R02",
              "Bun & Beyond",
              "Burgers & comfort food",
              "Lake Road, Ampara",
              "0772222222",
              "burger",
              true));
    if (restaurants.findRestaurant("R03") == null)
      restaurants.addRecord(
          new Restaurant(
              "R03",
              "Slice Society",
              "Pizza & fresh bowls",
              "Park Street, Ampara",
              "0773333333",
              "pizza",
              true));
    add(
        "F01",
        "R01",
        "Chicken biryani",
        "Rice",
        "Fragrant rice, warming spices and tender chicken.",
        95000,
        "rice");
    add(
        "F02",
        "R01",
        "Vegetable rice bowl",
        "Rice",
        "Seasonal vegetables with a generous bowl of rice.",
        65000,
        "salad");
    add(
        "F03",
        "R02",
        "Signature chicken burger",
        "Burgers",
        "A golden chicken fillet, crisp lettuce and house sauce.",
        85000,
        "burger");
    add(
        "F04",
        "R02",
        "Classic veggie burger",
        "Burgers",
        "A hearty vegetable patty with fresh greens.",
        70000,
        "burger");
    add(
        "F05",
        "R03",
        "Margherita pizza",
        "Pizza",
        "Tomato, mozzarella and fresh basil on a crisp base.",
        180000,
        "pizza");
    add(
        "F06",
        "R03",
        "Garden fresh bowl",
        "Bowls",
        "Colourful greens, crunchy vegetables and a light dressing.",
        75000,
        "salad");
    Files.writeString(marker, "Local demo data initialized.\n");
  }

  private void add(
      String id,
      String restaurant,
      String name,
      String category,
      String description,
      long price,
      String image)
      throws Exception {
    if (foods.findFood(id) == null)
      foods.addRecord(
          new FoodItem(id, restaurant, name, category, description, price, image, true));
  }
}
