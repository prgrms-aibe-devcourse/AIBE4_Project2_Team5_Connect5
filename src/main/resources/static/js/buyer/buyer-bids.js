(function () {
    // 탭 전환
    const tabButtons = document.querySelectorAll('.bid-tab-btn');
    const tabContents = document.querySelectorAll('.bid-tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;

            // 모든 탭 버튼 비활성화
            tabButtons.forEach(b => {
                b.classList.remove('border-gray-900', 'text-gray-900', 'font-semibold');
                b.classList.add('border-transparent', 'text-gray-500', 'font-medium');
            });

            // 클릭한 탭 버튼 활성화
            btn.classList.remove('border-transparent', 'text-gray-500', 'font-medium');
            btn.classList.add('border-gray-900', 'text-gray-900', 'font-semibold');

            // 모든 탭 콘텐츠 숨기기
            tabContents.forEach(content => {
                content.classList.add('hidden');
            });

            // 선택한 탭 콘텐츠 표시
            const targetContent = document.getElementById(`tab-${tabName}`);
            if (targetContent) {
                targetContent.classList.remove('hidden');
            }
        });
    });

    // 데이터 로드 (API 연동 시 사용)
    async function loadBids() {
        // TODO: API 연동
        // const response = await fetch('/api/buyer/bids');
        // const data = await response.json();

        // 임시 데이터
        updateCounts(0, 0, 0);
    }

    function updateCounts(bidding, won, lost) {
        document.getElementById('bidding-count').textContent = bidding;
        document.getElementById('won-count').textContent = won;
        document.getElementById('lost-count').textContent = lost;
    }

    // 페이지 로드 시 데이터 로드
    loadBids();
})();
