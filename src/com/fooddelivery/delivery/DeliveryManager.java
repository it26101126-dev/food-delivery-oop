
package com.fooddelivery.delivery;
// PACKAGE: Delivery related classes organize pannura package.

import com.fooddelivery.common.*;
// COMMON: FileStore, Text, Problem ponra shared classes.

import com.fooddelivery.order.*;
// ORDER: FoodOrder, OrderManager classes use panna.

import java.io.IOException;
// FILE ERROR: File read/write errors handle panna.

import java.time.Instant;
// TIME: Current date and time get panna.

import java.util.*;
// COLLECTIONS: ArrayList, List, Arrays use panna.


// CLASS: Delivery management business logic handle pannum class.
public class DeliveryManager {

  // ENCAPSULATION: private attributes
  // final = Constructor la initialize panniyathukku
  // piragu references a reassign panna mudiyathu.

  private final FileStore files;
  // JSON file read/write panna FileStore object.

  private final OrderManager orders;
  // Order details find panna OrderManager object.


  // ==============================
  // CONSTRUCTOR
  // ==============================

  public DeliveryManager(FileStore files, OrderManager orders) {

    this.files = files;
    // this.files = instance variable.
    // files = constructor parameter.

    this.orders = orders;
    // OrderManager object reference initialize pannuthu.
  }


  // ==========================================
  // CRUD - R (READ)
  // METHOD 1: readRecords()
  // ==========================================

  // PURPOSE:
  // deliveries.json file la irukkura data va
  // Java Delivery objects list a convert pannum.

  public ArrayList<Delivery> readRecords() throws IOException {

    // throws IOException = File related error
    // caller method ku propagate aagalam.

    try {

      // STEP 1: File la irukkura JSON text read pannum.
      // STEP 2: JSON text a Delivery[] array a convert pannum.

      Delivery[] records =
          files.json.fromJson(
              files.read("deliveries.json"),
              Delivery[].class
          );

      // fromJson() = JSON to Java object conversion.
      // Delivery[].class = Delivery objects array type.

      // JSON data null a iruntha exception throw pannum.
      if (records == null)
        throw new IllegalArgumentException(
            "Empty JSON record list."
        );

      // Array -> ArrayList conversion.
      // ArrayList dynamic size collection.
      // Add, remove, update operations easy.

      return new ArrayList<>(Arrays.asList(records));

    } catch (RuntimeException e) {

      // JSON parsing ponra runtime error vanthal
      // athai IOException a convert panni throw pannum.

      throw new IOException(
          "Cannot read deliveries.json. Restore your backup.",
          e
      );
    }
  }


  // ==========================================
  // CRUD - R (READ / SEARCH)
  // METHOD 2: findDelivery()
  // ==========================================

  // PURPOSE:
  // Delivery ID use panni matching delivery find pannum.

  public Delivery findDelivery(String id) throws IOException {

    // Enhanced for loop / for-each loop.
    // readRecords() return panna list la
    // ovvoru Delivery object um check pannum.

    for (Delivery item : readRecords()) {

      // getId() = Getter method.
      // equals() = String content compare pannum.

      if (item.getId().equals(id))
        return item;
        // Matching delivery object return pannum.
    }

    // Matching ID illana null return pannum.
    return null;
  }


  // ==========================================
  // CRUD - C (CREATE)
  // METHOD 3: addRecord()
  // ==========================================

  // PURPOSE:
  // New Delivery object a JSON file la add pannum.

  public void addRecord(Delivery item) throws IOException {

    // STEP 1: Existing records read pannum.
    ArrayList<Delivery> items = readRecords();

    // STEP 2: Same ID already irukka check pannum.
    if (findDelivery(item.getId()) != null)

      // Duplicate ID iruntha error.
      throw new IllegalArgumentException(
          "ID already exists."
      );

    // STEP 3: New object ArrayList ku add pannum.
    items.add(item);

    // STEP 4: ArrayList -> JSON conversion.
    // STEP 5: JSON file la save pannum.
    files.write(
        "deliveries.json",
        files.json.toJson(items)
    );

    // toJson() = Java objects to JSON text.
    // files.write() = File la persist pannum.
  }


  // ==========================================
  // CRUD - U (UPDATE)
  // METHOD 4: replaceRecord()
  // ==========================================

