import {
  ready,
  api,
  esc,
  foodCard,
  bindFoodCards,
  showEmpty,
  money,
  openEditor,
  photoOptions,
  managerTable,
  confirmAction,
  toast,
} from "./common.js";
export async function renderFoodManager(container) {
  const [foods, restaurants] = await Promise.all([
    api("/api/foods"),
    api("/api/restaurants"),
  ]);
  container.innerHTML = `<div class="section-title"><div><p class="eyebrow">MENU MANAGEMENT</p><h2>Food items</h2></div><button class="button" id="add-food">+ Add food</button></div>${managerTable(
    ["Food", "Restaurant", "Price", "Status", "Actions"],
    foods.map(
      (f) =>
        `<tr><td><strong>${esc(f.name)}</strong><br><span class="muted">${esc(f.category)}</span></td><td>${esc(f.restaurantName)}</td><td>${money(f.priceCents)}</td><td>${f.available ? "Available" : "Sold out"}</td><td><div class="table-actions"><button class="button small secondary" data-edit="${esc(f.id)}">Edit</button><button class="button small secondary" data-delete="${esc(f.id)}">Delete</button></div></td></tr>`,
    ),
  )}`;
  function edit(f = { available: true, image: "rice" }) {
    if (!restaurants.length) {
      toast("Add a restaurant first.");
      return;
    }
    openEditor(
      f.id ? "Edit food" : "Add food",
      [
        {
          name: "restaurantId",
          label: "Restaurant",
          type: "select",
          value: f.restaurantId,
          options: restaurants.map((r) => ({ value: r.id, label: r.name })),
        },
        { name: "name", label: "Food name", value: f.name },
        { name: "category", label: "Category", value: f.category },
        {
          name: "description",
          label: "Description",
          type: "textarea",
          value: f.description,
        },
        {
          name: "price",
          label: "Price (LKR)",
          type: "number",
          value: f.priceCents ? f.priceCents / 100 : "",
        },
        {
          name: "image",
          label: "Photo",
          type: "select",
          value: f.image,
          options: photoOptions,
        },
        {
          name: "available",
          label: "Available to order",
          type: "checkbox",
          value: f.available,
        },
      ],
      async (values) => {
        await api(
          "/api/manage/foods" + (f.id ? "/" + f.id : ""),
          f.id ? "PUT" : "POST",
          values,
        );
        await renderFoodManager(container);
      },
    );
  }
  container.querySelector("#add-food").onclick = () => edit();
  container
    .querySelectorAll("[data-edit]")
    .forEach(
      (b) =>
        (b.onclick = () => edit(foods.find((f) => f.id === b.dataset.edit))),
    );
  container.querySelectorAll("[data-delete]").forEach(
    (b) =>
      (b.onclick = async () => {
        if (
          await confirmAction(
            "Delete this food?",
            "Existing orders keep their original item details.",
            "Delete food",
          )
        ) {
          try {
            await api("/api/manage/foods/" + b.dataset.delete, "DELETE");
            await renderFoodManager(container);
          } catch (e) {
            toast(e.message);
          }
        }
      }),
  );
}
if (document.querySelector("#food-list")) {
  await ready;
  const box = document.querySelector("#food-list");
  try {
    const [foods, restaurants] = await Promise.all([
      api("/api/foods"),
      api("/api/restaurants"),
    ]);
    const restaurant = document.querySelector("#restaurant-filter"),
      category = document.querySelector("#category-filter"),
      search = document.querySelector("#food-search");
    restaurant.innerHTML =
      '<option value="">All restaurants</option>' +
      restaurants
        .map((r) => `<option value="${esc(r.id)}">${esc(r.name)}</option>`)
        .join("");
    category.innerHTML =
      '<option value="">Every craving</option>' +
      [...new Set(foods.map((f) => f.category))]
        .map((c) => `<option>${esc(c)}</option>`)
        .join("");
    restaurant.value =
      new URLSearchParams(location.search).get("restaurant") || "";
    function render() {
      const list = foods.filter(
        (f) =>
          (!restaurant.value || f.restaurantId === restaurant.value) &&
          (!category.value || f.category === category.value) &&
          (f.name + " " + f.description)
            .toLowerCase()
            .includes(search.value.toLowerCase()),
      );
      document.querySelector("#menu-count").textContent =
        list.length + " meals to explore";
      box.innerHTML = list.map(foodCard).join("");
      bindFoodCards(box, list);
      if (!list.length)
        showEmpty(box, "No meals found", "Try another search or filter.");
    }
    [restaurant, category].forEach((el) => (el.onchange = render));
    search.oninput = render;
    render();
  } catch (e) {
    showEmpty(box, "Could not load menu", e.message);
  }
}
