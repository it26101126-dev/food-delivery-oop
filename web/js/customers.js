import {
  ready,
  session,
  api,
  esc,
  formData,
  requireUser,
  showError,
  toast,
  refreshSession,
  showEmpty,
} from "./common.js";
await ready;
const authBox = document.querySelector("#auth-box");
if (authBox) {
  const register = location.pathname === "/register";
  authBox.innerHTML = `<p class="eyebrow">${register ? "JOIN THE TABLE" : "GOOD TO SEE YOU AGAIN"}</p><h2>${register ? "Create your account" : "Welcome back."}</h2><p class="small-text">${register ? "A few details, then you’re ready to order." : "Your next delicious meal starts here."}</p>${new URLSearchParams(location.search).has("registered") ? '<p class="notice success">Account created. Log in to get started.</p>' : ""}<form id="auth-form"><div class="form-error" role="alert"></div>${register ? '<div class="field"><label for="name">Full name</label><input id="name" name="name" autocomplete="name" maxlength="100" required></div>' : ""}<div class="field"><label for="email">Email address</label><input id="email" name="email" type="email" autocomplete="username" maxlength="150" required></div>${register ? '<div class="form-row"><div class="field"><label for="phone">Phone number</label><input id="phone" name="phone" type="tel" autocomplete="tel" placeholder="0771234567" maxlength="16" required></div><div class="field"><label for="address">Delivery address</label><input id="address" name="address" autocomplete="street-address" maxlength="250" required></div></div>' : ""}<div class="field"><label for="password">Password</label><div class="password-field"><input id="password" name="password" type="password" autocomplete="${register ? "new-password" : "current-password"}" ${register ? 'minlength="8"' : ""} required><button class="password-toggle" type="button" aria-label="Show password">Show</button></div></div>${register ? '<div class="field"><label for="confirmPassword">Confirm password</label><input id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="8" required></div>' : ""}<button class="button" type="submit">${register ? "Create account" : "Log in"} →</button></form><p class="auth-switch">${register ? 'Already have an account? <a class="text-link" href="/login">Log in</a>' : 'New around here? <a class="text-link" href="/register">Create an account</a>'}</p>`;
  authBox.querySelector(".password-toggle").onclick = (event) => {
    const input = document.querySelector("#password");
    const visible = input.type === "password";
    input.type = visible ? "text" : "password";
    event.currentTarget.textContent = visible ? "Hide" : "Show";
    event.currentTarget.setAttribute(
      "aria-label",
      visible ? "Hide password" : "Show password",
    );
  };
  document.querySelector("#auth-form").onsubmit = async (event) => {
    event.preventDefault();
    const form = event.currentTarget,
      button = form.querySelector("[type=submit]");
    button.disabled = true;
    form.querySelector(".form-error").textContent = "";
    try {
      await api(
        register ? "/api/register" : "/api/login",
        "POST",
        formData(form),
      );
      if (register) location.href = "/login?registered=1";
      else {
        const next = new URLSearchParams(location.search).get("next");
        const allowed = [
          "/cart",
          "/orders",
          "/profile",
          "/dashboard",
          "/manage",
          "/reviews",
        ];
        location.href =
          next && allowed.includes(next.split("?")[0]) ? next : "/dashboard";
      }
    } catch (e) {
      showError(form.querySelector(".form-error"), e);
      button.disabled = false;
    }
  };
}
const profile = document.querySelector("#profile-content");
if (profile && requireUser()) {
  const user = session.user;
  profile.innerHTML = `<div class="profile-layout"><section class="panel"><h2>Your details</h2><form id="profile-form"><div class="form-error" role="alert"></div><div class="field"><label for="name">Full name</label><input id="name" name="name" value="${esc(user.name)}" autocomplete="name" maxlength="100" required></div><div class="field"><label for="email">Email address</label><input id="email" type="email" value="${esc(user.email)}" readonly><small>Your sign-in email stays the same.</small></div><div class="form-row"><div class="field"><label for="phone">Phone number</label><input id="phone" name="phone" type="tel" value="${esc(user.phone)}" maxlength="16" autocomplete="tel" required></div><div class="field"><label for="address">Delivery address</label><input id="address" name="address" value="${esc(user.address)}" maxlength="250" autocomplete="street-address" required></div></div><div class="actions"><button class="button" type="submit">Save my details</button><a class="button secondary" href="/dashboard">Back to account</a></div></form></section><aside class="profile-summary"><div class="avatar">${esc(user.name[0])}</div><h3>${esc(user.name)}</h3><p class="small-text">${user.role === "MANAGER" ? "Manager account" : "Ready for your next great meal."}</p><a class="text-link" href="/orders">View my orders →</a>${user.role === "CUSTOMER" ? '<div class="danger-zone"><h3>Close your account</h3><p class="small-text">Complete or cancel active orders first. Completed order history is retained.</p><button class="button small secondary" id="delete-account">Delete account</button></div>' : ""}</aside></div>`;
  document.querySelector("#profile-form").onsubmit = async (event) => {
    event.preventDefault();
    const form = event.currentTarget,
      button = form.querySelector("[type=submit]");
    button.disabled = true;
    try {
      await api("/api/profile", "PUT", formData(form));
      await refreshSession();
      toast("Your profile has been updated.");
    } catch (e) {
      showError(form.querySelector(".form-error"), e);
    } finally {
      button.disabled = false;
    }
  };
  document.querySelector("#delete-account")?.addEventListener("click", () => {
    const d = document.createElement("dialog");
    d.innerHTML =
      '<h2>Delete your account?</h2><p>This permanently removes your sign-in account. You will need to register again.</p><form><div class="form-error" role="alert"></div><div class="field"><label for="delete-password">Confirm your password</label><input id="delete-password" name="password" type="password" autocomplete="current-password" required></div><label class="check-field"><input name="confirmed" type="checkbox" required>I want to delete my account.</label><div class="actions"><button class="button danger" type="submit">Delete account</button><button class="button secondary" type="button" data-cancel>Keep my account</button></div></form>';
    document.body.append(d);
    d.querySelector("[data-cancel]").onclick = () => d.close();
    d.addEventListener("close", () => d.remove());
    d.querySelector("form").onsubmit = async (event) => {
      event.preventDefault();
      const form = event.currentTarget,
        button = form.querySelector("[type=submit]");
      button.disabled = true;
      try {
        await api("/api/profile", "DELETE", {
          password: form.password.value,
          confirmed: form.confirmed.checked,
        });
        location.href = "/";
      } catch (e) {
        showError(form.querySelector(".form-error"), e);
        button.disabled = false;
      }
    };
    d.showModal();
  });
}
const dashboard = document.querySelector("#dashboard-content");
if (dashboard && requireUser()) {
  try {
    const orders = await api("/api/orders"),
      user = session.user;
    const own = orders.filter((o) => o.customerId === user.id),
      active = own.filter(
        (o) => !["DELIVERED", "CANCELLED"].includes(o.status),
      );
    const links = [
      [
        "01",
        "Find your next meal",
        "Explore the menu and build your basket.",
        "/foods",
      ],
      [
        "02",
        "Follow your orders",
        "Check progress and view your order history.",
        "/orders",
      ],
      [
        "03",
        "My profile",
        "Update your name and delivery details.",
        "/profile",
      ],
      [
        "04",
        "Discover restaurants",
        "Meet the kitchens behind your favourites.",
        "/restaurants",
      ],
      [
        "05",
        "Share your thoughts",
        "Review the meals you have received.",
        "/reviews",
      ],
      [
        "06",
        user.role === "MANAGER" ? "Manage the kitchen" : "Your basket",
        user.role === "MANAGER"
          ? "Restaurants, food, orders and delivery."
          : "Your favourites, ready for checkout.",
        user.role === "MANAGER" ? "/manage" : "/cart",
      ],
    ];
    dashboard.innerHTML = `<div class="page-head"><p class="eyebrow">YOUR FOODDELIVERY ACCOUNT</p><h1>Hello, ${esc(user.name.split(" ")[0])}.</h1><div class="account-strip"><p>What sounds good today?</p><button class="button secondary small" id="logout">Log out</button></div><div class="stats-row"><div class="stat"><strong>${own.length}</strong><span>Your orders</span></div><div class="stat"><strong>${active.length}</strong><span>In progress</span></div><div class="stat"><strong>${own.filter((o) => o.status === "DELIVERED").length}</strong><span>Delivered with care</span></div></div></div><div class="dashboard-grid">${links.map(([n, title, text, url]) => `<a class="card dashboard-card" href="${url}"><span class="number">${n} /</span><h3>${title} ↗</h3><p>${text}</p></a>`).join("")}</div>`;
    document.querySelector("#logout").onclick = async () => {
      try {
        await api("/api/logout", "POST", {});
        location.href = "/login";
      } catch (e) {
        toast(e.message);
      }
    };
  } catch (e) {
    showEmpty(dashboard, "Could not load account", e.message);
  }
}
