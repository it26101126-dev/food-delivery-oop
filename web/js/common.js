export let session = { user: null, csrf: "" };
export const money = (cents) =>
  "LKR " +
  (Number(cents) / 100).toLocaleString("en-LK", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
export const esc = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
export const shortId = (id) => String(id).slice(0, 10).toUpperCase();
export const date = (value) =>
  new Date(value).toLocaleString("en-LK", {
    dateStyle: "medium",
    timeStyle: "short",
  });
export function toast(message) {
  const box = document.querySelector("#toast");
  box.textContent = message;
  box.classList.add("show");
  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => box.classList.remove("show"), 3500);
}
export async function api(path, method = "GET", body) {
  const response = await fetch(path, {
    method,
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": session.csrf,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response
    .json()
    .catch(() => ({ error: "The server returned an unexpected response." }));
  if (!response.ok) {
    const error = new Error(data.error || "Request failed. Please try again.");
    error.status = response.status;
    throw error;
  }
  return data;
}
export async function refreshSession() {
  session = await api("/api/session");
  return session;
}
export function cart() {
  try {
    const data = JSON.parse(
      localStorage.getItem("fooddelivery-simple-cart-v1") || "[]",
    );
    return Array.isArray(data)
      ? data.filter(
          (x) =>
            typeof x.foodId === "string" &&
            Number.isInteger(x.quantity) &&
            x.quantity > 0 &&
            x.quantity <= 20,
        )
      : [];
  } catch {
    return [];
  }
}
export function saveCart(items) {
  localStorage.setItem("fooddelivery-simple-cart-v1", JSON.stringify(items));
  updateCartCount();
}
export function updateCartCount() {
  document
    .querySelectorAll(".cart-count")
    .forEach(
      (el) => (el.textContent = cart().reduce((n, i) => n + i.quantity, 0)),
    );
}
export async function addToCart(food) {
  let items = cart();
  const all = await api("/api/foods");
  const first = all.find((x) => x.id === items[0]?.foodId);
  if (first && first.restaurantId !== food.restaurantId) {
    if (
      !(await confirmAction(
        "Start a new basket?",
        "An order can contain food from one restaurant. Replace the current basket?",
        "Start new basket",
      ))
    )
      return;
    items = [];
  }
  const existing = items.find((x) => x.foodId === food.id);
  if (existing) {
    if (existing.quantity >= 20) {
      toast("Maximum 20 of each item.");
      return;
    }
    existing.quantity++;
  } else items.push({ foodId: food.id, quantity: 1 });
  saveCart(items);
  toast(food.name + " added to your basket.");
}
export function foodCard(food) {
  return `<article class="card food-card"><div class="card-image"><img class="food-image" src="/images/${esc(food.image)}.jpg" alt="${esc(food.name)}" loading="lazy"></div><div class="card-body"><span class="tag">${esc(food.category)}</span><p class="small-text muted" style="margin:10px 0 0">${esc(food.restaurantName)}</p><h3>${esc(food.name)}</h3><p>${esc(food.description)}</p><div class="card-bottom"><span class="price">${money(food.priceCents)}</span><button class="button small" data-add="${esc(food.id)}" ${food.orderable ? "" : "disabled"}>${food.orderable ? "+ Add" : "Unavailable"}</button></div></div></article>`;
}
export function bindFoodCards(container, foods) {
  container.querySelectorAll("[data-add]").forEach(
    (button) =>
      (button.onclick = async () => {
        button.disabled = true;
        try {
          await addToCart(foods.find((f) => f.id === button.dataset.add));
        } catch (e) {
          toast(e.message);
        } finally {
          button.disabled = false;
        }
      }),
  );
}
export function requireUser() {
  if (session.user) return session.user;
  location.href =
    "/login?next=" + encodeURIComponent(location.pathname + location.search);
  return null;
}
export function formData(form) {
  return Object.fromEntries(new FormData(form));
}
export function showError(container, error) {
  container.textContent = error.message || String(error);
}
export function showEmpty(container, title, text, href, label) {
  container.innerHTML = `<div class="empty"><h3>${esc(title)}</h3><p>${esc(text)}</p>${href ? `<a class="button" href="${href}">${esc(label)}</a>` : ""}</div>`;
}
export function confirmAction(title, message, action = "Confirm") {
  return new Promise((resolve) => {
    const dialog = document.createElement("dialog");
    dialog.innerHTML = `<h2>${esc(title)}</h2><p>${esc(message)}</p><div class="actions"><button class="button danger" data-confirm>${esc(action)}</button><button class="button secondary" data-cancel>Cancel</button></div>`;
    document.body.append(dialog);
    dialog.addEventListener(
      "close",
      () => {
        const result = dialog.returnValue === "yes";
        dialog.remove();
        resolve(result);
      },
      { once: true },
    );
    dialog.querySelector("[data-confirm]").onclick = () => dialog.close("yes");
    dialog.querySelector("[data-cancel]").onclick = () => dialog.close();
    dialog.showModal();
    dialog.querySelector("[data-cancel]").focus();
  });
}
export function openEditor(title, fields, save) {
  const dialog = document.createElement("dialog");
  dialog.innerHTML = `<button class="dialog-close" aria-label="Close dialog">×</button><h2>${esc(title)}</h2><form><div class="form-error" role="alert"></div>${fields
    .map((f) => {
      if (f.type === "checkbox")
        return `<label class="check-field"><input type="checkbox" name="${f.name}" ${f.value ? "checked" : ""}>${esc(f.label)}</label>`;
      let input =
        f.type === "select"
          ? `<select name="${f.name}" required>${f.options.map((o) => `<option value="${esc(o.value)}" ${String(o.value) === String(f.value) ? "selected" : ""}>${esc(o.label)}</option>`).join("")}</select>`
          : f.type === "textarea"
            ? `<textarea name="${f.name}" maxlength="${f.max || 300}" required>${esc(f.value || "")}</textarea>`
            : `<input name="${f.name}" type="${f.type || "text"}" value="${esc(f.value ?? "")}" ${f.type === "number" ? 'step="0.01" min="0.01" max="100000"' : ""} maxlength="${f.max || 100}" required>`;
      return `<div class="field"><label>${esc(f.label)}${input}</label></div>`;
    })
    .join(
      "",
    )}<div class="actions"><button class="button" type="submit">Save changes</button><button class="button secondary" type="button" data-cancel>Cancel</button></div></form>`;
  document.body.append(dialog);
  dialog.querySelector(".dialog-close").onclick = () => dialog.close();
  dialog.querySelector("[data-cancel]").onclick = () => dialog.close();
  dialog.addEventListener("close", () => dialog.remove());
  dialog.querySelector("form").onsubmit = async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector("[type=submit]");
    button.disabled = true;
    const values = formData(form);
    fields
      .filter((f) => f.type === "checkbox")
      .forEach((f) => (values[f.name] = form.elements[f.name].checked));
    try {
      await save(values);
      dialog.close();
      toast("Changes saved.");
    } catch (e) {
      showError(form.querySelector(".form-error"), e);
    } finally {
      button.disabled = false;
    }
  };
  dialog.showModal();
}
export const photoOptions = ["rice", "burger", "pizza", "salad"].map(
  (value) => ({ value, label: value[0].toUpperCase() + value.slice(1) }),
);
export function managerTable(headers, rows) {
  return `<div class="table-wrap"><table><thead><tr>${headers.map((h) => `<th>${esc(h)}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table></div>`;
}
export async function playDelivery(order) {
  const d = document.createElement("dialog");
  d.className = "dispatch-dialog";
  d.innerHTML = `<p class="eyebrow">ORDER ${esc(shortId(order.id))}</p><h2>Your order is placed.</h2><p class="dispatch-caption">From our kitchen to your happy place.</p><div class="dispatch-stage" aria-hidden="true"><div class="dispatch-sun"></div><div class="dispatch-city"><i></i><i></i><i></i><i></i><i></i></div><div class="dispatch-kitchen"><span>THE KITCHEN</span><b></b><i></i></div><div class="dispatch-road"></div><div class="dispatch-vehicle"><img src="/images/delivery-rider.png" alt=""><div class="bag-lid"></div><div class="parcel"><span>fresh<br>for you</span></div><div class="ride-dust"><i></i><i></i><i></i></div></div></div><div class="dispatch-done" hidden><div class="dispatch-success" aria-hidden="true">✓</div><p>The restaurant has your request.<br>Follow its progress in My Orders.</p><p><strong>${money(order.totalCents)}</strong> · Cash on delivery</p><div class="actions"><a class="button" href="/orders?highlight=${encodeURIComponent(order.id)}">Track my order →</a><a class="button secondary" href="/foods">Back to menu</a></div></div>`;
  document.body.append(d);
  d.addEventListener("close", () => {
    d.remove();
    location.href = "/orders";
  });
  d.showModal();
  await new Promise((resolve) =>
    setTimeout(
      resolve,
      matchMedia("(prefers-reduced-motion: reduce)").matches ? 50 : 6200,
    ),
  );
  if (!d.isConnected) return;
  d.querySelector(".dispatch-stage").classList.add("finished");
  d.querySelector(".dispatch-caption").hidden = true;
  d.querySelector(".dispatch-done").hidden = false;
  d.querySelector(".button").focus();
}
function shell() {
  const user = session.user;
  const path = location.pathname;
  const links = [
    ["/foods", "Menu"],
    ["/restaurants", "Restaurants"],
    ["/orders", "My orders"],
    ["/reviews", "Reviews"],
  ];
  document.querySelector("#site-header").innerHTML =
    `<header class="site-header"><div class="wrap header-inner"><a class="brand" href="/"><b class="brand-mark">f</b>Food<span>Delivery.</span></a><nav class="site-nav" id="site-nav" aria-label="Main navigation">${links.map(([href, label]) => `<a href="${href}" class="${path === href ? "active" : ""}">${label}</a>`).join("")}${user?.role === "MANAGER" ? '<a href="/manage">Manage</a>' : ""}</nav><div class="nav-actions"><a class="cart-link" href="/cart" aria-label="Shopping basket"><span class="cart-word">Basket</span><span class="cart-count">0</span></a><a class="button small" href="${user ? "/dashboard" : "/login"}">${user ? "My account" : "Log in"}</a><button class="nav-toggle" aria-controls="site-nav" aria-expanded="false">Menu</button></div></div></header>`;
  document.querySelector(".nav-toggle").onclick = (event) => {
    const nav = document.querySelector("#site-nav");
    nav.classList.toggle("open");
    event.currentTarget.setAttribute(
      "aria-expanded",
      nav.classList.contains("open"),
    );
  };
  document.querySelector("#site-footer").innerHTML =
    `<footer class="footer"><div class="wrap footer-inner"><div><a class="brand" href="/">Food<span>Delivery.</span></a><p>Local flavours. Good company. Food that feels like home.</p></div><div class="footer-links"><a href="/foods">Our menu</a><a href="/restaurants">Restaurants</a><a href="/reviews">Reviews</a></div></div></footer>`;
  updateCartCount();
}
export const ready = refreshSession()
  .then(shell)
  .catch((error) => {
    shell();
    toast("Could not connect to the server. Please refresh.");
    console.error(error);
  });
