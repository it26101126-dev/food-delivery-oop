package com.fooddelivery.common;

import com.fooddelivery.customer.*;
import com.fooddelivery.delivery.*;
import com.fooddelivery.food.*;
import com.fooddelivery.order.*;
import com.fooddelivery.restaurant.*;
import com.fooddelivery.review.*;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// Shared website adapter. Members study their Manager class first.
// HttpHandler is the JDK's interface, not a custom project framework.
public class WebServer implements HttpHandler {
  private final FileStore files;
  private final CustomerManager customers;
  private final RestaurantManager restaurants;
  private final FoodManager foods;
  private final OrderManager orders;
  private final DeliveryManager deliveries;
  private final ReviewManager reviews;
  private final int port;
  private final Map<String, Session> sessions = new HashMap<>();

  private static class Session {
    String customerId;
    String csrf = UUID.randomUUID().toString();
    long expires = System.currentTimeMillis() + 30 * 60 * 1000;
  }

  // Fields submitted by our HTML forms. Gson converts JSON to this small object.
  public static class Form {
    public String name, email, phone, address, password, confirmPassword, cuisine, image;
    public String restaurantId, category, description, price, requestKey, status;
    public String orderId, driverName, driverPhone, comment;
    public boolean confirmed, open, available;
    public int rating;
    public Long quotedTotalCents;
    public List<OrderManager.CartItem> items;
  }

  public WebServer(
      FileStore files,
      CustomerManager customers,
      RestaurantManager restaurants,
      FoodManager foods,
      OrderManager orders,
      DeliveryManager deliveries,
      ReviewManager reviews,
      int port) {
    this.files = files;
    this.customers = customers;
    this.restaurants = restaurants;
    this.foods = foods;
    this.orders = orders;
    this.deliveries = deliveries;
    this.reviews = reviews;
    this.port = port;
  }

