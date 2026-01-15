(function () {
    let currentFilter = 'all';
    let allReviews = [];

    const filterButtons = document.querySelectorAll('.seller-review-filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            currentFilter = btn.dataset.filter;

            filterButtons.forEach(b => {
                b.classList.remove('bg-gray-900', 'text-white');
                b.classList.add('bg-white', 'text-gray-700', 'border', 'border-gray-300');
            });

            btn.classList.remove('bg-white', 'text-gray-700', 'border', 'border-gray-300');
            btn.classList.add('bg-gray-900', 'text-white');

            const filtered = applyFilter(allReviews);
            renderReviews(filtered);
            updateStats(allReviews);
        });
    });

    async function loadReviews() {
        showLoading();
        try {
            const response = await fetch(`/api/seller/reviews`);
            if (!response.ok) throw new Error('받은 리뷰 목록 조회 실패');

            allReviews = await response.json();

            const filtered = applyFilter(allReviews);
            renderReviews(filtered);
            updateStats(allReviews);
        } catch (e) {
            console.error(e);
            renderReviews([]);
            updateStats([]);
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

        if (!reviews || reviews.length === 0) {
            list.classList.add('hidden');
            emptyState.classList.remove('hidden');
            return;
        }

        list.classList.remove('hidden');
        emptyState.classList.add('hidden');

        list.innerHTML = reviews.map(review => {
            const dealLink = `/seller/deals/${review.dealId}`;
            const detailLink = `/mypage/seller-reviews/${review.reviewId}`;

            return `
            <div class="rounded-lg border border-gray-200 bg-white p-6 hover:bg-gray-50">
                <div class="flex items-start justify-between">
                    <div class="flex-1">
                        <div class="flex items-center gap-2">
                            <div class="flex">${renderStars(review.rating)}</div>
                            <span class="text-sm font-semibold text-gray-900">${review.rating}.0</span>
                        </div>

                        <div class="mt-3 flex items-center gap-2">
                            <div class="h-8 w-8 rounded-full bg-gray-200"></div>
                            <div>
                                <div class="text-sm font-medium text-gray-900">${escapeHtml(review.buyerNickname)}</div>
                                <div class="text-xs text-gray-500">${formatDate(review.createdAt)}</div>
                            </div>
                        </div>

                        <div class="mt-3">
                            <a href="${dealLink}" class="text-sm font-medium text-gray-900 hover:underline">
                                ${escapeHtml(review.dealTitle)} · ${formatPrice(review.dealFinalPrice)}
                            </a>
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

    function updateStats(reviews) {
        const total = reviews.length;
        const avg = total === 0 ? 0 : (reviews.reduce((s, r) => s + (r.rating ?? 0), 0) / total);
        const fiveStar = reviews.filter(r => (r.rating ?? 0) === 5).length;

        const now = new Date();
        const monthReviews = reviews.filter(r => {
            if (!r.createdAt) return false;
            const d = new Date(r.createdAt);
            return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
        }).length;

        document.getElementById('total-reviews').textContent = total;
        document.getElementById('avg-rating').textContent = avg.toFixed(1);
        document.getElementById('five-star').textContent = fiveStar;
        document.getElementById('month-reviews').textContent = monthReviews;
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

    loadReviews();
})();
