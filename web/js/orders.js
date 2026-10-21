import {
  ready,
  session,
  api,
  requireUser,
  esc,
  money,
  shortId,
  date,
  confirmAction,
  toast,
  showEmpty,
  managerTable,
} from "./common.js";
const stages = [
  "PLACED",
  "CONFIRMED",
  "PREPARING",
  "READY",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
];
export function statusName(status) {
  return status
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (c) => c.toUpperCase());
}
export async function renderOrderManager(container) {
  const orders = await api("/api/orders");
  container.innerHTML = `<div class="section-title"><h2>Orders</h2><button class="button secondary" id="refresh-manager-orders">↻ Refresh</button></div><p class="small-text">Move orders from placed → confirmed → preparing → ready. Assign a courier in Deliveries when ready.</p>${managerTable(
    ["Order", "Customer", "Items / total", "Status", "Next step"],
    orders.map((o) => {
      const next = {
        PLACED: "CONFIRMED",
        CONFIRMED: "PREPARING",
        PREPARING: "READY",
      }[o.status];
      return `<tr><td><strong>${shortId(o.id)}</strong><br>${date(o.createdAt)}<br>${esc(o.restaurantName)}</td><td>${esc(o.customerName)}<br><span class="muted">${esc(o.address)}<br>${esc(o.phone)}</span></td><td>${o.items.map((i) => esc(i.name) + " × " + i.quantity).join("<br>")}<br><strong>${money(o.totalCents)}</strong></td><td><span class="tag">${statusName(o.status)}</span></td><td><div class="table-actions">${next ? `<button class="button small" data-status="${next}" data-id="${esc(o.id)}">${statusName(next)}</button>` : ""}${["PLACED", "CONFIRMED"].includes(o.status) ? `<button class="button small secondary" data-status="CANCELLED" data-id="${esc(o.id)}">Cancel</button>` : ""}${o.status === "READY" ? '<a class="text-link" href="/manage?tab=deliveries">Assign courier →</a>' : ""}${o.status === "CANCELLED" ? `<button class="button small secondary" data-delete="${esc(o.id)}">Delete</button>` : ""}</div></td></tr>`;
    }),
  )}`;
  container.querySelector("#refresh-manager-orders").onclick = () =>
    renderOrderManager(container).catch((e) => toast(e.message));
  bindOrderActions(container, () => renderOrderManager(container));
}
function bindOrderActions(container, reload) {
  container.querySelectorAll("[data-status]").forEach(
    (b) =>
      (b.onclick = async () => {
        if (
          b.dataset.status === "CANCELLED" &&
          !(await confirmAction(
            "Cancel this order?",
            "This request will not be prepared.",
            "Cancel order",
          ))
        )
          return;
        b.disabled = true;
        try {
          await api("/api/orders/" + b.dataset.id, "PUT", {
            status: b.dataset.status,
          });
          await reload();
        } catch (e) {
          toast(e.message);
          b.disabled = false;
        }
      }),
  );
  container.querySelectorAll("[data-delete]").forEach(
    (b) =>
      (b.onclick = async () => {
        if (
          !(await confirmAction(
            "Remove cancelled order?",
            "This removes the cancelled order from the saved history.",
            "Remove order",
          ))
        )
          return;
        try {
          await api("/api/orders/" + b.dataset.delete, "DELETE");
          await reload();
        } catch (e) {
          toast(e.message);
        }
      }),
  );
}
if (document.querySelector("#orders-list")) {
  await ready;
  if (requireUser()) {
    const box = document.querySelector("#orders-list");
    let selected = "all";
    async function render() {
      try {
        let orders = (await api("/api/orders")).filter(
          (o) => o.customerId === session.user.id,
        );
        if (selected === "active")
          orders = orders.filter(
            (o) => !["DELIVERED", "CANCELLED"].includes(o.status),
          );
        if (selected === "delivered")
          orders = orders.filter((o) => o.status === "DELIVERED");
        if (selected === "cancelled")
          orders = orders.filter((o) => o.status === "CANCELLED");
        if (!orders.length) {
          showEmpty(
            box,
            "No orders here yet.",
            "Your next favourite is waiting on the menu.",
            "/foods",
            "Explore the menu",
          );
          return;
        }
        box.innerHTML = orders
          .map(
            (o) =>
              `<article class="card order-card ${new URLSearchParams(location.search).get("highlight") === o.id ? "highlight" : ""}"><div class="order-top"><div><span class="eyebrow">${shortId(o.id)}</span><h3>${esc(o.restaurantName)}</h3><span class="order-meta">${date(o.createdAt)} · Cash on delivery</span></div><span class="tag ${o.status === "DELIVERED" ? "green" : o.status === "CANCELLED" ? "grey" : ""}">${statusName(o.status)}</span></div>${o.status !== "CANCELLED" ? `<div class="status-track" aria-label="Order status: ${statusName(o.status)}">${stages.map((s, i) => `<span class="status-step ${i <= stages.indexOf(o.status) ? "done" : ""}">${statusName(s)}</span>`).join("")}</div>` : ""}<div class="order-items">${o.items.map((i) => `<div><span>${i.quantity} × ${esc(i.name)}</span><span>${money(i.totalCents)}</span></div>`).join("")}<div class="muted"><span>Delivery</span><span>${money(o.deliveryCents)}</span></div></div><p class="order-meta">Deliver to: ${esc(o.address)} · ${esc(o.phone)}</p><div class="order-bottom"><strong>${money(o.totalCents)}</strong><div class="actions">${["PLACED", "CONFIRMED"].includes(o.status) ? `<button class="button secondary small" data-status="CANCELLED" data-id="${esc(o.id)}">Cancel order</button>` : ""}${o.status === "CANCELLED" ? `<button class="button secondary small" data-delete="${esc(o.id)}">Remove from history</button>` : ""}${o.status === "DELIVERED" ? `<a class="button small" href="/reviews?order=${encodeURIComponent(o.id)}">Write a review</a>` : ""}</div></div>${o.delivery ? `<div class="delivery-info"><strong>Your courier: ${esc(o.delivery.driverName)}</strong><br>Phone: ${esc(o.delivery.driverPhone)} · ${statusName(o.delivery.status)}</div>` : ""}</article>`,
          )
          .join("");
        bindOrderActions(box, render);
      } catch (e) {
        showEmpty(box, "Could not load orders", e.message);
      }
    }
    document.querySelectorAll("[data-filter]").forEach(
      (b) =>
        (b.onclick = () => {
          selected = b.dataset.filter;
          document
            .querySelectorAll("[data-filter]")
            .forEach((x) => x.classList.toggle("active", x === b));
          render();
        }),
    );
    document.querySelector("#refresh-orders").onclick = render;
    await render();
  }
}