  // PURPOSE:
  // Existing delivery ID find panni
  // old object a new object vachu replace pannum.

  private void replaceRecord(Delivery item) throws IOException {

    // STEP 1: All records read pannum.
    ArrayList<Delivery> items = readRecords();

    // STEP 2: Index based for loop.
    for (int i = 0; i < items.size(); i++) {

      // items.get(i) = Current index la irukkura object.
      // getId() = Delivery ID get pannum.

      if (items.get(i).getId().equals(item.getId())) {

        // STEP 3: Old object replace pannum.
        items.set(i, item);

        // set(index, object) = UPDATE operation.
        // add() = New item add.
        // set() = Existing item replace.

        // STEP 4: Updated list JSON file la save.
        files.write(
            "deliveries.json",
            files.json.toJson(items)
        );

        return;
        // Update mudinjathum method exit.
      }
    }

    // Matching delivery ID illana 404 error.
    throw new Problem(404, "Record not found.");
  }


  // ==========================================
  // CRUD - D (DELETE)
  // METHOD 5: removeRecord()
  // ==========================================

  // PURPOSE:
  // Matching Delivery object remove panni
  // remaining records file la save pannum.

  private void removeRecord(String id) throws IOException {

    // STEP 1: Existing records read pannum.
    ArrayList<Delivery> items = readRecords();

    // STEP 2: Matching ID search pannum.
    for (int i = 0; i < items.size(); i++) {

      if (items.get(i).getId().equals(id)) {

        // STEP 3: Matching index item delete.
        items.remove(i);

        // remove(index) = DELETE operation.

        // STEP 4: Remaining records JSON la save.
        files.write(
            "deliveries.json",
            files.json.toJson(items)
        );

        return;
        // Delete mudinjathum method exit.
      }
    }

    // Record illana Not Found error.
    throw new Problem(404, "Record not found.");
  }


  // ==========================================
  // CRUD - R (READ)
  // METHOD 6: getDeliveries()
  // ==========================================

  // PURPOSE:
  // All delivery records return pannum.
  // List = Interface.
  // ArrayList = List interface implementation.

  public List<Delivery> getDeliveries() throws IOException {

    return readRecords();
    // Already irukkura READ method reuse pannuthu.
  }


  // ==========================================
  // COMMON BUSINESS LOGIC METHOD
  // METHOD 7: save()
  // ==========================================

  // PURPOSE:
  // CREATE and UPDATE rendu operations kum
  // common validation and saving logic.

  // private = DeliveryManager class kulla mattum use.

