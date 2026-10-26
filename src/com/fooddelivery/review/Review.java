package com.fooddelivery.review;

public class Review {
    private String id;

    public String getId() {
        return id;
    }

    private String customerId;
    private String orderId;
    private String restaurantId;
    private int rating;
    private String comment;
    private String createdAt;

    public Review(
            String id,
            String customerId,
            String orderId,
            String restaurantId,
            int rating,
            String comment,
            String createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.orderId = orderId;
        this.restaurantId = restaurantId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getLabel() {
        return comment;
    }
}
