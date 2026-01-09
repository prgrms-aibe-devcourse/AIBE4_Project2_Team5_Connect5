(function () {
    let currentFilter = 'all';

    // 필터 버튼
    const filterButtons = document.querySelectorAll('.seller-review-filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            currentFilter = btn.dataset.filter;

            // 모든 버튼 비활성화
            filterButtons.forEach(b => {
                b.classList.remove('bg-gray-900', 'text-white');
                b.classList.add('bg-white', 'text-gray-700', 'border', 'border-gray-300');
            });

            // 클릭한 버튼 활성화
            btn.classList.remove('bg-white', 'text-gray-700', 'border', 'border-gray-300');
            btn.classList.add('bg-gray-900', 'text-white');

            loadReviews();
        });
    });

    // 데이터 로드
    async function loadReviews() {
        showLoading();

        // TODO: API 연동
        // const response = await fetch(`/api/seller/reviews?filter=${currentFilter}`);
        // const data = await response.json();

        // 임시: 빈 데이터
        setTimeout(() => {
            renderReviews([]);
            updateStats(0, 0, 0, 0);
            hideLoading();
        }, 500);
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

        list.innerHTML = reviews.map(review => `
            <div class="rounded-lg border border-gray-200 bg-white p-6">
                <div class="flex items-start justify-between">
                    <div class="flex-1">
                        <!-- 별점 -->
                        <div class="flex items-center gap-2">
                            <div class="flex">
                                ${renderStars(review.rating)}
                            </div>
                            <span class="text-sm font-semibold text-gray-900">${review.rating}.0</span>
                        </div>

                        <!-- 구매자 정보 -->
                        <div class="mt-3 flex items-center gap-2">
                            <div class="h-8 w-8 rounded-full bg-gray-200"></div>
                            <div>
                                <div class="text-sm font-medium text-gray-900">${escapeHtml(review.buyerNickname)}</div>
                                <div class="text-xs text-gray-500">${formatDate(review.createdAt)}</div>
                            </div>
                        </div>

                        <!-- 상품 정보 -->
                        <div class="mt-3">
                            <a href="/auctions/${review.auctionId}" class="text-sm font-medium text-gray-900 hover:underline">
                                ${escapeHtml(review.auctionTitle)}
                            </a>
                        </div>

                        <!-- 리뷰 내용 -->
                        <p class="mt-3 text-sm text-gray-700">
                            ${escapeHtml(review.content)}
                        </p>
                    </div>
                </div>
            </div>
        `).join('');
    }

    function renderStars(rating) {
        const fullStars = Math.floor(rating);
        let html = '';
        for (let i = 0; i < 5; i++) {
            if (i < fullStars) {
                html += '<svg class="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>';
            } else {
                html += '<svg class="h-5 w-5 text-gray-300" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>';
            }
        }
        return html;
    }

    function updateStats(total, avg, fiveStar, month) {
        document.getElementById('total-reviews').textContent = total;
        document.getElementById('avg-rating').textContent = avg.toFixed(1);
        document.getElementById('five-star').textContent = fiveStar;
        document.getElementById('month-reviews').textContent = month;
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatDate(dateString) {
        // TODO: 실제 날짜 포맷팅
        return dateString;
    }

    function showLoading() {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('reviews-list').classList.add('hidden');
        document.getElementById('empty-state').classList.add('hidden');
    }

    function hideLoading() {
        document.getElementById('loading').classList.add('hidden');
    }

    // 페이지 로드 시 데이터 로드
    loadReviews();
})();
