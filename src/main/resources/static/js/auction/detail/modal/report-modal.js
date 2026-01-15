/**
 * 경로: src/main/resources/static/js/auction/detail/modal/report-modal.js
 * 역할: 신고 모달 제어 및 API 호출
 */

(function initReportModal() {
    const root = document.querySelector("#auction-detail-page");
    if (!root) return;

    const auctionId = root.dataset.auctionId;
    const meUserId = root.dataset.meUserId;
    const sellerId = root.dataset.sellerId; // 판매자 ID 추가

    const openBtn = document.querySelector("#report-button");
    const modal = document.querySelector("#report-modal");
    const form = document.querySelector("#report-form");
    const cancelBtn = document.querySelector("#report-cancel");
    const reasonSelect = document.querySelector("#report-reason");
    const descTextarea = document.querySelector("#report-description");
    const reportTargetRadios = document.querySelectorAll("input[name='reportTarget']");

    // Toast
    const toast = document.querySelector("#toast");
    const toastTitle = document.querySelector("#toastTitle");
    const toastMsg = document.querySelector("#toastMsg");

    if (!openBtn || !modal || !form) return;

    // --- State ---
    let isOpen = false;
    let isSubmitting = false;

    // --- Event Listeners ---
    openBtn.addEventListener("click", () => {
        if (!meUserId) {
            showToast("안내", "로그인 후 신고 기능을 이용할 수 있습니다.");
            return;
        }
        open();
    });

    cancelBtn?.addEventListener("click", close);

    modal.addEventListener("click", (e) => {
        if (e.target === modal) close();
    });

    document.addEventListener("keydown", (e) => {
        if (isOpen && e.key === "Escape") close();
    });

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        if (isSubmitting) return;

        const reason = reasonSelect.value;
        const description = descTextarea.value.trim();
        const targetType = getSelectedTargetType();

        if (!description || description.length < 10) {
            alert("신고 내용을 10자 이상 상세히 적어주세요.");
            descTextarea.focus();
            return;
        }

        if (!sellerId) {
            showToast("오류", "판매자 정보를 찾을 수 없어 신고할 수 없습니다.");
            return;
        }

        if (!confirm("정말 신고하시겠습니까? 허위 신고 시 제재를 받을 수 있습니다.")) {
            return;
        }

        try {
            isSubmitting = true;
            await submitReport({
                reportedUserId: sellerId, // 피신고자 ID (판매자)
                auctionId: auctionId,     // 경매 ID
                type: targetType,         // 신고 대상 타입 (AUCTION or USER_PROFILE)
                reason: reason,           // 신고 사유
                description: description  // 상세 설명
            });
            showToast("신고 완료", "신고가 접수되었습니다.");
            close();
        } catch (err) {
            console.error(err);
            showToast("오류", err.message || "신고 접수에 실패했습니다.");
        } finally {
            isSubmitting = false;
        }
    });

    // --- Functions ---
    function open() {
        isOpen = true;
        modal.classList.remove("hidden");
        modal.classList.add("flex");
        // 초기화
        form.reset();
    }

    function close() {
        isOpen = false;
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }

    function getSelectedTargetType() {
        for (const radio of reportTargetRadios) {
            if (radio.checked) return radio.value;
        }
        return "AUCTION";
    }

    async function submitReport(payload) {
        const url = "/api/reports";
        
        const res = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            throw new Error(errorData.message || `신고 실패 (${res.status})`);
        }
        
        return await res.json();
    }

    function showToast(title, message) {
        if (!toast) {
            alert(message);
            return;
        }
        if (toastTitle) toastTitle.textContent = title;
        if (toastMsg) toastMsg.textContent = message;
        
        toast.classList.remove("hidden");
        setTimeout(() => {
            toast.classList.add("hidden");
        }, 3000);
    }

})();
