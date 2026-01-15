(function () {
    const role = document.getElementById("reviewRole")?.value; // BUYER / SELLER
    const apiUrl = document.getElementById("reviewListApi")?.value;
    const dealBasePath = document.getElementById("dealBasePath")?.value;

    if (!role || !apiUrl || !dealBasePath) {
        console.error("[reviews] missing config", { role, apiUrl, dealBasePath });
        return;
    }

    let reviewsCache = [];
    let currentSort = "latest"; // latest | rating

    const sortButtons = document.querySelectorAll(".review-sort-btn");

    // ---- events ----
    sortButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            const sort = btn.dataset.sort;
            if (!sort) return;

            currentSort = sort;
            applySortButtonStyle(sortButtons, btn);
            render();
        });
    });

    // ---- initial ----
    loadReviews();

    async function loadReviews() {
        showLoading();

        try {
            const res = await fetch(apiUrl);
            if (!res.ok) throw new Error(`리뷰 목록 조회 실패: ${res.status}`);

            const body = await res.json();

            // API가 배열(List<ReviewResponse>)이면 그대로, 아니면 reviews 키를 탐색
            const raw = Array.isArray(body) ? body : (body.reviews ?? body.data?.reviews ?? []);
            reviewsCache = raw.map(normalizeReview);

            render();
        } catch (e) {
            console.error("[reviews] load error", e);
            reviewsCache = [];
            render();
        } finally {
            hideLoading();
        }
    }

    function render() {
        const list = document.getElementById("reviews-list");
        const emptyState = document.getElementById("empty-state");
        const countSpan = document.getElementById("review-count"); // buyer 페이지에만 존재할 수 있음

        if (!list || !emptyState) return;

        const sorted = sortReviews([...reviewsCache], currentSort);

        // buyer count
        if (countSpan) countSpan.textContent = String(sorted.length);

        // seller stats (요소가 있을 때만)
        updateSellerStatsIfExists(sorted);

        if (!sorted.length) {
            list.classList.add("hidden");
            emptyState.classList.remove("hidden");
            list.innerHTML = "";
            return;
        }

        list.classList.remove("hidden");
        emptyState.classList.add("hidden");

        list.innerHTML = sorted.map((r) => createReviewCardHtml(r)).join("");
    }

    // ---- card ----
    function createReviewCardHtml(review) {
        const titleText = escapeHtml(review.title || "거래");
        const createdAtText = escapeHtml(formatDate(review.createdAt));
        const contentText = escapeHtml(review.content || "-");
        const buyerNickname = escapeHtml(review.buyerNickname || "-");

        const dealUrl = review.dealId ? `${dealBasePath}/${review.dealId}` : null;

        // SELLER: 구매자 정보 표시
        const sellerHeader = `
      <div class="mt-3 flex items-center gap-2">
        <div class="h-8 w-8 rounded-full bg-gray-200 overflow-hidden">
          ${review.buyerProfileImageUrl ? `<img src="${escapeHtml(review.buyerProfileImageUrl)}" class="h-full w-full object-cover" alt="profile">` : ""}
        </div>
        <div>
          <div class="text-sm font-medium text-gray-900">${buyerNickname}</div>
          <div class="text-xs text-gray-500">${createdAtText}</div>
        </div>
      </div>
    `;

        const dealTitleBlock = `
      <div class="mt-3">
        ${
            dealUrl
                ? `<a href="${dealUrl}" class="text-sm font-medium text-gray-900 hover:underline">${titleText}</a>`
                : `<span class="text-sm font-medium text-gray-900">${titleText}</span>`
        }
        ${role === "BUYER" ? `<p class="mt-1 text-xs text-gray-500">${createdAtText}</p>` : ""}
      </div>
    `;

        const actionButtons =
            role === "BUYER"
                ? `
          <div class="ml-4 flex flex-col gap-2 items-end">
            <button type="button"
                    class="inline-flex items-center px-3 py-1.5 border border-gray-300 rounded-md text-xs font-medium text-gray-700 bg-white hover:bg-gray-50"
                    onclick="goToDealFromReview('${review.dealId ?? ""}')">
              거래로 이동
            </button>  
        `
                : `
          <div class="ml-4">
            <button type="button"
                    class="inline-flex items-center px-3 py-1.5 border border-gray-300 rounded-md text-xs font-medium text-gray-700 bg-white hover:bg-gray-50"
                    onclick="goToDealFromReview('${review.dealId ?? ""}')">
              거래로 이동
            </button>
          </div>
        `;

        return `
      <div class="rounded-lg border border-gray-200 bg-white p-6">
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <!-- 별점 -->
            <div class="flex items-center gap-2">
              <div class="flex">
                ${renderStars(review.rating)}
              </div>
              <span class="text-sm font-semibold text-gray-900">${Number(review.rating).toFixed(1)}</span>
            </div>

            ${role === "SELLER" ? sellerHeader : ""}

            ${dealTitleBlock}

            <!-- 리뷰 내용 -->
            <p class="mt-3 text-sm text-gray-700">
              ${contentText}
            </p>
          </div>

          ${actionButtons}
        </div>
      </div>
    `;
    }

    // ---- global actions ----
    window.goToDealFromReview = function (dealId) {
        if (!dealId) {
            alert("이 리뷰에 연결된 거래 정보(dealId)가 없습니다.");
            return;
        }
        window.location.href = `${dealBasePath}/${dealId}`;
    };

    // 필요하면 실제 구현 연결
    window.editReview = function (reviewId) {
        console.log("Edit review:", reviewId);
    };

    window.deleteReview = function (reviewId) {
        if (confirm("리뷰를 삭제하시겠습니까?")) {
            console.log("Delete review:", reviewId);
        }
    };

    // ---- helpers ----
    function normalizeReview(r) {
        return {
            reviewId: r.reviewId ?? r.id ?? null,
            dealId: r.dealId ?? null,
            rating: toNumber(r.rating, 0),
            content: r.content ?? "",
            createdAt: r.createdAt ?? null,

            // title 키가 프로젝트 내에서 바뀔 수 있어서 전부 흡수
            title: r.auctionTitle ?? r.dealTitle ?? r.title ?? "",

            // seller 페이지에서만 필요할 수 있음
            buyerNickname: r.buyerNickname ?? "",
            buyerProfileImageUrl: r.buyerProfileImageUrl ?? "",
        };
    }

    function sortReviews(list, sort) {
        if (sort === "rating") {
            // 별점 높은 순, 같으면 최신순
            return list.sort((a, b) => {
                const diff = (b.rating ?? 0) - (a.rating ?? 0);
                if (diff !== 0) return diff;
                return toTime(b.createdAt) - toTime(a.createdAt);
            });
        }
        // latest
        return list.sort((a, b) => toTime(b.createdAt) - toTime(a.createdAt));
    }

    function updateSellerStatsIfExists(reviews) {
        const totalEl = document.getElementById("total-reviews");
        const avgEl = document.getElementById("avg-rating");
        const fiveEl = document.getElementById("five-star");
        const monthEl = document.getElementById("month-reviews");

        // seller 페이지가 아니거나 카드가 없으면 스킵
        if (!totalEl && !avgEl && !fiveEl && !monthEl) return;

        const total = reviews.length;
        const sum = reviews.reduce((acc, r) => acc + (r.rating ?? 0), 0);
        const avg = total ? sum / total : 0;

        const fiveStar = reviews.filter((r) => Math.floor(r.rating) === 5 || r.rating === 5).length;

        const now = new Date();
        const monthCount = reviews.filter((r) => {
            const d = new Date(r.createdAt);
            if (Number.isNaN(d.getTime())) return false;
            return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
        }).length;

        if (totalEl) totalEl.textContent = String(total);
        if (avgEl) avgEl.textContent = avg.toFixed(1);
        if (fiveEl) fiveEl.textContent = String(fiveStar);
        if (monthEl) monthEl.textContent = String(monthCount);
    }

    function applySortButtonStyle(allButtons, activeBtn) {
        allButtons.forEach((b) => {
            b.classList.remove("bg-gray-900", "text-white");
            b.classList.add("bg-white", "text-gray-700", "border", "border-gray-300");
        });

        activeBtn.classList.remove("bg-white", "text-gray-700", "border", "border-gray-300");
        activeBtn.classList.add("bg-gray-900", "text-white");
    }

    function renderStars(rating) {
        const fullStars = Math.floor(toNumber(rating, 0));
        let html = "";
        for (let i = 0; i < 5; i++) {
            if (i < fullStars) {
                html +=
                    '<svg class="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>';
            } else {
                html +=
                    '<svg class="h-5 w-5 text-gray-300" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>';
            }
        }
        return html;
    }

    function escapeHtml(text) {
        if (text == null) return "";
        const div = document.createElement("div");
        div.textContent = String(text);
        return div.innerHTML;
    }

    function formatDate(dateString) {
        return dateString ?? "-";
    }

    function toNumber(v, def) {
        const n = Number(v);
        return Number.isNaN(n) ? def : n;
    }

    function toTime(iso) {
        if (!iso) return 0;
        const d = new Date(iso);
        const t = d.getTime();
        return Number.isNaN(t) ? 0 : t;
    }

    function showLoading() {
        document.getElementById("loading")?.classList.remove("hidden");
        document.getElementById("reviews-list")?.classList.add("hidden");
        document.getElementById("empty-state")?.classList.add("hidden");
    }

    function hideLoading() {
        document.getElementById("loading")?.classList.add("hidden");
        document.getElementById("reviews-list")?.classList.remove("hidden");
    }
})();
