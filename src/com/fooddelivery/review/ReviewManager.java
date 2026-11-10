package com.fooddelivery.review;

import com.fooddelivery.common.*;
import com.fooddelivery.customer.*;
import com.fooddelivery.order.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

// Handles review validation and CRUD operations.
public class ReviewManager {

  private final FileStore files;           // Reads and writes reviews.json.
  private final OrderManager orders;       // Looks up orders and checks their status.
  private final CustomerManager customers; // Stored here, but unused by these methods.

  // Receives the shared objects that the application passes to this manager.
  public ReviewManager(FileStore files, OrderManager orders, CustomerManager customers) {
    this.files = files;
    this.orders = orders;
    this.customers = customers;
  }

  // READ: Loads the saved reviews as a modifiable list.
  public ArrayList<Review> readRecords() throws IOException {
    try {
      // Read the JSON text and convert it into an array of Review objects.
      Review[] records = files.json.fromJson(files.read("reviews.json"), Review[].class);

      // A null result is invalid. A valid empty array [] is allowed.
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");

      // Make a list that supports adding, replacing, and removing records.
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      // Report a read error and keep the original exception as its cause.
      throw new IOException("Cannot read reviews.json. Restore your backup.", e);
    }
  }

  // Searches for a review by its review ID.
  public Review findReview(String id) throws IOException {
    for (Review item : readRecords()) {
      // Return immediately when the ID matches.
      if (item.getId().equals(id)) return item;
    }

    // No matching review was found.
    return null;
  }

  // CREATE: Adds a review to the list and saves the updated list.
  public void addRecord(Review item) throws IOException {
    ArrayList<Review> items = readRecords();

    // Prevent two records from having the same review ID.
    if (findReview(item.getId()) != null) throw new IllegalArgumentException("ID already exists.");

    items.add(item);

    // Convert the whole list back into JSON and write it to the file.
    files.write("reviews.json", files.json.toJson(items));
  }

  // UPDATE: Replaces the existing review with the same ID.
  private void replaceRecord(Review item) throws IOException {
    ArrayList<Review> items = readRecords();

    // Use the index so we can replace the record at that position.
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(item.getId())) {
        items.set(i, item);
        files.write("reviews.json", files.json.toJson(items));
        return;
      }
    }

    // The requested review ID was not in the list.
    throw new Problem(404, "Record not found.");
  }

  // DELETE: Removes a review by ID and saves the remaining records.
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

  // Shares the validation and saving logic for both create and update.
  // A null ID means create. An existing review ID means update.
  private Review save(String id, Customer user, String orderId, int rating, String comment)
      throws IOException {

    // Get the order and verify that it belongs to this user and is delivered.
    FoodOrder order = orders.owned(orderId, user);
    if (!order.getCustomerId().equals(user.getId()) || !orders.status(order).equals("DELIVERED"))
      throw new IllegalArgumentException("Only your delivered orders can be reviewed.");

    // Accept only ratings from 1 to 5, inclusive.
    if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be from 1 to 5.");

    // Load the old review only when editing.
    Review old = id == null ? null : findReview(id);

    // To edit, the review must exist, belong to this user, and match this order.
    if (id != null
        && (old == null
            || !old.getCustomerId().equals(user.getId())
            || !old.getOrderId().equals(orderId)))
      throw new Problem(403, "You can only edit your own review.");

    // Allow one review per order. During an edit, skip the current review.
    for (Review r : readRecords())
      if (r.getOrderId().equals(orderId) && !r.getId().equals(id))
        throw new IllegalArgumentException(
            "You already reviewed this order. Edit your existing review.");

    Review review =
        new Review(
            id == null ? Text.id("V") : id, // Generate an ID for a new review.
            user.getId(),
            orderId,
            order.getRestaurantId(),
            rating,
            Text.required(comment, "Review", 1000), // Validate the required comment.
            // Keep the original creation time when editing.
            old == null ? Instant.now().toString() : old.getCreatedAt());

    // Choose the file operation based on whether this is a create or an edit.
    if (id == null) addRecord(review);
    else replaceRecord(review);

    return review;
  }

  // Deletes a review after checking that the user has permission.
  public void deleteReview(String id, Customer user) throws IOException {
    Review review = findReview(id);

    // Return a 404 error if the review does not exist.
    if (review == null) throw new Problem(404, "Review not found.");

    // Reject deletion if the user is neither the review owner nor a manager.
    if (!review.getCustomerId().equals(user.getId()) && !user.getRole().equals("MANAGER"))
      throw new Problem(403, "You can only delete your own review.");

    removeRecord(id);
  }

  // Pass null as the ID to create a new review.
  public Review addReview(Customer user, String orderId, int rating, String comment)
      throws IOException {
    return save(null, user, orderId, rating, comment);
  }

  // Pass the existing review ID to update that review.
  public Review updateReview(String id, Customer user, String orderId, int rating, String comment)
      throws IOException {
    return save(id, user, orderId, rating, comment);
  }

  // Returns all reviews currently saved in reviews.json.
  public List<Review> getReviews() throws IOException {
    return readRecords();
  }
}