  private Delivery save(
      String id,
      String orderId,
      String name,
      String phone,
      String status
  ) throws IOException {


    // =======================================
    // VALIDATION 1: ORDER READY CHECK
    // =======================================

    // OrderManager use panni order find pannum.
    FoodOrder order = orders.findOrder(orderId);

    // order == null = Order not found.
    // !equals("READY") = Order ready illa.
    // || = OR operator.

    if (order == null || !order.getStatus().equals("READY"))

      throw new IllegalArgumentException(
          "The order must be ready for pickup."
      );

    // BUSINESS RULE:
    // READY order ku mattum courier assign pannalam.


    // =======================================
    // VALIDATION 2: CREATE OR UPDATE?
    // =======================================

    // Ternary operator: condition ? true : false.
    // id == null -> New delivery.
    // id != null -> Existing delivery find pannum.

    Delivery old = id == null ? null : findDelivery(id);

    // UPDATE panna requested ID exist aaganum.
    if (id != null && old == null)
      throw new Problem(404, "Delivery not found.");


    // =======================================
    // VALIDATION 3: ORDER ID CHANGE BLOCK
    // =======================================

    // Already assign panna delivery ku
    // vera orderId change panna allow illa.

    if (old != null && !old.getOrderId().equals(orderId))

      throw new IllegalArgumentException(
          "A delivery's order cannot be changed."
      );


    // =======================================
    // VALIDATION 4: DUPLICATE ASSIGNMENT CHECK
    // =======================================

    // One order ku multiple delivery records
    // create aagama prevent pannum.

    for (Delivery d : readRecords())

      // Same order ID AND different delivery ID.
      if (d.getOrderId().equals(orderId) &&
          !d.getId().equals(id))

        throw new IllegalArgumentException(
            "This order already has a delivery."
        );


    // =======================================
    // VALIDATION 5: DELIVERY STATUS CHECK
    // =======================================

    // New delivery na initial status ASSIGNED.
    // Existing delivery na old status get pannum.

    String current =
        old == null ? "ASSIGNED" : old.getStatus();

    // Initially transition not allowed.
    boolean allowed = false;


    // CASE 1: Same status remain pannalam.
    if (current.equals(status))
      allowed = true;


    // CASE 2: ASSIGNED -> PICKED_UP allowed.
    if (current.equals("ASSIGNED") &&
        "PICKED_UP".equals(status))
      allowed = true;


    // CASE 3: PICKED_UP -> DELIVERED allowed.
    if (current.equals("PICKED_UP") &&
        "DELIVERED".equals(status))
      allowed = true;


    // CASE 4: New delivery ASSIGNED la thaan start.
    if (old == null && !"ASSIGNED".equals(status))
      allowed = false;


    // Invalid status transition na error.
    if (!allowed)
      throw new IllegalArgumentException(
          "Move delivery forward one stage at a time."
      );

    // ALLOWED FLOW:
    // ASSIGNED -> PICKED_UP -> DELIVERED.
    // Direct ASSIGNED -> DELIVERED not allowed.
    // Backward transitions not allowed.


    // =======================================
    // OBJECT CREATION: OOP CONCEPT
    // =======================================

    // new Delivery() = Parameterized constructor call.
    // Encapsulation: Delivery fields getters moolam access.
    // Text.required() = Courier name validation.
    // Text.phone() = Phone processing/validation.
    // Instant.now() = Current timestamp.

    Delivery delivery =
        new Delivery(
            id == null ? Text.id("D") : id,
            // CREATE na new ID; UPDATE na old ID.

            orderId,
            // Related order ID.

            Text.required(name, "Courier name", 100),
            // Courier name required, max 100 characters.

            Text.phone(phone),
            // Phone validation/processing.

            status,
            // Current delivery status.

            Instant.now().toString()
            // Current update time.
        );


    // =======================================
    // FINAL CRUD EXECUTION
    // =======================================

    if (id == null)

      // CREATE: New delivery add pannum.
      addRecord(delivery);

    else

      // UPDATE: Existing delivery replace pannum.
      replaceRecord(delivery);

    // Created / Updated object return pannum.
    return delivery;
  }


  // ==========================================
  // CRUD - D (DELETE)
  // METHOD 8: deleteAssignment()
  // ==========================================

  // PURPOSE:
  // Delivery picked up aagala na mattum delete.

  public void deleteAssignment(String id) throws IOException {

    // STEP 1: Delivery find pannum.
    Delivery d = findDelivery(id);

    // STEP 2: Record exist aagutha check.
    if (d == null)
      throw new Problem(404, "Delivery not found.");

    // STEP 3: ASSIGNED status mattum delete allowed.
    if (!d.getStatus().equals("ASSIGNED"))

      throw new IllegalArgumentException(
          "Only a delivery that has not been picked up can be removed."
      );

    // STEP 4: DELETE method call.
    removeRecord(id);

    // PICKED_UP / DELIVERED aana delete panna mudiyathu.
  }


  // ==========================================
  // CRUD - C (CREATE)
  // METHOD 9: assignDelivery()
  // ==========================================

  // PURPOSE:
  // New courier assignment create pannum.

  public Delivery assignDelivery(
      String orderId,
      String name,
      String phone,
      String status
  ) throws IOException {

    // null ID = CREATE mode.
    // save() validations run pannum.
    // Last la addRecord() call aagum.

    return save(null, orderId, name, phone, status);
  }


  // ==========================================
  // CRUD - U (UPDATE)
  // METHOD 10: updateDeliveryStatus()
  // ==========================================

  // PURPOSE:
  // Existing delivery details/status update pannum.

  public Delivery updateDeliveryStatus(
      String id,
      String orderId,
      String name,
      String phone,
      String status
  ) throws IOException {

    // Existing ID pass pannuvom.
    // save() validations run pannum.
    // Last la replaceRecord() call aagum.

    return save(id, orderId, name, phone, status);
  }

} // END OF DeliveryManager CLASS

