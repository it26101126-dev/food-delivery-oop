package com.fooddelivery;

import com.fooddelivery.common.*;
import com.fooddelivery.customer.*;
import com.fooddelivery.delivery.*;
import com.fooddelivery.food.*;
import com.fooddelivery.order.*;
import com.fooddelivery.restaurant.*;
import com.fooddelivery.review.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class Main {
  public static void main(String[] args) throws Exception {
    int port = 8081;
    String directory = "data";
    // Optional test arguments. Normal launch always uses 8081 and data/.
    for (String arg : args) {
      if (arg.startsWith("--port=")) port = Integer.parseInt(arg.substring(7));
      if (arg.startsWith("--data=")) directory = arg.substring(7);
    }
    FileStore files = new FileStore(directory);
    CustomerManager customers = new CustomerManager(files);
    RestaurantManager restaurants = new RestaurantManager(files);
    FoodManager foods = new FoodManager(files, restaurants);
    OrderManager orders = new OrderManager(files, foods, restaurants);
    DeliveryManager deliveries = new DeliveryManager(files, orders);
    ReviewManager reviews = new ReviewManager(files, orders, customers);
    new DemoData(customers, restaurants, foods, directory).run();
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
      server.createContext(
          "/",
          new WebServer(files, customers, restaurants, foods, orders, deliveries, reviews, port));
      // The JDK default executor handles requests on its server thread.
      // One request at a time keeps the classroom text-file store simple.
      server.start();
      System.out.println("FoodDelivery ready: http://localhost:" + port);
      System.out.println("Keep this window open. Press Ctrl+C to stop.");
    } catch (java.net.BindException e) {
      System.err.println(
          "Port " + port + " is already in use. Stop the earlier app with Ctrl+C, then try again.");
      System.exit(1);
    }
  }
}
