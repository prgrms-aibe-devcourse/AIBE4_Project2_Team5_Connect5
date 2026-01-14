// src/main/resources/static/js/seller/seller-deals.js

document.addEventListener("DOMContentLoaded", () => {
    console.info("[seller-deals] init");

    const tabButtons = document.querySelectorAll(".seller-deal-tab-btn");
    const tabContents = {
        all: document.getElementById("tab-all"),
        pending: document.getElementById("tab-pending"),
        ongoing: document.getElementById("tab-ongoing"),
        completed: document.getElementById("tab-completed"),
        cancelled: document.getElementById("tab-cancelled"),
    };

    const countSpans = {
        all: document.getElementById("all-count"),
        pending: document.getElementById("pending-count"),
        ongoing: document.getElementById("ongoing-count"),
        completed: document.getElementById("completed-count"),
        cancelled: document.getElementById("cancelled-count"),
    };

    let allDeals = [];

    setupTabs(tabButtons, tabContents);
    fetchSellerDeals()
        .then((deals) => {
            allDeals = deals;
            renderAllTabs(deals, tabContents, countSpans);
        })
        .catch((err) => {
            console.error("[seller-deals] fetch error", err);
            showErrorEmptyState(tabContents.all, "거래를 불러오는 중 오류가 발생했습니다.");
        });
});

// ===================== API =====================

async function fetchSellerDeals() {
    console.info("[seller-deals] fetching /api/seller/deals");

    const res = await fetch("/api/seller/deals");
    if (!res.ok) {
        throw new Error(`failed to fetch seller deals: ${res.status}`);
    }

    const body = await res.json();
    console.info("[seller-deals] response", body);

    const deals = body.deals ?? body.data?.deals ?? [];
    return deals.map(normalizeDeal);
}

function normalizeDeal(deal) {
    // 백엔드 DTO 구조가 약간 달라도 대응 가능하도록 널 가드
    return {
        dealId: deal.dealId,
        auctionId: deal.auctionId ?? deal.auction_id ?? null,
        auctionTitle: deal.auctionTitle ?? deal.title ?? "제목 없음",
        thumbnailUrl: deal.thumbnailUrl ?? deal.auctionThumbnailUrl ?? deal.thumbnail ?? null,
        status: deal.status,
        finalPrice: deal.finalPrice ?? deal.price ?? null,
        createdAt: deal.createdAt ?? deal.created_at ?? null,
        hasReview: deal.hasReview ?? false,
    };
}

// ===================== 탭 / 렌더링 =====================

function setupTabs(tabButtons, tabContents) {
    tabButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            const tab = btn.dataset.tab;
            if (!tab) return;

            tabButtons.forEach((b) => {
                b.classList.remove("border-gray-900", "font-semibold", "text-gray-900");
                b.classList.add("border-transparent", "text-gray-500");
            });

            btn.classList.remove("border-transparent", "text-gray-500");
            btn.classList.add("border-gray-900", "font-semibold", "text-gray-900");

            Object.entries(tabContents).forEach(([key, el]) => {
                if (!el) return;
                if (key === tab) {
                    el.classList.remove("hidden");
                } else {
                    el.classList.add("hidden");
                }
            });
        });
    });

    // 초기: 전체 탭
    const defaultBtn = document.querySelector('.seller-deal-tab-btn[data-tab="all"]');
    if (defaultBtn) {
        defaultBtn.click();
    }
}

function renderAllTabs(deals, tabContents, countSpans) {
    const grouped = groupDealsByStatus(deals);

    // count 반영
    if (countSpans.all) countSpans.all.textContent = grouped.all.length;
    if (countSpans.pending) countSpans.pending.textContent = grouped.pending.length;
    if (countSpans.ongoing) countSpans.ongoing.textContent = grouped.ongoing.length;
    if (countSpans.completed) countSpans.completed.textContent = grouped.completed.length;
    if (countSpans.cancelled) countSpans.cancelled.textContent = grouped.cancelled.length;

    // 각 탭 렌더링
    renderDealList(tabContents.all, grouped.all, "전체 거래가 없습니다");
    renderDealList(tabContents.pending, grouped.pending, "거래 대기 중인 거래가 없습니다");
    renderDealList(tabContents.ongoing, grouped.ongoing, "진행 중인 거래가 없습니다");
    renderDealList(tabContents.completed, grouped.completed, "완료된 거래가 없습니다");
    renderDealList(tabContents.cancelled, grouped.cancelled, "취소/만료된 거래가 없습니다");
}

function groupDealsByStatus(deals) {
    const result = {
        all: [],
        pending: [],
        ongoing: [],
        completed: [],
        cancelled: [],
    };

    deals.forEach((deal) => {
        result.all.push(deal);

        switch (deal.status) {
            case "PENDING_CONFIRMATION":
                result.pending.push(deal);
                break;
            case "CONFIRMED":
                result.ongoing.push(deal);
                break;
            case "COMPLETED":
                result.completed.push(deal);
                break;
            case "TERMINATED":
            case "EXPIRED":
                result.cancelled.push(deal);
                break;
            default:
                // 알 수 없는 상태는 일단 전체에만 포함
                break;
        }
    });

    return result;
}

function renderDealList(container, deals, emptyMessage) {
    if (!container) return;

    container.innerHTML = "";

    if (!deals.length) {
        showEmptyState(container, emptyMessage);
        return;
    }

    const listWrapper = document.createElement("div");
    listWrapper.className = "space-y-4";

    deals.forEach((deal) => {
        const card = createDealCard(deal);
        listWrapper.appendChild(card);
    });

    container.appendChild(listWrapper);
}

