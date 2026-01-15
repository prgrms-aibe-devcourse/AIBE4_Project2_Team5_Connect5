(function () {
    let currentFilter = 'all';
    let allReviews = [];

    const filterButtons = document.querySelectorAll('.review-filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            currentFilter = btn.dataset.filter;

            filterButtons.forEach(b => {
                b.classList.remove('bg-gray-900', 'text-white');
                b.classList.add('bg-white', 'text-gray-700', 'border', 'border-gray-300');
            });

            btn.classList.remove('bg-white', 'text-gray-700', 'border', 'border-gray-300');
            btn.classList.add('bg-gray-900', 'text-white');

            renderReviews(applyFilter(allReviews));
        });
    });

    async function loadReviews() {
        showLoading();
        try {
            const response = await fetch(`/api/buyer/reviews`);
            if (!response.ok) throw new Error('작성 리뷰 목록 조회 실패');

            allReviews = await response.json();
            renderReviews(applyFilter(allReviews));
        } catch (e) {
            console.error(e);
            renderReviews([]);
        } finally {
            hideLoading();
        }
    }

    function applyFilter(reviews) {
        if (!reviews) return [];
        if (currentFilter === 'all') return reviews;

        if (currentFilter === 'low') {
            return reviews.filter(r => (r.rating ?? 0) <= 2);
        }

        const star = Number(currentFilter);
        if (!Number.isNaN(star)) {
            return reviews.filter(r => (r.rating ?? 0) === star);
        }

        return reviews;
    }

    function renderReviews(reviews) {
        const list = document.getElementById('reviews-list');
        const emptyState = document.getElementById('empty-state');
        const countSpan = document.getElementById('review-count');

        countSpan.textContent = String(reviews?.length ?? 0);

        if (!reviews || reviews.length === 0) {
            list.classList.add('hidden');
            emptyState.classList.remove('hidden');
            return;
        }

        list.classList.remove('hidden');
        emptyState.classList.add('hidden');

        list.innerHTML = reviews.map(review => {
            const dealLink = `/buyer/deals/${review.dealId}`; // Buyer 거래 상세로 연결
            const detailLink = `/mypage/buyer-reviews/${review.reviewId}`; // 마이페이지 리뷰 상세(우리가 추가한 뷰)

            return `
            <div class="rounded-lg border border-gray-200 bg-white p-6 hover:bg-gray-50">
                <div class="flex items-start justify-between">
                    <div class="flex-1">
                        <div class="flex items-center gap-2">
                            <div class="flex">${renderStars(review.rating)}</div>
                            <span class="text-sm font-semibold text-gray-900">${review.rating}.0</span>
                        </div>

                        <div class="mt-3">
                            <a href="${dealLink}" class="text-sm font-medium text-gray-900 hover:underline">
                                ${escapeHtml(review.dealTitle)} · ${formatPrice(review.dealFinalPrice)}
                            </a>
                            <p class="mt-1 text-xs text-gray-500">${formatDate(review.createdAt)}</p>
                            <p class="mt-1 text-xs text-gray-600">대상(판매자): ${escapeHtml(review.sellerNickname)}</p>
                        </div>

                        <p class="mt-3 text-sm text-gray-700">
                            ${escapeHtml(review.content)}
                        </p>

                        <div class="mt-4">
                            <a href="${detailLink}" class="text-sm font-semibold text-gray-900 hover:underline">
                                상세 보기 →
                            </a>
                        </div>
                    </div>

                    <div class="ml-4 flex gap-2">
                        <!-- 수정은 정책 확정 전이면 숨기는게 안전 -->
                        <button type="button"
                                class="text-sm text-red-600 hover:text-red-900"
                                onclick="deleteReview('${review.reviewId}')">
                            삭제
                        </button>
                    </div>
                </div>
            </div>
            `;
        }).join('');
    }

    function renderStars(rating) {
        const fullStars = Math.max(0, Math.min(5, Math.floor(rating || 0)));
        let html = '';
        for (let i = 0; i < 5; i++) {
            html += (i < fullStars)
                ? '<svg class="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>'
                : '<svg class="h-5 w-5 text-gray-300" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>';
        }
        return html;
    }

    function escapeHtml(text) {
        if (!text) return '-';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function formatDate(dateString) {
        if (!dateString) return '-';
        const d = new Date(dateString);
        if (isNaN(d.getTime())) return dateString;
        return d.toLocaleDateString('ko-KR');
    }

    function formatPrice(price) {
        if (price == null) return '';
        return `${Number(price).toLocaleString()}원`;
    }

    function showLoading() {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('reviews-list').classList.add('hidden');
        document.getElementById('empty-state').classList.add('hidden');
    }

    function hideLoading() {
        document.getElementById('loading').classList.add('hidden');
    }

    window.deleteReview = async function (reviewId) {
        if (!confirm('리뷰를 삭제하시겠습니까?')) return;
        try {
            const res = await fetch(`/api/reviews/${reviewId}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('삭제 실패');

            allReviews = allReviews.filter(r => String(r.reviewId) !== String(reviewId));
            renderReviews(applyFilter(allReviews));
        } catch (e) {
            console.error(e);
            alert('삭제 중 오류가 발생했습니다.');
        }
    };

    loadReviews();
})();
