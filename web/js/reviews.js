import {
    ready,
    session,
    api,
    esc,
    date,
    shortId,
    formData,
    showError,
    showEmpty,
    confirmAction,
    toast,
    managerTable,
} from "./common.js";
export async function renderReviewManager(container) {
    const reviews = await api("/api/reviews");
    container.innerHTML = `<div class="section-title"><h2>Customer reviews</h2></div>${managerTable(
        ["Customer", "Rating", "Review", "Action"],
        reviews.map(
            (r) =>
                `<tr><td>${esc(r.author)}</td><td>${r.rating} / 5</td><td>${esc(r.comment)}</td><td><button class="button small secondary" data-delete="${esc(r.id)}">Delete</button></td></tr>`,
        ),
    )}`;
    container.querySelectorAll("[data-delete]").forEach(
        (b) =>
            (b.onclick = async () => {
                if (
                    await confirmAction(
                        "Remove this review?",
                        "This permanently removes the review.",
                        "Remove review",
                    )
                ) {
                    try {
                        await api("/api/reviews/" + b.dataset.delete, "DELETE");
                        await renderReviewManager(container);
                    } catch (e) {
                        toast(e.message);
                    }
                }
            }),
    );
}
if (document.querySelector("#review-list")) {
    await ready;
    let editing = null;
    const list = document.querySelector("#review-list"),
        panel = document.querySelector("#review-form-panel");
    async function render() {
        try {
            const reviews = await api("/api/reviews"),
                orders = session.user
                    ? (await api("/api/orders")).filter(
                        (o) =>
                            o.customerId === session.user.id && o.status === "DELIVERED",
                    )
                    : [];
            const average = reviews.length
                ? (
                    reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length
                ).toFixed(1)
                : null;
            document.querySelector("#reviews-summary").innerHTML = average
                ? `<div class="reviews-summary"><strong>${average} / 5</strong> · ${reviews.length} verified-order ${reviews.length === 1 ? "review" : "reviews"}</div>`
                : "";
            list.innerHTML = reviews
                .map(
                    (r) =>
                        `<article class="card review-card"><div class="review-top"><div><span class="review-stars" aria-label="${r.rating} out of 5 stars">${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}</span><h3>${esc(r.author)}</h3></div><time>${date(r.createdAt)}</time></div><p>${esc(r.comment)}</p><span class="tag green">Delivered order</span>${r.customerId === session.user?.id ? `<div class="actions"><button class="button small secondary" data-edit="${esc(r.id)}">Edit review</button><button class="button small secondary" data-delete="${esc(r.id)}">Delete</button></div>` : ""}</article>`,
                )
                .join("");
            if (!reviews.length)
                showEmpty(
                    list,
                    "The first word is yours.",
                    "Reviews will appear here after customers receive their orders.",
                );
            if (!session.user) {
                panel.innerHTML =
                    '<h2>Share your experience.</h2><p>Log in to review a delivered order.</p><a class="button" href="/login?next=/reviews">Log in</a>';
                return;
            }
            const eligible = orders.filter(
                (o) =>
                    o.id === editing?.orderId || !reviews.some((r) => r.orderId === o.id),
            );
            if (!eligible.length && !editing) {
                panel.innerHTML =
                    '<h2>Every meal has a story.</h2><p>You can review an order after it is delivered. You can also edit your existing reviews.</p><a class="text-link" href="/orders">View my orders →</a>';
            } else {
                const chosen =
                    editing?.orderId ||
                    new URLSearchParams(location.search).get("order") ||
                    "";
                panel.innerHTML = `<h2>${editing ? "Edit your review" : "Tell us about it."}</h2><form id="review-form"><div class="form-error" role="alert"></div><div class="field"><label for="review-order">Delivered order</label><select id="review-order" name="orderId" required>${eligible
                    .filter((o) => !editing || o.id === editing.orderId)
                    .map(
                        (o) =>
                            `<option value="${esc(o.id)}" ${chosen === o.id ? "selected" : ""}>${esc(o.restaurantName)} · ${shortId(o.id)}</option>`,
                    )
                    .join(
                        "",
                    )}</select></div><div class="field"><label for="rating">Your rating</label><select id="rating" name="rating">${[5, 4, 3, 2, 1].map((n) => `<option value="${n}" ${(editing?.rating || 5) === n ? "selected" : ""}>${n} ${n === 1 ? "star" : "stars"}</option>`).join("")}</select></div><div class="field"><label for="comment">Your review</label><textarea id="comment" name="comment" maxlength="1000" placeholder="What did you enjoy?" required>${esc(editing?.comment || "")}</textarea></div><div class="actions"><button class="button" type="submit">${editing ? "Save review" : "Post review"}</button>${editing ? '<button class="button secondary" type="button" id="cancel-review">Cancel</button>' : ""}</div></form>`;
                panel.querySelector("#cancel-review")?.addEventListener("click", () => {
                    editing = null;
                    render();
                });
                panel.querySelector("form").onsubmit = async (event) => {
                    event.preventDefault();
                    const form = event.currentTarget,
                        button = form.querySelector("[type=submit]");
                    button.disabled = true;
                    try {
                        const values = formData(form);
                        values.rating = Number(values.rating);
                        await api(
                            "/api/reviews" + (editing ? "/" + editing.id : ""),
                            editing ? "PUT" : "POST",
                            values,
                        );
                        editing = null;
                        await render();
                        toast("Thank you for your review.");
                    } catch (e) {
                        showError(form.querySelector(".form-error"), e);
                        button.disabled = false;
                    }
                };
            }
            list.querySelectorAll("[data-edit]").forEach(
                (b) =>
                    (b.onclick = () => {
                        editing = reviews.find((r) => r.id === b.dataset.edit);
                        render().then(() =>
                            panel.scrollIntoView({ behavior: "smooth", block: "start" }),
                        );
                    }),
            );
            list.querySelectorAll("[data-delete]").forEach(
                (b) =>
                    (b.onclick = async () => {
                        if (
                            await confirmAction(
                                "Delete your review?",
                                "You can write a new review for the same delivered order later.",
                                "Delete review",
                            )
                        ) {
                            try {
                                await api("/api/reviews/" + b.dataset.delete, "DELETE");
                                editing = null;
                                await render();
                            } catch (e) {
                                toast(e.message);
                            }
                        }
                    }),
            );
        } catch (e) {
            showEmpty(list, "Could not load reviews", e.message);
        }
    }
    await render();
}
