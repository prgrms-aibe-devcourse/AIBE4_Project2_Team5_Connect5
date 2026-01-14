/**
 * 경로:
 * src/main/resources/static/js/auction/detail/bid_list.js
 *
 * 역할:
 * - 경매 상세 페이지에서 "입찰기록 내역" 모달 오픈/닫기
 * - /api/auctions/{auctionId}/bids?page=&size= 호출
 * - 서버가 내려준 amount(마스킹: null) 규칙 그대로 렌더링
 * - bidder 표기는 개인정보 최소화를 위해 "N번 입찰자"로 통일(서버가 bidderLabel을 주면 그대로 사용)
 */

(function initBidListModal() {
    const root = document.querySelector("#auction-detail-page");
    if (!root) return;

    const auctionId = root.dataset.auctionId;
    const meUserId = normalizeUuid(root.dataset.meUserId);

    // trigger (상세 화면에 추가할 버튼)
    const openBtn = document.querySelector("#open-bid-list");
    if (!openBtn) return;

    // modal
    const modal = document.querySelector("#bid-list-modal");
    const closeX = document.querySelector("#bid-list-close-x");
    const okBtn = document.querySelector("#bid-list-ok");
    const sizeSelect = document.querySelector("#bid-list-size");
    const tbody = document.querySelector("#bid-list-tbody");
    const prevBtn = document.querySelector("#bid-list-prev");
    const nextBtn = document.querySelector("#bid-list-next");
    const pagesWrap = document.querySelector("#bid-list-pages");

    // toast (기존 페이지 토스트 DOM 재사용)
    const toast = document.querySelector("#toast");
    const toastTitle = document.querySelector("#toastTitle");
    const toastMsg = document.querySelector("#toastMsg");

    // state
    let isOpen = false;
    let page = 0;          // 0-based
    let size = 10;
    let totalPages = 1;
    let loading = false;

    // bidder label 안정화(서버가 bidderLabel 안 주는 경우 대비)
    const bidderLabelMap = new Map(); // key: bidderId, value: "N번 입찰자"
    let nextBidderNo = 1;

    // --------------- events ---------------
    openBtn.addEventListener("click", async () => {
        // 비로그인 접근 방지(프론트 1차 방어)
        // 백엔드에서도 401/403 처리되겠지만 UX를 위해 사전 차단
        if (!meUserId) {
            showToast("안내", "로그인 후 입찰 기록을 확인할 수 있습니다.");
            return;
        }

        open();
        await loadAndRender(0);
    });

    closeX?.addEventListener("click", close);
    okBtn?.addEventListener("click", close);

    // overlay click close
    modal?.addEventListener("click", (e) => {
        if (e.target === modal) close();
    });

    // esc close
    document.addEventListener("keydown", (e) => {
        if (!isOpen) return;
        if (e.key === "Escape") close();
    });

    sizeSelect?.addEventListener("change", async () => {
        const v = Number(sizeSelect.value);
        size = Number.isFinite(v) && v > 0 ? v : 10;
        await loadAndRender(0);
    });

    prevBtn?.addEventListener("click", async () => {
        if (loading) return;
        if (page <= 0) return;
        await loadAndRender(page - 1);
    });

    nextBtn?.addEventListener("click", async () => {
        if (loading) return;
        if (page >= totalPages - 1) return;
        await loadAndRender(page + 1);
    });

    // --------------- core ---------------
    function open() {
        if (!modal) return;
        isOpen = true;
        modal.classList.remove("hidden");
        modal.classList.add("flex");
        // 초기값 세팅
        const v = Number(sizeSelect?.value ?? 10);
        size = Number.isFinite(v) && v > 0 ? v : 10;
    }

    function close() {
        if (!modal) return;
        isOpen = false;
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }

    async function loadAndRender(nextPage) {
        if (!auctionId) {
            showToast("오류", "auctionId가 없습니다.");
            return;
        }

        loading = true;
        page = Math.max(0, Number(nextPage ?? 0));

        renderLoadingRow();

        try {
            const data = await fetchBidHistory(auctionId, page, size);

            const content = extractContent(data);
            const meta = extractMeta(data, page, size, content.length);

            totalPages = meta.totalPages;
            page = meta.page;

            renderRows(content);
            renderPagination(page, totalPages);
            updateNavButtons();
        } catch (e) {
            renderErrorRow("입찰 목록을 불러오지 못했습니다.");
            const msg = String(e?.message ?? "");
            showToast("오류", msg || "입찰 목록을 불러오지 못했습니다.");
        } finally {
            loading = false;
        }
    }

    async function fetchBidHistory(auctionId, page, size) {
        const params = new URLSearchParams({
            page: String(page),
            size: String(size),
        });

        const url = `/api/auctions/${encodeURIComponent(auctionId)}/bids?${params.toString()}`;

        const res = await fetch(url, {
            method: "GET",
            headers: { Accept: "application/json" },
            credentials: "same-origin",
        });

        // 비로그인/권한 거부 대응
        if (res.status === 401) {
            throw new Error("로그인 후 이용해 주세요.");
        }
        if (res.status === 403) {
            throw new Error("접근 권한이 없습니다.");
        }

        if (!res.ok) {
            throw new Error(`GET ${url} failed: ${res.status}`);
        }

        return await res.json();
    }

    // --------------- render ---------------
    function renderLoadingRow() {
        if (!tbody) return;
        tbody.innerHTML = `
      <tr>
        <td colspan="3" class="px-4 py-8 text-center text-slate-600">
          불러오는 중...
        </td>
      </tr>
    `;
    }

    function renderErrorRow(message) {
        if (!tbody) return;
        tbody.innerHTML = `
      <tr>
        <td colspan="3" class="px-4 py-8 text-center text-rose-600 font-semibold">
          ${escapeHtml(message)}
        </td>
      </tr>
    `;
    }

    function renderEmptyRow() {
        if (!tbody) return;
        tbody.innerHTML = `
      <tr>
        <td colspan="3" class="px-4 py-8 text-center text-slate-600">
          입찰 내역이 없습니다.
        </td>
      </tr>
    `;
    }

    function renderRows(rows) {
        if (!tbody) return;

        if (!rows || rows.length === 0) {
            renderEmptyRow();
            return;
        }

        const html = rows.map((r) => {
            const bidAt = formatBidAt(r?.bidAt ?? r?.createdAt ?? r?.created_at);

            let bidderLabel = resolveBidderLabel(r);
            if (r?.isMe === true) {
                bidderLabel = `${bidderLabel}(내 입찰)`;
            }

            const amountText = formatAmount(r?.amount);

            return `
        <tr class="hover:bg-slate-50">
          <td class="px-4 py-4 text-center text-slate-800 tabular-nums">${escapeHtml(bidAt)}</td>
          <td class="px-4 py-4 text-center text-slate-800 font-semibold">${escapeHtml(bidderLabel)}</td>
          <td class="px-4 py-4 text-center text-slate-900 font-extrabold">${escapeHtml(amountText)}</td>
        </tr>
      `;
        }).join("");

        tbody.innerHTML = html;
    }

    function renderPagination(currentPage, totalPages) {
        if (!pagesWrap) return;

        // 표시할 페이지 버튼: 최대 5개(현재 기준)
        const maxButtons = 5;
        const cur = Math.max(0, currentPage);
        const total = Math.max(1, totalPages);

        let start = Math.max(0, cur - 2);
        let end = Math.min(total - 1, start + (maxButtons - 1));
        start = Math.max(0, end - (maxButtons - 1));

        const buttons = [];
        for (let p = start; p <= end; p++) {
            const isActive = p === cur;
            buttons.push(`
        <button type="button"
                data-page="${p}"
                class="${
                isActive
                    ? "h-10 min-w-[40px] rounded-lg bg-slate-900 px-3 text-sm font-extrabold text-white"
                    : "h-10 min-w-[40px] rounded-lg bg-white px-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50"
            }">
          ${p + 1}
        </button>
      `);
        }

        pagesWrap.innerHTML = buttons.join("");

        pagesWrap.querySelectorAll("button[data-page]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const p = Number(btn.dataset.page);
                if (!Number.isFinite(p)) return;
                if (loading) return;
                await loadAndRender(p);
            });
        });
    }

    function updateNavButtons() {
        if (prevBtn) prevBtn.disabled = loading || page <= 0;
        if (nextBtn) nextBtn.disabled = loading || page >= totalPages - 1;

        prevBtn?.classList.toggle("opacity-40", prevBtn?.disabled);
        nextBtn?.classList.toggle("opacity-40", nextBtn?.disabled);
    }

    // --------------- data helpers ---------------
    function extractContent(data) {
        // PageResponse 스타일 대응
        if (Array.isArray(data?.content)) return data.content;
        if (Array.isArray(data?.items)) return data.items;
        if (Array.isArray(data?.bids)) return data.bids;
        if (Array.isArray(data)) return data;
        return [];
    }

    function extractMeta(data, fallbackPage, fallbackSize, contentLength) {
        // 다양한 Page 형태 대응
        const totalPages =
            asInt(data?.totalPages) ??
            asInt(data?.page?.totalPages) ??
            asInt(data?.pageInfo?.totalPages) ??
            1;

        const page =
            asInt(data?.page) ??
            asInt(data?.number) ??
            asInt(data?.pageNumber) ??
            asInt(data?.page?.number) ??
            fallbackPage;

        const size =
            asInt(data?.size) ??
            asInt(data?.pageSize) ??
            asInt(data?.page?.size) ??
            fallbackSize;

        // totalPages가 0으로 내려오는 케이스 방어
        const safeTotalPages = Math.max(1, totalPages);

        return {
            page: Math.max(0, page),
            size: Math.max(1, size),
            totalPages: safeTotalPages,
            totalElements: asInt(data?.totalElements) ?? asInt(data?.totalCount) ?? contentLength,
        };
    }

    function resolveBidderLabel(row) {
        // 서버가 label/번호를 내려주면 최우선 사용
        const direct =
            row?.bidderLabel ??
            row?.bidderName ??
            row?.displayBidder ??
            null;

        if (direct) return String(direct);

        const bidderNo = asInt(row?.bidderNo ?? row?.bidderIndex);
        if (Number.isFinite(bidderNo) && bidderNo > 0) return `${bidderNo}번 입찰자`;

        const bidderId = row?.bidderId ?? row?.bidderUUID ?? row?.bidder_id ?? null;
        const key = bidderId ? String(bidderId) : null;

        if (!key) return "입찰자";

        if (bidderLabelMap.has(key)) return bidderLabelMap.get(key);

        const label = `${nextBidderNo++}번 입찰자`;
        bidderLabelMap.set(key, label);
        return label;
    }

    function formatAmount(amount) {
        // 서버 정책:
        // - 관리자/판매자: amount 숫자
        // - 입찰자: 본인 건만 숫자, 나머지 null
        // - 일반유저: 전부 null
        if (amount === null || amount === undefined) return "***원";

        const n = Number(amount);
        if (!Number.isFinite(n)) return "***원";

        return `${n.toLocaleString("ko-KR")}원`;
    }

    function formatBidAt(v) {
        if (!v) return "-";

        const s = String(v).trim();
        // ISO 형태면 Date.parse 가능
        const ms = Date.parse(s);
        if (Number.isNaN(ms)) {
            // 혹시 이미 "YYYY-MM-DD HH:mm:ss.SS"로 내려오면 그대로 사용
            return s;
        }

        const d = new Date(ms);

        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");
        const hh = String(d.getHours()).padStart(2, "0");
        const mi = String(d.getMinutes()).padStart(2, "0");
        const ss = String(d.getSeconds()).padStart(2, "0");

        // 2자리(0.01초 단위)로 맞춤: screenshot 형태(....53.10)
        const centi = String(Math.floor(d.getMilliseconds() / 10)).padStart(2, "0");

        return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}.${centi}`;
    }

    function showToast(title, message) {
        if (!toast) {
            alert(message);
            return;
        }

        if (toastTitle) toastTitle.textContent = title || "알림";
        if (toastMsg) toastMsg.textContent = message || "";
        toast.classList.remove("hidden");

        setTimeout(() => {
            toast.classList.add("hidden");
        }, 2500);
    }

    function normalizeUuid(v) {
        if (v === null || v === undefined) return null;
        const s = String(v).trim();
        if (!s) return null;
        return s.toLowerCase();
    }

    function asInt(v) {
        if (v === null || v === undefined) return null;
        const n = Number(v);
        return Number.isFinite(n) ? Math.trunc(n) : null;
    }

    function escapeHtml(s) {
        return String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();
