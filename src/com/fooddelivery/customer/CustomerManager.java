package com.fooddelivery.customer;

import com.fooddelivery.common.*;
import java.io.IOException;
import java.util.*;

public class CustomerManager {

  private final FileStore files;

  public CustomerManager(FileStore files) {
    this.files = files;
  }

  // READ: convert the saved text into an ordinary list of Customer objects.
  public ArrayList<Customer> readRecords() throws IOException {
    try {
      Customer[] records = files.json.fromJson(files.read("customers.json"), Customer[].class);
      if (records == null) throw new IllegalArgumentException("Empty JSON record list.");
      return new ArrayList<>(Arrays.asList(records));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read customers.json. Restore your backup.", e);
    }
  }

  public Customer findCustomer(String id) throws IOException {
    for (Customer item : readRecords()) {
      if (item.getId().equals(id)) return item;
    }
    return null;
  }

  // CREATE: add the new object, then save the list.
  public void addRecord(Customer item) throws IOException {
    ArrayList<Customer> items = readRecords();
    if (findCustomer(item.getId()) != null)
      throw new IllegalArgumentException("ID already exists.");
    items.add(item);
    files.write("customers.json", files.json.toJson(items));
  }

  // UPDATE: find the matching ID and replace that object.
  private void replaceRecord(Customer item) throws IOException {
    ArrayList<Customer> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(item.getId())) {
        items.set(i, item);
        files.write("customers.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  // DELETE: remove the matching object and save the remaining list.
  private void removeRecord(String id) throws IOException {
    ArrayList<Customer> items = readRecords();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        files.write("customers.json", files.json.toJson(items));
        return;
      }
    }
    throw new Problem(404, "Record not found.");
  }

  public Customer registerCustomer(
      String name,
      String email,
      String phone,
      String address,
      String password,
      String confirmPassword)
      throws IOException {
    name = Text.required(name, "Name", 100);
    email = Text.email(email);
    phone = Text.phone(phone);
    address = Text.required(address, "Address", 250);
    if (password == null || !password.equals(confirmPassword))
      throw new IllegalArgumentException("Passwords do not match.");
    for (Customer customer : readRecords()) {
      if (customer.getEmail().equals(email))
        throw new IllegalArgumentException("This email is already registered.");
    }
    Customer customer =
        new Customer(
            Text.id("C"), name, email, phone, address, Passwords.hash(password), "CUSTOMER");
    addRecord(customer);
    return customer;
  }

  public Customer login(String email, String password) throws IOException {
    email = Text.email(email);
    for (Customer customer : readRecords()) {
      if (customer.getEmail().equals(email)
          && Passwords.matches(password, customer.getPasswordHash())) return customer;
    }
    throw new Problem(401, "Invalid email or password.");
  }

  public Customer updateCustomer(Customer current, String name, String phone, String address)
      throws IOException {
    Customer updated =
        new Customer(
            current.getId(),
            Text.required(name, "Name", 100),
            current.getEmail(),
            Text.phone(phone),
            Text.required(address, "Address", 250),
            current.getPasswordHash(),
            current.getRole());
    replaceRecord(updated);
    return updated;
  }

  public void deleteCustomer(Customer customer, String password) throws IOException {
    if (customer.getRole().equals("MANAGER"))
      throw new IllegalArgumentException("The demo manager account cannot be deleted.");
    if (!Passwords.matches(password, customer.getPasswordHash()))
      throw new IllegalArgumentException("Incorrect password.");
    removeRecord(customer.getId());
  }
}
