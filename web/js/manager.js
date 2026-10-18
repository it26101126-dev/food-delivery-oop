import {
  ready,
  session,
  api,
  requireUser,
  esc,
  managerTable,
  showEmpty,
} from "./common.js";
import { renderRestaurantManager } from "./restaurants.js";
import { renderFoodManager } from "./foods.js";
import { renderOrderManager } from "./orders.js";
import { renderDeliveryManager } from "./deliveries.js";
import { renderReviewManager } from "./reviews.js";
await ready;
const root = document.querySelector("#manager-root");
if (requireUser()) {
  if (session.user.role !== "MANAGER") {
    showEmpty(
      root,
      "Manager access only",
      "Your account can browse the menu, place orders and leave reviews.",
      "/foods",
      "Explore the menu",
    );
  } else {
    const tabs = {
      restaurants: "Restaurants",
      foods: "Food menu",
      orders: "Orders",
      deliveries: "Deliveries",
      reviews: "Reviews",
      customers: "Customers",
    };
    let tab = new URLSearchParams(location.search).get("tab") || "orders";
    if (!tabs[tab]) tab = "orders";
    root.innerHTML = `<div class="manager-layout"><nav class="manager-nav" aria-label="Management sections">${Object.entries(
      tabs,
    )
      .map(
        ([key, label]) =>
          `<a href="/manage?tab=${key}" class="${tab === key ? "active" : ""}">${label}</a>`,
      )
      .join(
        "",
      )}</nav><section class="panel manager-content" id="manager-content"><p class="loading">Loading ${tabs[tab].toLowerCase()}…</p></section></div>`;
    const content = document.querySelector("#manager-content");
    try {
      const renderers = {
        restaurants: renderRestaurantManager,
        foods: renderFoodManager,
        orders: renderOrderManager,
        deliveries: renderDeliveryManager,
        reviews: renderReviewManager,
      };
      if (renderers[tab]) await renderers[tab](content);
      else {
        const customers = await api("/api/manage/customers");
        content.innerHTML = `<h2>Customers</h2><p class="small-text">Customers manage their own profile and account deletion.</p>${managerTable(
          ["Name", "Email", "Phone", "Role"],
          customers.map(
            (c) =>
              `<tr><td>${esc(c.name)}</td><td>${esc(c.email)}</td><td>${esc(c.phone)}</td><td>${esc(c.role)}</td></tr>`,
          ),
        )}`;
      }
    } catch (e) {
      showEmpty(content, "Could not load this section", e.message);
    }
  }
}
