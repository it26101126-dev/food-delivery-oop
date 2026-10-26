package com.fooddelivery.review;

import com.fooddelivery.common.*;
import com.fooddelivery.customer.*;
import com.fooddelivery.order.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class ReviewManager {

    private final FileStore files;
    private final OrderManager orders;
    private final CustomerManager customers;

    public ReviewManager(FileStore files, OrderManager orders, CustomerManager customers) {
        this.files = files;
        this.orders = orders;
        this.customers = customers;
    }

    // READ: convert the saved text into an ordinary list of Review objects.
    public ArrayList<Review> readRecords() throws IOException {
        try {
            Review[] records = files.json.fromJson(files.read("reviews.json"), Review[].class);
            if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
            return new ArrayList<>(Arrays.asList(records));
        } catch (RuntimeException e) {
            throw new IOException("Cannot read reviews.json. Restore your backup.", e);
        }
    }

    public Review findReview(String id) throws IOException {
        for (Review item : readRecords()) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }

    // CREATE: add the new object, then save the list.
    public void addRecord(Review item) throws IOException {
        ArrayList<Review> items = readRecords();
        if (findReview(item.getId()) != null) throw new IllegalArgumentException("ID already exists.");
        items.add(item);
        files.write("reviews.json", files.json.toJson(items));
    }

    // UPDATE: find the matching ID and replace that object.
    private void replaceRecord(Review item) throws IOException {
        ArrayList<Review> items = readRecords();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                items.set(i, item);
                files.write("reviews.json", files.json.toJson(items));
                return;
            }
        }
        throw new Problem(404, "Record not found.");
    }

    // DELETE: remove the matching object and save the remaining list.
    private void removeRecord(String id) throws IOException {
        ArrayList<Review> items = readRecords();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                items.remove(i);
                files.write("reviews.json", files.json.toJson(items));
                return;
            }
        }
        throw new Problem(404, "Record not found.");
    }

    private Review save(String id, Customer user, String orderId, int rating, String comment)
            throws IOException {
        FoodOrder order = orders.owned(orderId, user);
        if (!order.getCustomerId().equals(user.getId()) || !orders.status(order).equals("DELIVERED"))
            throw new IllegalArgumentException("Only your delivered orders can be reviewed.");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be from 1 to 5.");
        Review old = id == null ? null : findReview(id);
        if (id != null
                && (old == null
                || !old.getCustomerId().equals(user.getId())
                || !old.getOrderId().equals(orderId)))
            throw new Problem(403, "You can only edit your own review.");
        for (Review r : readRecords())
            if (r.getOrderId().equals(orderId) && !r.getId().equals(id))
                throw new IllegalArgumentException(
                        "You already reviewed this order. Edit your existing review.");
        Review review =
                new Review(
                        id == null ? Text.id("V") : id,
                        user.getId(),
                        orderId,
                        order.getRestaurantId(),
                        rating,
                        Text.required(comment, "Review", 1000),
                        old == null ? Instant.now().toString() : old.getCreatedAt());
        if (id == null) addRecord(review);
        else replaceRecord(review);
        return review;
    }

    public void deleteReview(String id, Customer user) throws IOException {
        Review review = findReview(id);
        if (review == null) throw new Problem(404, "Review not found.");
        if (!review.getCustomerId().equals(user.getId()) && !user.getRole().equals("MANAGER"))
            throw new Problem(403, "You can only delete your own review.");
        removeRecord(id);
    }

    public Review addReview(Customer user, String orderId, int rating, String comment)
            throws IOException {
        return save(null, user, orderId, rating, comment);
    }

    public Review updateReview(String id, Customer user, String orderId, int rating, String comment)
            throws IOException {
        return save(id, user, orderId, rating, comment);
    }

    public List<Review> getReviews() throws IOException {
        return readRecords();
    }
}
