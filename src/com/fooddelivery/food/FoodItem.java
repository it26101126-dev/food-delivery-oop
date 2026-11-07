

package com.fooddelivery.food;

public class FoodItem {
  private String id;

  public String getId() {
    return id;
  }

  private String restaurantId;
  private String name;
  private String category;
  private String description;
  private long priceCents;
  private String image;
  private boolean available;

  public FoodItem(
      String id,
      String restaurantId,
      String name,
      String category,
      String description,
      long priceCents,
      String image,
      boolean available) {
    this.id = id;
    this.restaurantId = restaurantId;
    this.name = name;
    this.category = category;
    this.description = description;
    this.priceCents = priceCents;
    this.image = image;
    this.available = available;
  }

  public String getRestaurantId() {
    return restaurantId;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public long getPriceCents() {
    return priceCents;
  }

  public String getImage() {
    return image;
  }

  public boolean isAvailable() {
    return available;
  }

  public String getLabel() {
    return name;
  }
}
