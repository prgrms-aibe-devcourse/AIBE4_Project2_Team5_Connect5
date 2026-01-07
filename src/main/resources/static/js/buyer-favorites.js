(function () {
    let currentFilter = 'all';
    let currentSort = 'recent';

    // 필터 버튼
    const filterButtons = document.querySelectorAll('.fav-filter-btn');
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

            loadFavorites();
        });
    });

    // 정렬 선택
    const sortSelect = document.getElementById('sort-select');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            currentSort = e.target.value;
            loadFavorites();
        });
    }

    // 데이터 로드
    async function loadFavorites() {
        showLoading();

        // TODO: API 연동
        // const response = await fetch(`/api/buyer/favorites?filter=${currentFilter}&sort=${currentSort}`);
        // const data = await response.json();

        // 임시: 빈 데이터
        setTimeout(() => {
            renderFavorites([]);
            hideLoading();
        }, 500);
    }

    function renderFavorites(favorites) {
        const grid = document.getElementById('favorites-grid');
        const emptyState = document.getElementById('empty-state');
        const countSpan = document.getElementById('favorite-count');

        if (!favorites || favorites.length === 0) {
            grid.classList.add('hidden');
            emptyState.classList.remove('hidden');
            countSpan.textContent = '0';
            return;
        }

        grid.classList.remove('hidden');
        emptyState.classList.add('hidden');
        countSpan.textContent = favorites.length;

        // 경매 카드 렌더링 (auction-list fragment와 동일한 구조)
        grid.innerHTML = favorites.map(auction => `
            <article class="group rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:shadow-md">
                <a href="/auctions/${auction.auctionId}" class="block">
                    <div class="auction-thumb-wrap">
                        <img
                            class="auction-thumb-img"
                            src="${auction.thumbnailUrl || '/images/placeholder.png'}"
                            alt="${escapeHtml(auction.title)}"
                            loading="lazy"
                        />
                        <button
                            type="button"
                            class="auction-fav-btn is-active"
                            data-auction-id="${auction.auctionId}"
                            aria-pressed="true"
                            aria-label="찜"
                            onclick="event.preventDefault(); event.stopPropagation();"
                        >
                            <span class="auction-fav-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M12 21s-7.5-4.35-9.6-9.05C.9 8.9 2.5 6 5.8 6c1.9 0 3.1 1 4.2 2.2C11.1 7 12.3 6 14.2 6c3.3 0 4.9 2.9 3.4 5.95C19.5 16.65 12 21 12 21z"/>
                                </svg>
                            </span>
                        </button>
                    </div>
                    <div class="p-4">
                        <h3 class="line-clamp-2 text-sm font-medium text-slate-900">
                            ${escapeHtml(auction.title)}
                        </h3>
                        <div class="mt-2 text-xl font-semibold text-slate-950">
                            <span>${formatPrice(auction.currentPrice)}</span>
                            <span class="text-base font-medium text-slate-700">원</span>
                        </div>
                        <div class="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-600">
                            <span class="inline-flex items-center gap-1.5">
                                <span>입찰</span>
                                <span class="font-medium text-slate-800">${auction.bidCount}</span>
                                <span>회</span>
                            </span>
                            <span class="auction-remaining">${auction.remainingTime || ''}</span>
                        </div>
                    </div>
                </a>
            </article>
        `).join('');
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatPrice(price) {
        return price.toLocaleString('ko-KR');
    }

    function showLoading() {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('favorites-grid').classList.add('hidden');
        document.getElementById('empty-state').classList.add('hidden');
    }

    function hideLoading() {
        document.getElementById('loading').classList.add('hidden');
    }

    // 페이지 로드 시 데이터 로드
    loadFavorites();
})();