function showEmptyState(container, message) {
    container.innerHTML = `
    <div class="text-center py-12 bg-gray-50 rounded-lg">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
      </svg>
      <h3 class="mt-4 text-sm font-semibold text-gray-900">${message}</h3>
      <p class="mt-2 text-sm text-gray-600">거래가 생성되면 이곳에 표시됩니다.</p>
    </div>
  `;
}

function showErrorEmptyState(container, message) {
    if (!container) return;
    container.innerHTML = `
    <div class="text-center py-12 bg-red-50 rounded-lg">
      <svg class="mx-auto h-12 w-12 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M5.22 5.22l13.56 13.56"/>
      </svg>
      <h3 class="mt-4 text-sm font-semibold text-red-900">오류</h3>
      <p class="mt-2 text-sm text-red-700">${message}</p>
    </div>
  `;
}

// ===================== 카드 UI =====================

function createDealCard(deal) {
    const card = document.createElement("div");
    card.className =
        "flex gap-4 p-4 bg-white rounded-lg shadow-sm border border-gray-100 hover:shadow-md transition-shadow";

    const thumbnail = document.createElement("img");
    thumbnail.className = "w-24 h-24 rounded-md object-cover bg-gray-100 flex-shrink-0";

    if (deal.thumbnailUrl) {
        thumbnail.src = deal.thumbnailUrl;
        thumbnail.alt = deal.auctionTitle ?? "거래 썸네일";
    } else {
        thumbnail.src =
            "https://via.placeholder.com/96x96.png?text=No+Image"; // 임시 플레이스홀더
        thumbnail.alt = "이미지 없음";
    }

    const body = document.createElement("div");
    body.className = "flex-1 flex flex-col justify-between";

    const titleRow = document.createElement("div");
    titleRow.className = "flex items-start justify-between gap-2";

    const title = document.createElement("h3");
    title.className = "text-sm font-semibold text-gray-900 line-clamp-2";
    title.textContent = deal.auctionTitle ?? "제목 없음";

    const statusBadge = document.createElement("span");
    statusBadge.className =
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium";
    const { badgeClass, badgeLabel } = getStatusBadgeInfo(deal.status);
    statusBadge.className += " " + badgeClass;
    statusBadge.textContent = badgeLabel;

    titleRow.appendChild(title);
    titleRow.appendChild(statusBadge);

    const metaRow = document.createElement("div");
    metaRow.className = "mt-2 flex items-center gap-4 text-xs text-gray-500";

    if (deal.finalPrice != null) {
        const price = document.createElement("span");
        price.textContent = `거래 금액: ${deal.finalPrice.toLocaleString()}원`;
        metaRow.appendChild(price);
    }

    if (deal.createdAt) {
        const created = document.createElement("span");
        created.textContent = `생성일: ${formatDateTime(deal.createdAt)}`;
        metaRow.appendChild(created);
    }

    const actionsRow = document.createElement("div");
    actionsRow.className = "mt-4 flex items-center justify-end gap-2";

    const detailBtn = document.createElement("button");
    detailBtn.type = "button";
    detailBtn.className =
        "inline-flex items-center px-3 py-1.5 border border-gray-300 rounded-md text-xs font-medium text-gray-700 bg-white hover:bg-gray-50";
    detailBtn.textContent = "상세 보기";
    detailBtn.addEventListener("click", () => {
        window.location.href = `/seller/deals/${deal.dealId}`;
    });
    actionsRow.appendChild(detailBtn);

    // 완료 상태인 경우 리뷰 보기 버튼 노출 (이미 리뷰가 있다면 '리뷰 보기', 없다면 '리뷰 확인')
    if (deal.status === "COMPLETED") {
        const reviewBtn = document.createElement("button");
        reviewBtn.type = "button";
        reviewBtn.className =
            "inline-flex items-center px-3 py-1.5 border border-indigo-600 rounded-md text-xs font-medium text-indigo-600 bg-white hover:bg-indigo-50";

        reviewBtn.textContent = deal.hasReview ? "리뷰 보기" : "리뷰 확인";

        reviewBtn.addEventListener("click", () => {
            // 판매자는 "리뷰를 받는 쪽"이므로, 나중에 리뷰 상세/목록 페이지로 연결
            window.location.href = `/seller/reviews?dealId=${deal.dealId}`;
        });

        actionsRow.appendChild(reviewBtn);
    }

    body.appendChild(titleRow);
    body.appendChild(metaRow);
    body.appendChild(actionsRow);

    card.appendChild(thumbnail);
    card.appendChild(body);

    return card;
}

function getStatusBadgeInfo(status) {
    switch (status) {
        case "PENDING_CONFIRMATION":
            return {
                badgeClass: "bg-yellow-50 text-yellow-800 border border-yellow-100",
                badgeLabel: "거래 대기",
            };
        case "CONFIRMED":
            return {
                badgeClass: "bg-blue-50 text-blue-800 border border-blue-100",
                badgeLabel: "진행 중",
            };
        case "COMPLETED":
            return {
                badgeClass: "bg-emerald-50 text-emerald-800 border border-emerald-100",
                badgeLabel: "완료",
            };
        case "TERMINATED":
        case "EXPIRED":
            return {
                badgeClass: "bg-gray-50 text-gray-500 border border-gray-100",
                badgeLabel: "취소/만료",
            };
        default:
            return {
                badgeClass: "bg-gray-50 text-gray-500 border border-gray-100",
                badgeLabel: status ?? "알 수 없음",
            };
    }
}

function formatDateTime(isoString) {
    if (!isoString) return "";
    try {
        const d = new Date(isoString);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        const hh = String(d.getHours()).padStart(2, "0");
        const mm = String(d.getMinutes()).padStart(2, "0");
        return `${y}-${m}-${day} ${hh}:${mm}`;
    } catch (e) {
        console.warn("[seller-deals] 날짜 파싱 실패:", isoString);
        return isoString;
    }
}
