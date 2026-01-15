(function () {
    const reviewId = document.getElementById('reviewId')?.value;
    const mode = document.getElementById('mode')?.value; // WRITTEN / RECEIVED

    const errorEl = document.getElementById('errorMessage');
    const containerEl = document.getElementById('reviewContainer');

    const createdAtEl = document.getElementById('createdAt');
    const ratingEl = document.getElementById('rating');
    const targetNicknameEl = document.getElementById('targetNickname');
    const contentEl = document.getElementById('content');
    const dealInfoEl = document.getElementById('dealInfo');

    const backBtn = document.getElementById('backButton');

    backBtn?.addEventListener('click', () => {
        window.location.href = (mode === 'WRITTEN')
            ? '/mypage/buyer-reviews'
            : '/mypage/seller-reviews';
    });

    if (!reviewId || !mode) {
        showError('필수 정보가 없습니다. (reviewId/mode)');
        return;
    }

    const apiUrl = (mode === 'WRITTEN')
        ? `/api/buyer/reviews/${reviewId}`
        : `/api/seller/reviews/${reviewId}`;

    loadDetail(apiUrl);

    async function loadDetail(url) {
        try {
            const res = await fetch(url);
            if (!res.ok) {
                if (res.status === 403) throw new Error('접근 권한이 없습니다.');
                if (res.status === 404) throw new Error('리뷰를 찾을 수 없습니다.');
                throw new Error('리뷰 조회에 실패했습니다.');
            }

            const r = await res.json();

            // 화면 채우기 (ReviewResponse 기반)
            createdAtEl.textContent = formatDateTime(r.createdAt);
            ratingEl.textContent = `★ ${r.rating}`;

            targetNicknameEl.textContent = (mode === 'WRITTEN')
                ? `대상(판매자): ${safeText(r.sellerNickname)}`
                : `작성자(구매자): ${safeText(r.buyerNickname)}`;

            contentEl.textContent = safeText(r.content);

            dealInfoEl.textContent =
                `${safeText(r.dealTitle)} · 최종가 ${formatPrice(r.dealFinalPrice)} · dealId=${r.dealId}`;

            containerEl.classList.remove('hidden');
        } catch (e) {
            console.error(e);
            showError(e.message || '오류가 발생했습니다.');
        }
    }

    function showError(msg) {
        errorEl.textContent = msg;
        errorEl.classList.remove('hidden');
        containerEl.classList.add('hidden');
    }

    function safeText(v) {
        if (v == null) return '-';
        const s = String(v);
        return s.trim() === '' ? '-' : s;
    }

    function formatPrice(v) {
        if (v == null) return '-';
        const n = Number(v);
        if (Number.isNaN(n)) return String(v);
        return `${n.toLocaleString('ko-KR')}원`;
    }

    function formatDateTime(iso) {
        if (!iso) return '-';
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return String(iso);
        return d.toLocaleString('ko-KR');
    }
})();
