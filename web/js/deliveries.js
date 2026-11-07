import {
  api,
  esc,
  shortId,
  openEditor,
  managerTable,
  confirmAction,
  toast,
} from "./common.js";
import { statusName } from "./orders.js";
export async function renderDeliveryManager(container) {
  const [deliveries, orders] = await Promise.all([
    api("/api/manage/deliveries"),
    api("/api/orders"),
  ]);
  container.innerHTML = `<div class="section-title"><h2>Deliveries</h2><button class="button" id="add-delivery">+ Assign courier</button></div><p class="manager-note">Assign a courier to a ready order. Update to picked up, then delivered. Customers see each change in My Orders.</p>${managerTable(
    ["Order", "Courier", "Contact", "Status", "Actions"],
    deliveries.map(
      (d) =>
        `<tr><td>${shortId(d.orderId)}</td><td>${esc(d.driverName)}</td><td>${esc(d.driverPhone)}</td><td><span class="tag ${d.status === "DELIVERED" ? "green" : ""}">${statusName(d.status)}</span></td><td><div class="table-actions"><button class="button small secondary" data-edit="${esc(d.id)}">Edit / status</button>${d.status === "ASSIGNED" ? `<button class="button small secondary" data-delete="${esc(d.id)}">Remove</button>` : ""}</div></td></tr>`,
    ),
  )}`;
  function edit(d = { status: "ASSIGNED" }) {
    const ready = orders.filter(
      (o) =>
        (o.status === "READY" && !deliveries.some((x) => x.orderId === o.id)) ||
        o.id === d.orderId,
    );
    if (!ready.length) {
      toast("Move an order to Ready in the Orders tab first.");
      return;
    }
    const statuses = d.id
      ? {
          ASSIGNED: ["ASSIGNED", "PICKED_UP"],
          PICKED_UP: ["PICKED_UP", "DELIVERED"],
          DELIVERED: ["DELIVERED"],
        }[d.status]
      : ["ASSIGNED"];
    openEditor(
      d.id ? "Update delivery" : "Assign a courier",
      [
        {
          name: "orderId",
          label: "Order",
          type: "select",
          value: d.orderId,
          options: ready
            .filter((o) => !d.id || o.id === d.orderId)
            .map((o) => ({
              value: o.id,
              label: shortId(o.id) + " · " + o.customerName,
            })),
        },
        { name: "driverName", label: "Courier name", value: d.driverName },
        {
          name: "driverPhone",
          label: "Courier phone",
          type: "tel",
          max: 16,
          value: d.driverPhone,
        },
        {
          name: "status",
          label: "Delivery status",
          type: "select",
          value: d.status,
          options: statuses.map((s) => ({ value: s, label: statusName(s) })),
        },
      ],
      async (values) => {
        await api(
          "/api/manage/deliveries" + (d.id ? "/" + d.id : ""),
          d.id ? "PUT" : "POST",
          values,
        );
        await renderDeliveryManager(container);
      },
    );
  }
  container.querySelector("#add-delivery").onclick = () => edit();
  container
    .querySelectorAll("[data-edit]")
    .forEach(
      (b) =>
        (b.onclick = () =>
          edit(deliveries.find((d) => d.id === b.dataset.edit))),
    );
  container.querySelectorAll("[data-delete]").forEach(
    (b) =>
      (b.onclick = async () => {
        if (
          await confirmAction(
            "Remove this assignment?",
            "The order will remain ready for another courier.",
            "Remove assignment",
          )
        ) {
          try {
            await api("/api/manage/deliveries/" + b.dataset.delete, "DELETE");
            await renderDeliveryManager(container);
          } catch (e) {
            toast(e.message);
          }
        }
      }),
  );
}
