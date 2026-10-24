import {
  ready,
  api,
  esc,
  openEditor,
  photoOptions,
  managerTable,
  confirmAction,
  toast,
  showEmpty,
} from "./common.js";
export async function renderRestaurantManager(container) {
  const restaurants = await api("/api/restaurants");
  container.innerHTML = `<div class="section-title"><h2>Restaurants</h2><button class="button" id="add-restaurant">+ Add restaurant</button></div>${managerTable(
    ["Restaurant", "Cuisine", "Phone", "Status", "Actions"],
    restaurants.map(
      (r) =>
        `<tr><td>${esc(r.name)}</td><td>${esc(r.cuisine)}</td><td>${esc(r.phone)}</td><td>${r.open ? "Open" : "Closed"}</td><td><div class="table-actions"><button class="button small secondary" data-edit="${esc(r.id)}">Edit</button><button class="button small secondary" data-delete="${esc(r.id)}">Delete</button></div></td></tr>`,
    ),
  )}`;
  function edit(r = { open: true, image: "rice" }) {
    openEditor(
      r.id ? "Edit restaurant" : "Add restaurant",
      [
        { name: "name", label: "Restaurant name", value: r.name },
        { name: "cuisine", label: "Cuisine", value: r.cuisine, max: 60 },
        { name: "address", label: "Address", value: r.address, max: 250 },
        { name: "phone", label: "Phone", type: "tel", value: r.phone, max: 16 },
        {
          name: "image",
          label: "Cover photo",
          type: "select",
          value: r.image,
          options: photoOptions,
        },
        {
          name: "open",
          label: "Open for orders",
          type: "checkbox",
          value: r.open,
        },
      ],
      async (values) => {
        await api(
          "/api/manage/restaurants" + (r.id ? "/" + r.id : ""),
          r.id ? "PUT" : "POST",
          values,
        );
        await renderRestaurantManager(container);
      },
    );
  }
  container.querySelector("#add-restaurant").onclick = () => edit();
  container
    .querySelectorAll("[data-edit]")
    .forEach(
      (b) =>
        (b.onclick = () =>
          edit(restaurants.find((r) => r.id === b.dataset.edit))),
    );
  container.querySelectorAll("[data-delete]").forEach(
    (b) =>
      (b.onclick = async () => {
        if (
          await confirmAction(
            "Delete this restaurant?",
            "A restaurant with menu items cannot be deleted. You can mark it closed instead.",
            "Delete restaurant",
          )
        ) {
          try {
            await api("/api/manage/restaurants/" + b.dataset.delete, "DELETE");
            await renderRestaurantManager(container);
          } catch (e) {
            toast(e.message);
          }
        }
      }),
  );
}
if (document.querySelector("#restaurant-list")) {
  await ready;
  const box = document.querySelector("#restaurant-list");
  try {
    const restaurants = await api("/api/restaurants");
    box.innerHTML = restaurants
      .map(
        (r) =>
          `<article class="card restaurant-card"><div class="card-image"><img class="food-image" src="/images/${esc(r.image)}.jpg" alt="${esc(r.cuisine)} food" loading="lazy"></div><div class="card-body"><span class="tag ${r.open ? "green" : "grey"}">${r.open ? "Open for orders" : "Currently closed"}</span><h3>${esc(r.name)}</h3><p>${esc(r.cuisine)}</p><p>${esc(r.address)}</p><div class="card-bottom"><span class="small-text">${esc(r.phone)}</span><a class="text-link" href="/foods?restaurant=${esc(r.id)}">View menu →</a></div></div></article>`,
      )
      .join("");
    if (!restaurants.length)
      showEmpty(box, "No restaurants yet", "Please check back soon.");
  } catch (e) {
    showEmpty(box, "Could not load restaurants", e.message);
  }
}
