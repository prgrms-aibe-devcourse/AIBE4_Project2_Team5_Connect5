// 구매 리뷰 작성 페이지
document.addEventListener('DOMContentLoaded', () => {
    const log = (...args) => console.log('[buyer-review-create]', ...args);

    const dealIdInput = document.getElementById('deal-id');
    const ratingHidden = document.getElementById('rating-input');
    const contentInput = document.getElementById('content');

    const ratingError = document.getElementById('rating-error');
    const contentError = document.getElementById('content-error');
    const formError = document.getElementById('form-error');
    const countSpan = document.getElementById('content-count');

    const submitBtn = document.getElementById('submit-review-btn');
    const starButtons = document.querySelectorAll('.review-star');

    if (!dealIdInput || !ratingHidden || !contentInput || !submitBtn) {
        log('필수 요소를 찾지 못했습니다.');
        return;
    }

    let currentRating = Number(ratingHidden.value || 0);

    // --- 별 렌더링 ---
    function renderStars(rating) {
        starButtons.forEach((btn) => {
            const value = Number(btn.dataset.value);
            const svg = btn.querySelector('svg');
            if (!svg) return;

            if (value <= rating) {
                svg.classList.remove('text-gray-300');
                svg.classList.add('text-yellow-400');
            } else {
                svg.classList.remove('text-yellow-400');
                svg.classList.add('text-gray-300');
            }
        });
    }

    // 별 클릭 이벤트
    starButtons.forEach((btn) => {
        btn.addEventListener('click', () => {
            const value = Number(btn.dataset.value);
            currentRating = value;
            ratingHidden.value = String(value);
            renderStars(currentRating);
            ratingError.classList.add('hidden');
        });
    });

    renderStars(currentRating);

    // --- 내용 글자수 카운터 ---
    const MAX_LENGTH = 1000;
    const updateCount = () => {
        const len = contentInput.value.length;
        if (len > MAX_LENGTH) {
            contentInput.value = contentInput.value.slice(0, MAX_LENGTH);
        }
        if (countSpan) {
            countSpan.textContent = `${contentInput.value.length} / ${MAX_LENGTH}`;
        }
    };
    contentInput.addEventListener('input', () => {
        updateCount();
        if (contentInput.value.trim()) {
            contentError.classList.add('hidden');
        }
    });
    updateCount();

    // --- 유효성 검사 ---
    function validate() {
        let ok = true;
        const rating = Number(ratingHidden.value || 0);
        const content = contentInput.value.trim();

        if (!rating || rating < 1 || rating > 5) {
            ratingError.classList.remove('hidden');
            ok = false;
        } else {
            ratingError.classList.add('hidden');
        }

        if (!content) {
            contentError.classList.remove('hidden');
            ok = false;
        } else {
            contentError.classList.add('hidden');
        }

        if (!ok) {
            formError.classList.add('hidden');
        }

        return ok;
    }

    // --- 제출 ---
    async function submitReview() {
        if (!validate()) return;

        const dealId = dealIdInput.value;
        const rating = Number(ratingHidden.value);
        const content = contentInput.value.trim();

        const url = `/api/buyer/deals/${dealId}/review`;
        const body = { rating, content };

        log('POST', url, body);

        submitBtn.disabled = true;
        formError.classList.add('hidden');

        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });

            log('response status', res.status);

            if (res.ok) {
                // 성공 → 구매 거래 목록으로 이동
                window.location.href = '/buyer/deals';
                return;
            }

            // 실패 → 서버에서 내려준 메시지를 보여주기
            let message = '리뷰 작성 중 오류가 발생했습니다.';
            try {
                const data = await res.json();
                log('response body', data);
                if (data && (data.message || data.errorMessage || data.error)) {
                    message = data.message || data.errorMessage || data.error;
                }
            } catch (e) {
                log('응답 JSON 파싱 실패', e);
            }

            formError.textContent = message;
            formError.classList.remove('hidden');
        } catch (e) {
            log('요청 실패', e);
            formError.textContent = '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            formError.classList.remove('hidden');
        } finally {
            submitBtn.disabled = false;
        }
    }

    submitBtn.addEventListener('click', submitReview);
});
