// /static/js/favorite.js
document.addEventListener("DOMContentLoaded", () => {
    const FAVORITE_BTN_SELECTOR = ".js-fav-toggle";
    const FAVORITE_GRID_ID = "favorites-grid";
    const EMPTY_STATE_ID = "empty-state";
    const FAVORITE_COUNT_ID = "favorite-count";
    const FILTER_BTN_SELECTOR = ".fav-filter-btn";
    const SORT_SELECT_ID = "sort-select";

    // favorites page state (뷰 URL은 그대로 유지, 상태는 sessionStorage로만 관리)
    const STATE_KEY = "favorites_view_state_v1";
    const defaultState = { filter: "ALL", sort: "recent", page: 0, size: 20 };

    function loadState() {
        try {
            const raw = sessionStorage.getItem(STATE_KEY);
            return raw ? { ...defaultState, ...JSON.parse(raw) } : { ...defaultState };
        } catch {
            return { ...defaultState };
        }
    }

    function saveState(state) {
        sessionStorage.setItem(STATE_KEY, JSON.stringify(state));
    }

    // -----------------------------
    // CSRF (meta 태그가 있을 때만)
    // -----------------------------
    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        if (!token || !header) return {};
        return { [header]: token };
    }

    async function apiFetch(url, options = {}) {
        const headers = {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...getCsrfHeaders(),
            ...(options.headers || {}),
        };

        const res = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers,
        });

        if (res.status === 401) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return null;
        }
        if (res.status === 403) {
            alert("요청을 수행할 수 없습니다. (본인 경매는 찜할 수 없습니다.)");
            return null;
        }

        return res;
    }

    // -----------------------------
    // UI helpers (하트)
    // -----------------------------
    function setHeartUI(btn, favorited) {
        btn.dataset.favorited = String(!!favorited);

        const icon = btn.querySelector(".js-fav-icon");
        if (!icon) return;

        if (favorited) {
            icon.setAttribute("fill", "currentColor");
            icon.classList.remove("text-slate-700");
            icon.classList.add("text-rose-500");
        } else {
            icon.setAttribute("fill", "none");
            icon.classList.remove("text-rose-500");
            icon.classList.add("text-slate-700");
        }
    }

    function setBtnLoading(btn, loading) {
        btn.disabled = loading;
        btn.classList.toggle("opacity-60", loading);
        btn.classList.toggle("pointer-events-none", loading);
    }

    // -----------------------------
    // 1) 경매 목록: 배치로 찜 상태 세팅
    // -----------------------------
    function normalizeStatusResponse(data) {
        if (data?.favoritedByAuctionId && typeof data.favoritedByAuctionId === "object") {
            return data.favoritedByAuctionId;
        }

        // 1) { "uuid": true, ... } 형태
        if (data && !Array.isArray(data) && typeof data === "object" && !("auctionId" in data)) {
            return data;
        }

        // 2) [{ auctionId, favorited }, ...] 형태
        if (Array.isArray(data)) {
            const map = {};
            data.forEach((row) => {
                if (row && row.auctionId) map[row.auctionId] = !!row.favorited;
            });
            return map;
        }

        // 3) { auctionId, favorited, ... } 단일 객체 형태(지금 스샷처럼 보이는 케이스)
        if (data && typeof data === "object" && data.auctionId) {
            return { [data.auctionId]: !!data.favorited };
        }

        // 4) 감싸져 오는 케이스: { data: ... } / { result: ... }
        if (data?.data) return normalizeStatusResponse(data.data);
        if (data?.result) return normalizeStatusResponse(data.result);

        return {};
    }

    async function initFavoriteStatusOnAuctionList() {
        // favorites page에서는 굳이 배치 상태 호출할 필요가 없음
        const favoritesGrid = document.getElementById(FAVORITE_GRID_ID);
        if (favoritesGrid) return;

        const buttons = Array.from(document.querySelectorAll(FAVORITE_BTN_SELECTOR));
        if (buttons.length === 0) return;

        const userRoleElement = document.querySelector('[data-user-role]');
        const userRole = userRoleElement?.dataset?.userRole;

        // 로그인하지 않았거나 ANONYMOUS면 API 호출하지 않음
        if (!userRole || userRole === 'ANONYMOUS') {
            buttons.forEach((btn) => setHeartUI(btn, false));
            return;
        }

        const auctionIds = buttons.map((b) => b.dataset.auctionId).filter(Boolean);
        const uniqueIds = Array.from(new Set(auctionIds));
        if (uniqueIds.length === 0) return;

        const res = await apiFetch("/api/favorites/status", {
            method: "POST",
            body: JSON.stringify({ auctionIds: uniqueIds }),
        });

        if (!res || !res.ok) {
            buttons.forEach((btn) => setHeartUI(btn, false));
            return;
        }

        const raw = await res.json();
        const statusMap = normalizeStatusResponse(raw);

        buttons.forEach((btn) => {
            const id = btn.dataset.auctionId;
            if (!id) return;
            setHeartUI(btn, !!statusMap[id]);
        });
    }

    // -----------------------------
    // 2) 찜 토글(공용)
    // -----------------------------
    async function toggleFavorite(btn) {
        const auctionId = btn.dataset.auctionId;
        if (!auctionId) return;

        // ✅ 로그인 체크
        const userRole = document.body.dataset.userRole;

        if (!userRole || userRole === 'ANONYMOUS') {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        setBtnLoading(btn, true);

        const res = await apiFetch(`/api/favorites/${auctionId}`, { method: "POST" });

        setBtnLoading(btn, false);
        if (!res) return;

        if (res.status === 409) {
            alert("이미 찜한 경매입니다.");
            return;
        }

        if (!res.ok) {
            alert("요청 처리에 실패했습니다.");
            return;
        }

        const data = await res.json();
        setHeartUI(btn, data.favorited);

        const favoritesGrid = document.getElementById(FAVORITE_GRID_ID);
        if (favoritesGrid && !data.favorited) {
            const card = btn.closest("[data-auction-card]") || btn.closest("article") || btn.closest("div");
            if (card) card.remove();
            updateEmptyState();
            updateFavoriteCount(-1);
        }
    }

    function bindFavoriteButtons() {
        document.addEventListener("click", (e) => {
            const btn = e.target.closest(FAVORITE_BTN_SELECTOR);
            if (!btn) return;
            toggleFavorite(btn);
        });
    }

    // -----------------------------
    // 3) favorites page: 목록 조회 + 렌더링
    // -----------------------------
    function isFavoritesPage() {
        return !!document.getElementById(FAVORITE_GRID_ID);
    }

    function updateEmptyState() {
        const grid = document.getElementById(FAVORITE_GRID_ID);
        const empty = document.getElementById(EMPTY_STATE_ID);
        if (!grid || !empty) return;

        const hasCards = grid.querySelector("[data-auction-card]");
        empty.classList.toggle("hidden", !!hasCards);
    }

    function updateFavoriteCount(delta) {
        const el = document.getElementById(FAVORITE_COUNT_ID);
        if (!el) return;

        const current = parseInt((el.textContent || "0").replace(/[^\d]/g, ""), 10) || 0;
        el.textContent = String(Math.max(0, current + delta));
    }

    function setActiveFilterUI(filter) {
        document.querySelectorAll(FILTER_BTN_SELECTOR).forEach((btn) => {
            const isActive = (btn.dataset.filter || "").toUpperCase() === filter.toUpperCase();

            // 공통 클래스는 유지하고, 상태 클래스만 정확히 맞춘다
            btn.classList.toggle("bg-gray-900", isActive);
            btn.classList.toggle("text-white", isActive);
            btn.classList.toggle("font-semibold", isActive);

            btn.classList.toggle("bg-white", !isActive);
            btn.classList.toggle("text-gray-700", !isActive);
            btn.classList.toggle("border", !isActive);
            btn.classList.toggle("border-gray-300", !isActive);
            btn.classList.toggle("hover:bg-gray-50", !isActive);
            btn.classList.toggle("font-medium", !isActive);
        });
    }

    function setSortUI(sort) {
        const sel = document.getElementById(SORT_SELECT_ID);
        if (!sel) return;
        sel.value = sort;
    }

    function escapeHtml(str) {
        return String(str ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function statusBadge(item) {
        const status = item?.status;
        const endReason = item?.endReason;

        switch (status) {
            case "LIVE":
                return {
                    label: "진행 중",
                    cls:
                        "inline-flex items-center rounded-full border px-3 py-1 text-xs font-black " +
                        "border-emerald-200 bg-emerald-50 text-emerald-700",
                };
            case "DRAFT":
                return {
                    label: "임시 저장",
                    cls:
                        "inline-flex items-center rounded-full border px-3 py-1 text-xs font-black " +
                        "border-slate-200 bg-slate-50 text-slate-700",
                };
            case "ENDED_SOLD":
                return {
                    label: "낙찰 완료",
                    cls:
                        "inline-flex items-center rounded-full border px-3 py-1 text-xs font-black " +
                        "border-slate-300 bg-slate-100 text-slate-900",
                };
            case "ENDED_UNSOLD":
                if (endReason === "SELLER_STOPPED") {
                    return {
                        label: "경매 취소",
                        cls:
                            "inline-flex items-center rounded-full border px-3 py-1 text-xs font-black " +
                            "border-slate-300 bg-slate-100 text-slate-700",
                    };
                }
                return {
                    label: "유찰",
                    cls:
                        "inline-flex items-center rounded-full border px-3 py-1 text-xs font-black " +
                        "border-rose-200 bg-rose-50 text-rose-700",
                };
            default:
                return {
                    label: String(status ?? "-"),
                    cls:
                        "inline-flex items-center rounded-full border px-3 py-1 text-xs font-black " +
                        "border-slate-200 bg-white text-slate-700",
                };
        }
    }

    // 카드 HTML 생성 (auction-card.html과 유사한 Tailwind 구성)
    // 카드 HTML 생성 (찜 목록용 완성본 - 하트 이미지 오버레이)
    function renderFavoriteCard(item) {
        const auctionId = item.auctionId;
        const title = escapeHtml(item.title);
        const img = escapeHtml(item.thumbnailUrl || "");
        const nick = escapeHtml(item.sellerNickname || "");
        const currentPrice = item.currentPrice ?? 0;
        const favoriteCount = item.favoriteCount ?? 0;
        const bidCount = item.bidCount ?? 0;
        const remainingTime = escapeHtml(item.remainingTime || "");
        const status = statusBadge(item);

        return `
    <article data-auction-card
             class="group relative overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm hover:shadow-md transition-shadow">

      <!-- 이미지 영역 -->
      <div class="relative aspect-[4/3] w-full overflow-hidden bg-slate-100">
        ${
            img
                ? `<img src="${img}" alt="${title}"
                   class="h-full w-full object-cover group-hover:scale-105 transition-transform" />`
                : ""
        }
        
        <div class="absolute left-3 top-3">
          <span class="${status.cls}">${escapeHtml(status.label)}</span>
        </div>

        <!-- 하트: 이미지 위에 덮이기 -->
        <button type="button"
                class="js-fav-toggle absolute right-3 bottom-3 inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/95 shadow ring-1 ring-slate-200 hover:bg-white"
                data-auction-id="${auctionId}"
                aria-label="찜">
          <svg xmlns="http://www.w3.org/2000/svg"
               fill="currentColor"
               viewBox="0 0 24 24"
               stroke-width="1.8"
               stroke="currentColor"
               class="js-fav-icon h-5 w-5 text-rose-500">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733C11.285 4.876 9.623 3.75 7.688 3.75 5.099 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" />
          </svg>
        </button>
      </div>

      <!-- 텍스트 영역만 상세 링크 -->
      <a href="/auctions/${auctionId}" class="block p-4">
        <div class="mt-1 line-clamp-2 text-base font-semibold text-slate-900">${title}</div>

        <div class="mt-3 flex items-center justify-between">
          <div class="text-lg font-bold text-slate-900">
            ${Number(currentPrice).toLocaleString("ko-KR")}원
          </div>

          <div class="flex items-center gap-1 text-xs text-slate-500">
            <svg xmlns="http://www.w3.org/2000/svg"
                 class="h-4 w-4"
                 fill="none"
                 viewBox="0 0 24 24"
                 stroke="currentColor"
                 stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>${remainingTime}</span>
          </div>
        </div>

        <div class="mt-3 text-xs text-slate-600">
          <div class="truncate">
            판매자 <span class="font-semibold text-slate-800">${nick}</span>
          </div>
          <div class="mt-1">
            입찰 <span class="font-semibold text-slate-800">${bidCount}</span>
            <span class="mx-1 text-slate-300">·</span>
            찜 <span class="font-semibold text-slate-800">${favoriteCount}</span>
          </div>
        </div>
        
        
        
      </a>

    </article>
  `;
    }

    async function loadFavorites(state) {
        const grid = document.getElementById(FAVORITE_GRID_ID);
        if (!grid) return;

        // 내부 API 호출은 query를 써도 됨(사용자 URL은 건드리지 않음)
        const params = new URLSearchParams({
            filter: state.filter,
            sort: state.sort,
            page: String(state.page),
            size: String(state.size),
        });

        const res = await apiFetch(`/api/favorites/me?${params.toString()}`, { method: "GET" });
        if (!res || !res.ok) {
            grid.innerHTML = "";
            updateEmptyState();
            return;
        }

        const data = await res.json();

        // PageResponse 형태를 방어적으로 처리
        const content = data.content || data.items || [];
        grid.innerHTML = content.map(renderFavoriteCard).join("");

        // 찜 목록은 전부 찜 상태이므로 UI 강제 세팅
        grid.querySelectorAll(FAVORITE_BTN_SELECTOR).forEach((btn) => setHeartUI(btn, true));

        updateEmptyState();

        // totalElements가 있으면 count 표시 갱신(있을 때만)
        const total = data.pageInfo?.totalElements;
        if (typeof total === "number") {
            const countEl = document.getElementById(FAVORITE_COUNT_ID);
            if (countEl) countEl.textContent = String(total);
        }
    }

    function bindFavoritesPageControls() {
        const grid = document.getElementById(FAVORITE_GRID_ID);
        if (!grid) return;

        let state = loadState();
        setActiveFilterUI(state.filter);
        setSortUI(state.sort);

        // 필터 버튼
        document.querySelectorAll(FILTER_BTN_SELECTOR).forEach((btn) => {
            btn.addEventListener("click", () => {
                state = { ...state, filter: (btn.dataset.filter || "ALL").toUpperCase(), page: 0 };
                saveState(state);
                setActiveFilterUI(state.filter);
                loadFavorites(state);
            });
        });

        // 정렬 셀렉트
        const sel = document.getElementById(SORT_SELECT_ID);
        if (sel) {
            sel.addEventListener("change", () => {
                state = { ...state, sort: sel.value || "recent", page: 0 };
                saveState(state);
                loadFavorites(state);
            });
        }

        // 최초 로드
        loadFavorites(state);
    }

    // -----------------------------
    // 실행
    // -----------------------------
    bindFavoriteButtons();

    if (isFavoritesPage()) {
        bindFavoritesPageControls();
    } else {
        initFavoriteStatusOnAuctionList();
    }
});
