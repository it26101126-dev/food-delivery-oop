import { ready, api, foodCard, bindFoodCards, showEmpty } from "./common.js";
await ready;
const box = document.querySelector("#home-foods");
try {
  const foods = (await api("/api/foods"))
    .filter((f) => f.orderable)
    .slice(0, 3);
  box.innerHTML = foods.map(foodCard).join("");
  bindFoodCards(box, foods);
  if (!foods.length)
    showEmpty(box, "The kitchen is taking a break", "Please check back soon.");
} catch (e) {
  showEmpty(box, "Menu unavailable", e.message);
}
