(() => {
    console.log("[buyer-review_create] init");

    const pathMatch = window.location.pathname.match(/\/buyer\/deals\/(\d+)\/review/);
    const dealId = pathMatch ? pathMatch[1] : null;

    if (!dealId) {
        console.warn("[buyer-review_create] dealId not found in path, redirecting...");
        window.location.href = "/buyer/deals";
        return;
    }

    const ratingInput = document.getElementById("rating-input");
    const ratingError = document.getElementById("rating-error");
    const starButtons = Array.from(document.querySelectorAll(".rating-star-btn"));

    const contentInput = document.getElementById("content-input");
    const contentError = document.getElementById("content-error");
    const contentCount = document.getElementById("content-count");

    const cancelBtn = document.getElementById("cancel-btn");
    const submitBtn = document.getElementById("submit-btn");

    if (!ratingInput || !contentInput || !cancelBtn || !submitBtn) {
        console.error("[buyer-review_create] required elements not found");
        return;
    }

    // 초기 별점 UI 설정
    function updateStarUI(rating) {
        starButtons.forEach((btn) => {
            const value = Number(btn.dataset.value);
            if (value <= rating) {
                btn.classList.add("text-yellow-400");
                btn.classList.remove("text-gray-300");
            } else {
                btn.classList.remove("text-yellow-400");
                btn.classList.add("text-gray-300");
            }
        });
    }

    // 기본값 5점
    ratingInput.value = "5";
    updateStarUI(5);

    // 별점 클릭 이벤트
    starButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            const value = Number(btn.dataset.value);
            ratingInput.value = String(value);
            updateStarUI(value);
            ratingError.classList.add("hidden");
            ratingError.textContent = "";
        });
    });

    // 내용 글자수 카운트
    contentInput.addEventListener("input", () => {
        const length = contentInput.value.length;
        contentCount.textContent = `${length} / 1000`;

        if (length > 0) {
            contentError.classList.add("hidden");
            contentError.textContent = "";
        }
    });

    // 취소 버튼: 다시 거래 목록으로
    cancelBtn.addEventListener("click", () => {
        window.location.href = "/buyer/deals";
    });

    // 단순 fetch 래퍼
    async function postJson(url, body) {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(`HTTP ${response.status}: ${text}`);
        }

        return response.json().catch(() => null);
    }

    // 유효성 검사
    function validateForm() {
        let valid = true;
        const rating = Number(ratingInput.value);
        const content = contentInput.value.trim();

        if (!rating || rating < 1 || rating > 5) {
            ratingError.classList.remove("hidden");
            ratingError.textContent = "평점은 1점부터 5점 사이여야 합니다.";
            valid = false;
        }

        if (content.length === 0) {
            contentError.classList.remove("hidden");
            contentError.textContent = "후기 내용을 입력해주세요.";
            valid = false;
        } else if (content.length > 1000) {
            contentError.classList.remove("hidden");
            contentError.textContent = "후기 내용은 최대 1000자까지 입력할 수 있습니다.";
            valid = false;
        }

        return valid;
    }

    // 등록 버튼 클릭
    submitBtn.addEventListener("click", async () => {
        if (!validateForm()) {
            return;
        }

        const rating = Number(ratingInput.value);
        const content = contentInput.value.trim();

        submitBtn.disabled = true;

        try {
            console.log("[buyer-review_create] POST /api/reviews/deals/" + dealId);
            await postJson(`/api/reviews/deals/${dealId}`, {
                rating,
                content
            });

            alert("후기가 등록되었습니다.");
            // 완료 후: 내가 작성한 후기 목록으로 이동 (필요에 따라 경로 수정 가능)
            window.location.href = "/mypage/buyer-reviews";
        } catch (e) {
            console.error("[buyer-review_create] failed to submit review", e);
            alert("후기 등록에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } finally {
            submitBtn.disabled = false;
        }
    });
})();