  @Override
  public void handle(HttpExchange request) throws IOException {
    try {
      String host = request.getRequestHeaders().getFirst("Host");
      if (!("localhost:" + port).equalsIgnoreCase(host) && !("127.0.0.1:" + port).equals(host))
        throw new Problem(403, "Use the localhost application address.");
      request.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
      request.getResponseHeaders().set("X-Frame-Options", "DENY");
      request.getResponseHeaders().set("Cache-Control", "no-store");
      String path = request.getRequestURI().getPath();
      if (!path.startsWith("/api/")) {
        servePage(request, path);
        return;
      }
      Session session = session(request);
      String method = request.getRequestMethod();
      if (!method.equals("GET")
          && !session.csrf.equals(request.getRequestHeaders().getFirst("X-CSRF-Token")))
        throw new Problem(403, "Session token missing or expired. Refresh the page.");
      Form f = new Form();
      if (!method.equals("GET")) {
        byte[] bytes = request.getRequestBody().readNBytes(65537);
        if (bytes.length > 65536) throw new Problem(413, "Request too large.");
        if (bytes.length > 0)
          f = files.json.fromJson(new String(bytes, StandardCharsets.UTF_8), Form.class);
        if (f == null) throw new IllegalArgumentException("Enter the required form details.");
      }
      Object result = route(request, path, method, f, session);
      sendJson(request, 200, result);
    } catch (Problem e) {
      sendJson(request, e.getStatus(), Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException | JsonParseException e) {
      sendJson(
          request,
          400,
          Map.of("error", e.getMessage() == null ? "Invalid input." : e.getMessage()));
    } catch (IOException e) {
      sendJson(
          request,
          503,
          Map.of("error", "Could not read or save data. Check the data folder and try again."));
    } catch (Exception e) {
      e.printStackTrace();
      sendJson(request, 500, Map.of("error", "Unexpected server error. Check the terminal."));
    } finally {
      request.close();
    }
  }

  private Object route(HttpExchange request, String path, String method, Form f, Session session)
      throws IOException {
    // MEMBER 1: customer accounts.
    if (path.equals("/api/session") && method.equals("GET")) return sessionInfo(session);
    if (path.equals("/api/register") && method.equals("POST")) {
      customers.registerCustomer(
          f.name, f.email, f.phone, f.address, f.password, f.confirmPassword);
      return message("Account created. Please log in.");
    }
    if (path.equals("/api/login") && method.equals("POST")) {
      Customer user = customers.login(f.email, f.password);
      removeSession(request);
      session = createSession(request);
      session.customerId = user.getId();
      return sessionInfo(session);
    }
    if (path.equals("/api/logout") && method.equals("POST")) {
      removeSession(request);
      return message("Logged out.");
    }
    if (path.equals("/api/profile")) {
      Customer user = requireCustomer(session);
      if (method.equals("PUT"))
        return WebsiteData.customer(customers.updateCustomer(user, f.name, f.phone, f.address));
      if (method.equals("DELETE")) {
        if (!f.confirmed) throw new IllegalArgumentException("Confirm account deletion.");
        if (orders.hasActiveOrders(user.getId()))
          throw new IllegalArgumentException("Complete or cancel your active orders first.");
        customers.deleteCustomer(user, f.password);
        removeSession(request);
        return message("Account deleted.");
      }
    }
    if (path.equals("/api/manage/customers") && method.equals("GET")) {
      requireManager(session);
      List<Object> result = new ArrayList<>();
      for (Customer c : customers.readRecords()) result.add(WebsiteData.customer(c));
      return result;
    }
    // MEMBER 2: restaurants.
    if (path.equals("/api/restaurants") && method.equals("GET"))
      return restaurants.getRestaurants();
    if (path.equals("/api/manage/restaurants") || path.startsWith("/api/manage/restaurants/")) {
      requireManager(session);
      String id = recordId(path, "/api/manage/restaurants");
      if (id == null && method.equals("POST"))
        return restaurants.addRestaurant(f.name, f.cuisine, f.address, f.phone, f.open, f.image);
      if (id != null && method.equals("PUT"))
        return restaurants.updateRestaurant(
            id, f.name, f.cuisine, f.address, f.phone, f.open, f.image);
      if (id != null && method.equals("DELETE")) {
        restaurants.deleteRestaurant(id, foods);
        return message("Restaurant removed.");
      }
    }
    // MEMBER 3: food menu.
    if (path.equals("/api/foods") && method.equals("GET"))
      return WebsiteData.foods(foods, restaurants);
    if (path.equals("/api/manage/foods") || path.startsWith("/api/manage/foods/")) {
      requireManager(session);
      String id = recordId(path, "/api/manage/foods");
      if (id == null && method.equals("POST"))
        return foods.addFood(
            f.restaurantId, f.name, f.category, f.description, f.price, f.image, f.available);
      if (id != null && method.equals("PUT"))
        return foods.updateFood(
            id, f.restaurantId, f.name, f.category, f.description, f.price, f.image, f.available);
      if (id != null && method.equals("DELETE")) {
        foods.deleteFood(id);
        return message("Food removed.");
      }
    }
    // MEMBER 4: ordering.
    if (path.equals("/api/orders") || path.startsWith("/api/orders/")) {
      Customer user = requireCustomer(session);
      String id = recordId(path, "/api/orders");
      if (method.equals("GET"))
        return id == null
            ? WebsiteData.orders(orders.getOrders(user), orders, restaurants)
            : WebsiteData.order(orders.owned(id, user), orders, restaurants);
      if (id == null && method.equals("POST"))
        return WebsiteData.order(
            orders.placeOrder(user, f.items, f.address, f.phone, f.requestKey, f.quotedTotalCents),
            orders,
            restaurants);
      if (id != null && method.equals("PUT")) {
        if ("CANCELLED".equals(f.status)) orders.cancelOrder(id, user);
        else orders.updateOrderStatus(id, user, f.status);
        return message("Order updated.");
      }
      if (id != null && method.equals("DELETE")) {
        orders.deleteOrder(id, user);
        return message("Cancelled order removed.");
      }
    }
    // MEMBER 5: courier assignments.
    if (path.equals("/api/manage/deliveries") || path.startsWith("/api/manage/deliveries/")) {
      requireManager(session);
      String id = recordId(path, "/api/manage/deliveries");
      if (id == null && method.equals("GET")) return deliveries.getDeliveries();
      if (id == null && method.equals("POST"))
        return deliveries.assignDelivery(f.orderId, f.driverName, f.driverPhone, f.status);
      if (id != null && method.equals("PUT"))
        return deliveries.updateDeliveryStatus(
            id, f.orderId, f.driverName, f.driverPhone, f.status);
      if (id != null && method.equals("DELETE")) {
        deliveries.deleteAssignment(id);
        return message("Assignment removed.");
      }
    }
    // MEMBER 6: verified customer reviews.
    if (path.equals("/api/reviews") || path.startsWith("/api/reviews/")) {
      String id = recordId(path, "/api/reviews");
      if (id == null && method.equals("GET")) return WebsiteData.reviews(reviews, customers);
      Customer user = requireCustomer(session);
      if (id == null && method.equals("POST"))
        return reviews.addReview(user, f.orderId, f.rating, f.comment);
      if (id != null && method.equals("PUT"))
        return reviews.updateReview(id, user, f.orderId, f.rating, f.comment);
      if (id != null && method.equals("DELETE")) {
        reviews.deleteReview(id, user);
        return message("Review removed.");
      }
    }
    throw new Problem(404, "Page or API action not found.");
  }

  private String recordId(String path, String base) {
    if (path.equals(base)) return null;
    String id = path.substring(base.length() + 1);
    if (id.isEmpty() || id.contains("/")) throw new Problem(404, "Record not found.");
    return id;
  }

  private Map<String, String> message(String text) {
    return Map.of("message", text);
  }

  private Customer requireCustomer(Session session) throws IOException {
    Customer user = customers.findCustomer(session.customerId);
    if (user == null) throw new Problem(401, "Please log in.");
    return user;
  }

  private void requireManager(Session session) throws IOException {
    if (!requireCustomer(session).getRole().equals("MANAGER"))
      throw new Problem(403, "Manager access only.");
  }

  private Object sessionInfo(Session session) throws IOException {
    Customer user = customers.findCustomer(session.customerId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("csrf", session.csrf);
    result.put("user", user == null ? null : WebsiteData.customer(user));
    return result;
  }

  private String cookieId(HttpExchange request) {
    String cookies = request.getRequestHeaders().getFirst("Cookie");
    if (cookies != null)
      for (String cookie : cookies.split(";")) {
        String c = cookie.trim();
        if (c.startsWith("FDSESSION=")) return c.substring(10);
      }
    return "";
  }

  private Session session(HttpExchange request) {
    // Remove expired sessions before accepting a new request.
    long now = System.currentTimeMillis();
    Iterator<Session> iterator = sessions.values().iterator();
    while (iterator.hasNext()) if (iterator.next().expires < now) iterator.remove();
    Session session = sessions.get(cookieId(request));
    if (session == null) return createSession(request);
    session.expires = now + 30 * 60 * 1000;
    return session;
  }

  private Session createSession(HttpExchange request) {
    Session session = new Session();
    String id = UUID.randomUUID().toString();
    sessions.put(id, session);
    request
        .getResponseHeaders()
        .set("Set-Cookie", "FDSESSION=" + id + "; Path=/; HttpOnly; SameSite=Lax");
    return session;
  }

  private void removeSession(HttpExchange request) {
    sessions.remove(cookieId(request));
    request
        .getResponseHeaders()
        .set("Set-Cookie", "FDSESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
  }

  private void sendJson(HttpExchange request, int status, Object value) throws IOException {
    byte[] bytes = files.json.toJson(value).getBytes(StandardCharsets.UTF_8);
    request.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    request.sendResponseHeaders(status, bytes.length);
    request.getResponseBody().write(bytes);
  }

  private void servePage(HttpExchange request, String path) throws IOException {
    if (!request.getRequestMethod().equals("GET"))
      throw new Problem(405, "Use GET to read a page.");
    // Friendly URLs point to ordinary HTML files.
    switch (path) {
      case "/":
        path = "/index.html";
        break;
      case "/login":
      case "/register":
        path = "/auth.html";
        break;
      case "/foods":
      case "/menu":
        path = "/menu.html";
        break;
      case "/profile/edit":
        path = "/profile.html";
        break;
      case "/profile":
      case "/dashboard":
      case "/restaurants":
      case "/cart":
      case "/orders":
      case "/reviews":
      case "/manage":
        path += ".html";
        break;
    }
    Path root = Path.of("web").toRealPath();
    Path file = root.resolve(path.substring(1)).normalize();
    if (!file.startsWith(root) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root))
      throw new Problem(404, "File not found.");
    String type = "application/octet-stream";
    if (path.endsWith(".html")) type = "text/html; charset=utf-8";
    else if (path.endsWith(".css")) type = "text/css; charset=utf-8";
    else if (path.endsWith(".js")) type = "text/javascript; charset=utf-8";
    else if (path.endsWith(".png")) type = "image/png";
    else if (path.endsWith(".jpg")) type = "image/jpeg";
    else if (path.endsWith(".svg")) type = "image/svg+xml";
    byte[] bytes = Files.readAllBytes(file);
    request.getResponseHeaders().set("Content-Type", type);
    request.sendResponseHeaders(200, bytes.length);
    request.getResponseBody().write(bytes);
  }
}
