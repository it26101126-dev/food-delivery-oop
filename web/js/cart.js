import {
  ready,
  session,
  api,
  cart,
  saveCart,
  money,
  esc,
  formData,
  showEmpty,
  showError,
  playDelivery,
  toast,
} from "./common.js";
await ready;
const container = document.querySelector("#cart-content");
let requestKey = crypto.randomUUID();
async function render() {
  try {
    const foods = await api("/api/foods"),
      items = cart();
    if (!items.length) {
      showEmpty(
        container,
        "Your basket is waiting.",
        "Find a favourite and make it yours.",
        "/foods",
        "Explore the menu",
      );
      return;
    }
    const lines = items.map((item) => ({
      item,
      food: foods.find((f) => f.id === item.foodId),
    }));
    const invalid = lines.some((x) => !x.food?.orderable),
      subtotal = lines.reduce(
        (n, x) => n + (x.food?.priceCents || 0) * x.item.quantity,
        0,
      ),
      total = subtotal + 15000;
    const user = session.user;
    container.innerHTML = `<div class="checkout-layout"><section class="panel"><h2>Your picks</h2><p class="small-text">${esc(lines[0].food?.restaurantName || "Restaurant")}</p>${lines.map(({ item, food }) => `<article class="basket-item"><img src="/images/${esc(food?.image || "rice")}.jpg" alt="${esc(food?.name || "Removed food")}"><div><h3>${esc(food?.name || "Item no longer on the menu")}</h3><p class="${food?.orderable ? "" : "basket-unavailable"}">${food?.orderable ? money(food.priceCents) + " each" : "Unavailable — please remove this item"}</p><div class="quantity"><button data-quantity="${esc(item.foodId)}" data-delta="-1" aria-label="Decrease ${esc(food?.name || "item")} quantity">−</button><span>${item.quantity}</span><button data-quantity="${esc(item.foodId)}" data-delta="1" aria-label="Increase ${esc(food?.name || "item")} quantity">+</button></div><button class="remove-item" data-remove="${esc(item.foodId)}">Remove</button></div><span class="price">${money((food?.priceCents || 0) * item.quantity)}</span></article>`).join("")}<p style="margin:22px 0 0"><a class="text-link" href="/foods">← Add something else</a></p></section><section class="panel"><h2>The happy details.</h2><div class="summary-row"><span>Food subtotal</span><span>${money(subtotal)}</span></div><div class="summary-row"><span>Delivery</span><span>${money(15000)}</span></div><div class="summary-row total"><span>Total</span><span>${money(total)}</span></div>${invalid ? '<p class="notice error">Remove unavailable items before ordering.</p>' : ""}${user ? `<form id="checkout-form"><div class="form-error" role="alert"></div><div class="field"><label for="address">Delivery address</label><input id="address" name="address" value="${esc(user.address)}" maxlength="250" autocomplete="street-address" required></div><div class="field"><label for="phone">Phone number</label><input id="phone" name="phone" type="tel" value="${esc(user.phone)}" maxlength="16" autocomplete="tel" required></div><div class="payment-note"><strong>Cash on delivery</strong><br>Pay the courier when your food arrives.</div><button class="button checkout-submit" type="submit" ${invalid ? "disabled" : ""}>Place order · ${money(total)} →</button></form>` : '<p class="notice">Log in to add your delivery details and place your order.</p><a class="button checkout-submit" href="/login?next=/cart">Log in to checkout →</a>'}</section></div>`;
    container.querySelectorAll("[data-quantity]").forEach(
      (b) =>
        (b.onclick = () => {
          const changed = cart();
          const item = changed.find((x) => x.foodId === b.dataset.quantity);
          item.quantity = Math.max(
            1,
            Math.min(20, item.quantity + Number(b.dataset.delta)),
          );
          saveCart(changed);
          requestKey = crypto.randomUUID();
          render();
        }),
    );
    container.querySelectorAll("[data-remove]").forEach(
      (b) =>
        (b.onclick = () => {
          saveCart(cart().filter((x) => x.foodId !== b.dataset.remove));
          requestKey = crypto.randomUUID();
          render();
        }),
    );
    container
      .querySelector("#checkout-form")
      ?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = event.currentTarget,
          button = form.querySelector("[type=submit]");
        button.disabled = true;
        button.textContent = "Placing your order…";
        try {
          const values = formData(form);
          const order = await api("/api/orders", "POST", {
            ...values,
            items: cart(),
            requestKey,
            quotedTotalCents: total,
          });
          saveCart([]);
          await playDelivery(order);
        } catch (e) {
          showError(form.querySelector(".form-error"), e);
          button.disabled = false;
          button.textContent = "Try placing order again";
        }
      });
  } catch (e) {
    showEmpty(container, "Could not load your basket", e.message);
  }
}
await render();
