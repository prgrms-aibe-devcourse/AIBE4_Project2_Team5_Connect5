(function initSellerCredibilityModal() {
    const root = document.querySelector("#auction-detail-page");
    if (!root) return;

    const auctionId = (root.dataset?.auctionId || "").trim();
    if (!auctionId) {
        console.warn("[seller-credibility] auctionId not found");
        return;
    }

    const openBtn = root.querySelector("#open-seller-credibility");
    if (!openBtn || openBtn.dataset.bound === "1") return;
    openBtn.dataset.bound = "1";

    const modal = document.getElementById("seller-credibility-modal");
    if (!modal) return;

    const closeBtn = document.getElementById("seller-credibility-close");
    const closeX = document.getElementById("seller-credibility-close-x");

    // value fields
    const $sellerAccount = document.getElementById("sc-seller-account");
    const $nickname = document.getElementById("sc-nickname");
    const $completedDealCount = document.getElementById("sc-completed-deal-count");
    const $reportCount = document.getElementById("sc-report-count");

    const state = { isOpen: false };

    function safeText(v, fallback = "-") {
        return v === null || v === undefined || String(v).trim() === ""
            ? fallback
            : String(v);
    }

    function safeNumber(v) {
        const n = Number(v);
        return Number.isFinite(n) ? n : 0;
    }

    function openModal() {
        if (state.isOpen) return;
        state.isOpen = true;
        modal.classList.remove("hidden");
        modal.classList.add("flex");
        window.addEventListener("keydown", onKeydown);
    }

    function closeModal() {
        if (!state.isOpen) return;
        state.isOpen = false;
        modal.classList.add("hidden");
        modal.classList.remove("flex");
        window.removeEventListener("keydown", onKeydown);
    }

    function onKeydown(e) {
        if (e.key === "Escape") closeModal();
    }

    async function fetchSellerCredibility() {
        const res = await fetch(
            `/api/auctions/${encodeURIComponent(auctionId)}/seller-credibility`,
            { headers: { Accept: "application/json" }, credentials: "same-origin" }
        );

        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        return res.json();
    }

    function fillFields(data) {
        if ($sellerAccount) $sellerAccount.textContent = safeText(data?.sellerAccount);
        if ($nickname) $nickname.textContent = safeText(data?.nickname);
        if ($completedDealCount)
            $completedDealCount.textContent = safeNumber(data?.completedDealCount);
        if ($reportCount)
            $reportCount.textContent = safeNumber(data?.reportCount);
    }

    async function openAndLoad() {
        openModal();
        try {
            const data = await fetchSellerCredibility();
            fillFields(data);
        } catch (e) {
            console.warn("[seller-credibility] fetch failed", e);
            fillFields({});
            alert("판매자 정보를 불러오지 못했습니다.");
        }
    }

    // bindings
    openBtn.addEventListener("click", (e) => {
        e.preventDefault();
        openAndLoad();
    });

    closeBtn?.addEventListener("click", closeModal);
    closeX?.addEventListener("click", closeModal);
})();
